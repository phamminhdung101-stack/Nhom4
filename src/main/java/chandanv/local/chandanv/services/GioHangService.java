package chandanv.local.chandanv.services;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import chandanv.local.chandanv.models.dto.ThemMonRequest;
import chandanv.local.chandanv.models.entity.GioHang;
import chandanv.local.chandanv.models.entity.MonDaChon;
import chandanv.local.chandanv.repositories.GioHangRepository;
import chandanv.local.chandanv.websocket.OrderEventPublisher;

@Service
public class GioHangService {

    /** Số lần thử lại khi giỏ vừa bị điện thoại khác cùng bàn sửa đúng lúc. */
    private static final int SO_LAN_THU = 20;

    private final GioHangRepository repo;
    private final MonAnService monAnService;
    private final BanAnService banAnService;
    private final MongoTemplate mongo;
    private final OrderEventPublisher events;

    public GioHangService(GioHangRepository repo, MonAnService monAnService, BanAnService banAnService,
                          MongoTemplate mongo, OrderEventPublisher events) {
        this.repo = repo;
        this.monAnService = monAnService;
        this.banAnService = banAnService;
        this.mongo = mongo;
        this.events = events;
    }

    /** Lấy giỏ của bàn. Không tự tạo bản ghi khi chỉ đọc. */
    public GioHang lay(int idBan) {
        banAnService.layBan(idBan);
        return repo.findByIdBan(idBan).orElseGet(() -> new GioHang(idBan));
    }

    public GioHang themMon(int idBan, ThemMonRequest req) {
        MonDaChon moi = monAnService.taoDong(req);
        return suaGio(idBan, true, gio -> {
            MonDaChon trung = gio.getItems().stream().filter(i -> i.cungTuyChon(moi)).findFirst().orElse(null);
            if (trung != null) {
                int sl = Math.min(trung.getSoLuong() + moi.getSoLuong(), TuyChonMon.SO_LUONG_TOI_DA);
                trung.setSoLuong(sl);
                trung.setDonGia(moi.getDonGia()); // cập nhật theo giá hiện tại
                trung.tinhThanhTien();
            } else {
                gio.getItems().add(moi);
            }
        });
    }

    public GioHang capNhatSoLuong(int idBan, String itemId, Integer soLuong) {
        if (soLuong == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu số lượng");
        }
        if (soLuong > TuyChonMon.SO_LUONG_TOI_DA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng tối đa là " + TuyChonMon.SO_LUONG_TOI_DA);
        }
        return suaGio(idBan, false, gio -> {
            MonDaChon line = gio.getItems().stream().filter(i -> itemId.equals(i.getId())).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Món không còn trong giỏ"));
            if (soLuong <= 0) {
                gio.getItems().remove(line);
            } else {
                line.setSoLuong(soLuong);
                line.tinhThanhTien();
            }
        });
    }

    public GioHang xoaMon(int idBan, String itemId) {
        return suaGio(idBan, false, gio -> gio.getItems().removeIf(i -> itemId.equals(i.getId())));
    }

    public void xoaGio(int idBan) {
        repo.deleteByIdBan(idBan);
        events.gioHangThayDoi(idBan, null);
    }

    /**
     * Sửa giỏ an toàn khi nhiều điện thoại cùng bàn sửa cùng lúc (khoá lạc quan theo số phiên bản).
     *
     * Vấn đề: giỏ là 1 bản ghi chung của cả bàn. Máy A và máy B cùng đọc giỏ [Trà sữa], A thêm Cà phê,
     * B thêm Bánh, rồi cả 2 ghi cả giỏ xuống. Ghi bình thường thì bản ghi sau đè bản ghi trước -> mất 1 món.
     *
     * Cách xử lý: mỗi giỏ có trường phienBan, tăng 1 sau mỗi lần sửa. Khi ghi, điều kiện là "phienBan vẫn bằng
     * số lúc đọc" (findAndReplace kiểm tra và ghi nguyên tử). Ai ghi sau sẽ không khớp (saved == null) vì
     * phienBan đã tăng: request đó chờ vài ms ngẫu nhiên (để các request không va nhau lần nữa), đọc lại giỏ
     * mới nhất rồi làm lại thao tác, tối đa SO_LAN_THU lần. Kết quả: giỏ có đủ cả 3 món.
     */
    private GioHang suaGio(int idBan, boolean taoNeuChuaCo, Consumer<GioHang> thayDoi) {
        for (int lan = 0; lan < SO_LAN_THU; lan++) {
            GioHang gio = taoNeuChuaCo ? layHoacTao(idBan) : layCoSan(idBan);
            Long phienBanCu = gio.getPhienBan();
            thayDoi.accept(gio);
            gio.setPhienBan(phienBanCu == null ? 1L : phienBanCu + 1);
            gio.setUpdatedAt(Instant.now());

            // phienBan null khớp cả giỏ cũ chưa có trường này
            Query q = Query.query(Criteria.where("_id").is(gio.getId()).and("phienBan").is(phienBanCu));
            GioHang saved = mongo.findAndReplace(q, gio, FindAndReplaceOptions.options().returnNew());
            if (saved != null) {
                events.gioHangThayDoi(idBan, saved); // điện thoại khác cùng bàn cập nhật giỏ ngay
                return saved;
            }
            try {
                Thread.sleep(5 + ThreadLocalRandom.current().nextInt(20));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Giỏ hàng đang được cập nhật, vui lòng thử lại");
    }

    private GioHang layCoSan(int idBan) {
        banAnService.layBan(idBan);
        return repo.findByIdBan(idBan)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Giỏ hàng đang trống"));
    }

    private GioHang layHoacTao(int idBan) {
        banAnService.layBan(idBan);
        return repo.findByIdBan(idBan).orElseGet(() -> {
            try {
                return repo.save(new GioHang(idBan));
            } catch (DuplicateKeyException e) {
                // request khác vừa tạo giỏ cho cùng bàn (unique index idBan) -> đọc lại
                return repo.findByIdBan(idBan).orElseThrow(() -> e);
            }
        });
    }
}
