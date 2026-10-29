package chandanv.local.chandanv.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import chandanv.local.chandanv.models.entity.BanAn;
import chandanv.local.chandanv.models.entity.GioHang;
import chandanv.local.chandanv.models.entity.ThietBi;
import chandanv.local.chandanv.repositories.BanAnRepository;
import chandanv.local.chandanv.security.AuditLog;
import chandanv.local.chandanv.util.TextUtil;
import chandanv.local.chandanv.websocket.OrderEventPublisher;

/**
 * Bàn ăn và PHIÊN BÀN.
 *
 * Phiên bàn = khoảng thời gian 1 nhóm khách ngồi ở bàn, từ lúc bàn chuyển "Có khách" tới lúc bàn về "Trống".
 * Mã trên QR in trên bàn (token của bàn) KHÔNG dùng để đặt món, chỉ dùng để XIN VÀO bàn (vaoBan). Mỗi điện thoại
 * vào bàn được cấp 1 mã riêng (ThietBi.token, gửi kèm header X-Table-Token), lưu trong danh sách thietBi của bàn.
 * Khi phiên kết thúc, danh sách này bị xoá -> điện thoại của khách cũ mất quyền, dù đã chụp lại mã QR
 * hay về nhà vẫn giữ trang web cũng không đặt món vào bàn được nữa.
 */
@Service
public class BanAnService {

    private static final int QR_IMAGE_MAX_BYTES = 512 * 1024;
    /** Chống spam: tối đa số máy chờ duyệt cùng lúc ở 1 bàn. */
    private static final int MAX_MAY_CHO_DUYET = 10;

    private final BanAnRepository repo;
    private final InMemoryChatService chatService;
    private final OrderEventPublisher events;
    private final MongoTemplate mongo;
    private final boolean duyetMayMoi;

    public BanAnService(BanAnRepository repo, InMemoryChatService chatService, OrderEventPublisher events,
                        MongoTemplate mongo, @Value("${app.ban.duyet-may-moi:true}") boolean duyetMayMoi) {
        this.repo = repo;
        this.chatService = chatService;
        this.events = events;
        this.mongo = mongo;
        this.duyetMayMoi = duyetMayMoi;
    }

    public List<BanAn> layDanhSachBan() {
        return repo.findAllByOrderByIdBanAsc();
    }

    public BanAn layBan(int idBan) {
        return repo.findByIdBan(idBan)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bàn " + idBan));
    }

    public BanAn capNhatTrangThai(int idBan, int trangThai) {
        return capNhatTrangThai(idBan, trangThai, false);
    }

    /** Khách đã nhận đủ món -> bàn tự về trống, xoá phiên chat (thay cho việc nhân viên bấm "Hoàn thành"). */
    public BanAn traBanTuDong(int idBan) {
        return capNhatTrangThai(idBan, BanAn.TRONG, true);
    }

    private BanAn capNhatTrangThai(int idBan, int trangThai, boolean tuDong) {
        if (trangThai != BanAn.TRONG && trangThai != BanAn.CO_KHACH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái bàn không hợp lệ: " + trangThai);
        }
        Update update = new Update().set("trangThai", trangThai);
        if (trangThai == BanAn.CO_KHACH) {
            // Mốc bắt đầu lượt khách. Trả bàn thì GIỮ mốc cũ -> khách vừa xong vẫn xem lại được đơn của mình,
            // tới khi khách mới vào bàn (mốc mới) thì đơn cũ mới ẩn đi.
            update.set("thoiGianDatBan", Instant.now().toString());
        } else {
            ketThucPhien(update, layBan(idBan));
        }
        // Chỉ đổi khi khác trạng thái hiện tại (không phát sự kiện thừa); cập nhật từng trường, không ghi đè cả bàn
        BanAn saved = mongo.findAndModify(
                Query.query(Criteria.where("idBan").is(idBan).and("trangThai").ne(trangThai)),
                update, FindAndModifyOptions.options().returnNew(true), BanAn.class);
        if (saved == null) {
            return layBan(idBan); // đã đúng trạng thái (hoặc 404 nếu không có bàn)
        }
        if (trangThai == BanAn.TRONG) {
            sauKhiKetThucPhien(idBan);
        }
        events.banThayDoi(saved, tuDong);
        return saved;
    }

    /**
     * Phiên bàn vừa kết thúc: xoá phiên chat, xoá giỏ CHƯA GỬI của khách cũ (khách mới vào bàn không thấy /
     * không gửi nhầm món của người trước), đưa kết nối realtime của các máy cũ ra khỏi phòng bàn.
     */
    private void sauKhiKetThucPhien(int idBan) {
        chatService.clearTableChat(idBan);
        mongo.remove(Query.query(Criteria.where("idBan").is(idBan)), GioHang.class);
        events.gioHangThayDoi(idBan, null);
        events.loaiMayHetQuyen(idBan);
    }

    /** Xoá danh sách máy của bàn: mọi điện thoại của phiên vừa rồi mất quyền đặt món. */
    private static Update ketThucPhien(Update update) {
        return update.set("thietBi", new ArrayList<>()).set("soThietBi", 0);
    }

    /**
     * Như trên, nhưng lưu lại mã của các máy đã duyệt vào phienCuTokens kèm mốc bắt đầu / kết thúc phiên.
     * Nhờ vậy khách vừa trả bàn vẫn XEM LẠI được đơn của mình (chỉ xem, không đặt món), và không thấy đơn của khách sau.
     */
    private static Update ketThucPhien(Update update, BanAn ban) {
        List<String> tokens = ban.getThietBi().stream().filter(ThietBi::isDaDuyet).map(ThietBi::getToken).toList();
        if (!tokens.isEmpty()) {
            update.set("phienCuTokens", tokens)
                    .set("phienCuBatDau", ban.getThoiGianDatBan())
                    .set("phienCuKetThuc", Instant.now().toString());
        }
        return ketThucPhien(update);
    }

    /** Reset các bàn được chọn về Trống (xoá phiên chat từng bàn). Trả về số bàn thực sự được reset. */
    public int resetCacBan(List<Integer> idBans) {
        int dem = 0;
        for (Integer idBan : new LinkedHashSet<>(idBans)) {
            if (idBan == null) continue;
            BanAn ban = repo.findByIdBan(idBan).orElse(null);
            if (ban == null) continue;
            if (ban.getTrangThai() == BanAn.TRONG) {
                // bàn đã trống: vẫn xoá phiên chat / máy còn sót
                mongo.updateFirst(Query.query(Criteria.where("idBan").is(idBan)), ketThucPhien(new Update()), BanAn.class);
                sauKhiKetThucPhien(idBan);
            } else {
                capNhatTrangThai(idBan, BanAn.TRONG);
                dem++;
            }
        }
        return dem;
    }

    public void resetTatCaBan() {
        for (BanAn ban : repo.findAll()) {
            mongo.updateFirst(Query.query(Criteria.where("idBan").is(ban.getIdBan())),
                    ketThucPhien(new Update().set("trangThai", BanAn.TRONG), ban), BanAn.class);
            events.loaiMayHetQuyen(ban.getIdBan());
        }
        mongo.remove(new Query(), GioHang.class); // giỏ chưa gửi của mọi bàn
        chatService.clearAll();
        events.banThayDoi(null);
    }

    // ---------- Phiên bàn: các điện thoại đang ngồi ở bàn ----------

    /**
     * Điện thoại quét QR vào bàn (mã QR đã được kiểm tra trước).
     *  - Bàn chưa có máy nào được duyệt -> máy này là CHỦ BÀN, dùng được ngay, bàn chuyển "Có khách".
     *  - Bàn đã có người -> máy mới CHỜ NHÂN VIÊN DUYỆT (app.ban.duyet-may-moi=true): chặn người lạ
     *    quét trộm QR đặt món vào bàn người khác rồi bỏ đi.
     */
    public ThietBi vaoBan(int idBan) {
        BanAn ban = layBan(idBan);
        Instant now = Instant.now();
        ThietBi may = new ThietBi();
        may.setId(TextUtil.randomToken(6));
        may.setToken(TextUtil.randomToken(24));
        may.setVaoLuc(now);

        // 1) Thử làm chủ bàn. Điều kiện "bàn chưa có máy nào đã duyệt" được MongoDB kiểm tra và ghi trong cùng
        //    1 lệnh findAndModify, nên 2 máy quét cùng lúc thì chỉ 1 máy làm chủ; máy kia nhận r == null,
        //    đi tiếp xuống bước 2 và phải chờ duyệt. (Nếu kiểm tra ở Java rồi mới ghi, cả 2 đều thành chủ bàn.)
        may.setLaChu(true);
        may.setTen("Máy 1");
        may.setTrangThai(ThietBi.DA_DUYET);
        Update lamChu = new Update().push("thietBi", may).max("soThietBi", 1).set("trangThai", BanAn.CO_KHACH);
        if (ban.getTrangThai() == BanAn.TRONG) lamChu.set("thoiGianDatBan", now.toString());
        BanAn r = mongo.findAndModify(
                Query.query(Criteria.where("idBan").is(idBan)
                        .and("thietBi").not().elemMatch(Criteria.where("trangThai").is(ThietBi.DA_DUYET))),
                lamChu, FindAndModifyOptions.options().returnNew(true), BanAn.class);
        if (r != null) {
            if (ban.getTrangThai() == BanAn.TRONG) events.banThayDoi(r, false);
            AuditLog.info("TABLE_JOIN", "ban=" + idBan + " may=" + may.getTen() + " (chu ban)");
            return may;
        }

        // 2) Bàn đã có chủ -> thêm máy mới. Số thứ tự lấy bằng $inc nguyên tử để 2 máy vào cùng lúc không trùng tên.
        long choDuyet = ban.getThietBi().stream().filter(t -> !t.isDaDuyet()).count();
        if (duyetMayMoi && choDuyet >= MAX_MAY_CHO_DUYET) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Có quá nhiều máy đang chờ vào bàn này, vui lòng gọi nhân viên");
        }
        BanAn dem = mongo.findAndModify(Query.query(Criteria.where("idBan").is(idBan)),
                new Update().inc("soThietBi", 1), FindAndModifyOptions.options().returnNew(true), BanAn.class);
        may.setLaChu(false);
        may.setTen("Máy " + (dem == null ? 2 : Math.max(2, dem.getSoThietBi())));
        may.setTrangThai(duyetMayMoi ? ThietBi.CHO_DUYET : ThietBi.DA_DUYET);
        mongo.updateFirst(Query.query(Criteria.where("idBan").is(idBan)), new Update().push("thietBi", may), BanAn.class);
        AuditLog.info("TABLE_JOIN", "ban=" + idBan + " may=" + may.getTen() + " trangThai=" + may.getTrangThai());
        if (duyetMayMoi) events.mayChoDuyet(idBan, ban.getTenBan(), may);
        else events.banThayDoi(layBan(idBan), false);
        return may;
    }

    /** Nhân viên cho phép máy đang chờ vào bàn. */
    public void duyetMay(int idBan, String maMay) {
        var res = mongo.updateFirst(
                Query.query(Criteria.where("idBan").is(idBan).and("thietBi.id").is(maMay)),
                new Update().set("thietBi.$.trangThai", ThietBi.DA_DUYET), BanAn.class);
        if (res.getMatchedCount() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Máy này không còn ở bàn " + idBan);
        }
        events.banThayDoi(layBan(idBan), false);
    }

    /** Nhân viên từ chối máy chờ duyệt / mời 1 máy ra khỏi bàn: máy đó mất quyền ngay. */
    public void moiRaMay(int idBan, String maMay) {
        var res = mongo.updateFirst(Query.query(Criteria.where("idBan").is(idBan).and("thietBi.id").is(maMay)),
                new Update().pull("thietBi", new Document("id", maMay)), BanAn.class);
        if (res.getMatchedCount() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Máy này không còn ở bàn " + idBan);
        }
        AuditLog.info("TABLE_KICK", "ban=" + idBan + " may=" + maMay);
        events.loaiMayHetQuyen(idBan);
        events.banThayDoi(layBan(idBan), false);
    }

    // ---------- Ảnh QR ----------

    /** Nhận data URL (data:image/png;base64,...) hoặc chuỗi base64 thuần. */
    public void luuAnhQR(int idBan, String image) {
        if (image == null || image.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu dữ liệu ảnh QR");
        }
        String base64 = image.contains("base64,") ? image.substring(image.indexOf("base64,") + 7) : image;
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh QR không đúng định dạng base64");
        }
        if (bytes.length > QR_IMAGE_MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh QR quá lớn");
        }
        layBan(idBan);
        mongo.updateFirst(Query.query(Criteria.where("idBan").is(idBan)),
                new Update().set("qrImageBase64", base64.trim()), BanAn.class);
    }

    public byte[] layAnhQR(int idBan) {
        BanAn ban = layBan(idBan);
        if (!ban.isCoAnhQR()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bàn " + idBan + " chưa lưu ảnh QR");
        }
        return Base64.getDecoder().decode(ban.getQrImageBase64());
    }

    /** Cấp mã QR mới cho bàn (khi mã cũ bị lộ). Mã QR in cũ sẽ hết hiệu lực. */
    public BanAn doiMaQR(int idBan) {
        BanAn ban = layBan(idBan);
        ban.setToken(TextUtil.randomToken(9));
        ban.setMaQR(duongDanQR(ban));
        mongo.updateFirst(Query.query(Criteria.where("idBan").is(idBan)),
                new Update().set("token", ban.getToken()).set("maQR", ban.getMaQR()).unset("qrImageBase64"), BanAn.class);
        return layBan(idBan);
    }

    /** Bổ sung token/link QR còn thiếu hoặc sai định dạng. Trả về true nếu có thay đổi (cần save). */
    public static boolean damBaoMaQR(BanAn ban) {
        boolean changed = false;
        if (ban.getToken() == null || ban.getToken().isBlank()) {
            ban.setToken(TextUtil.randomToken(9));
            changed = true;
        }
        String expected = duongDanQR(ban);
        if (!expected.equals(ban.getMaQR())) {
            ban.setMaQR(expected);
            changed = true;
        }
        if (ban.getTenBan() == null || ban.getTenBan().isBlank()) {
            ban.setTenBan(tenBanMacDinh(ban.getIdBan()));
            changed = true;
        }
        return changed;
    }

    public static String duongDanQR(BanAn ban) {
        return "/welcome.html?idBan=" + ban.getIdBan() + "&t=" + ban.getToken();
    }

    public static String tenBanMacDinh(int idBan) {
        return "Bàn " + (idBan < 10 ? "0" + idBan : String.valueOf(idBan));
    }
}
