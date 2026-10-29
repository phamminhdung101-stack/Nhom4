package chandanv.local.chandanv.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import chandanv.local.chandanv.models.dto.TaiQuayRequest;
import chandanv.local.chandanv.models.dto.ThemMonRequest;
import chandanv.local.chandanv.models.entity.BanAn;
import chandanv.local.chandanv.models.entity.DonHang;
import chandanv.local.chandanv.models.entity.GioHang;
import chandanv.local.chandanv.models.entity.LichSuBanHang;
import chandanv.local.chandanv.models.entity.MonDaChon;
import chandanv.local.chandanv.repositories.BanAnRepository;
import chandanv.local.chandanv.repositories.DonHangRepository;
import chandanv.local.chandanv.repositories.LichSuBanHangRepository;
import chandanv.local.chandanv.security.AuditLog;
import chandanv.local.chandanv.util.TextUtil;
import chandanv.local.chandanv.websocket.OrderEventPublisher;

@Service
public class DonHangService {

    public static final String COUNTER_DON_HANG = "don_hang";
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DonHangRepository repo;
    private final BanAnService banAnService;
    private final BanAnRepository banAnRepository;
    private final MonAnService monAnService;
    private final SequenceService sequenceService;
    private final LichSuBanHangRepository lichSuRepository;
    private final OrderEventPublisher events;
    private final InMemoryChatService chatService;
    private final MongoTemplate mongo;

    public DonHangService(DonHangRepository repo,
                          BanAnService banAnService,
                          BanAnRepository banAnRepository,
                          MonAnService monAnService,
                          SequenceService sequenceService,
                          LichSuBanHangRepository lichSuRepository,
                          OrderEventPublisher events,
                          InMemoryChatService chatService,
                          MongoTemplate mongo) {
        this.repo = repo;
        this.banAnService = banAnService;
        this.banAnRepository = banAnRepository;
        this.monAnService = monAnService;
        this.sequenceService = sequenceService;
        this.lichSuRepository = lichSuRepository;
        this.events = events;
        this.chatService = chatService;
        this.mongo = mongo;
    }

    public DonHang datMonTuGio(int idBan, String ghiChu) {
        return datMonTuGio(idBan, ghiChu, null);
    }

    /**
     * Khách bấm "Gửi bếp": biến cả giỏ chung của bàn thành 1 đơn.
     *
     * Vì sao lấy giỏ bằng findAndRemove (đọc + xoá trong 1 lệnh nguyên tử của MongoDB):
     * nếu đọc giỏ rồi mới xoá ở 2 lệnh riêng, khi khách bấm "Gửi bếp" 2 lần liên tiếp hoặc 2 điện thoại
     * cùng bàn bấm cùng lúc, cả 2 request đều kịp đọc thấy giỏ -> bếp nhận 2 đơn trùng. Với findAndRemove
     * chỉ 1 request lấy được giỏ, request kia thấy giỏ trống và bị từ chối.
     * Nếu tạo đơn lỗi thì giỏ được lưu lại nguyên trạng để khách gửi lại.
     *
     * nguoiDat: tên máy đã bấm gửi ("Máy 2"...), hiện trên màn hình bếp.
     */
    public DonHang datMonTuGio(int idBan, String ghiChu, String nguoiDat) {
        banAnService.layBan(idBan); // 404 nếu bàn không tồn tại
        GioHang gio = mongo.findAndRemove(Query.query(Criteria.where("idBan").is(idBan)), GioHang.class);
        if (gio == null || gio.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng đang trống");
        }
        try {
            DonHang don = taoDonTuGio(idBan, gio, ghiChu, nguoiDat);
            events.gioHangThayDoi(idBan, null); // điện thoại khác cùng bàn thấy giỏ đã trống
            return don;
        } catch (RuntimeException e) {
            mongo.save(gio); // trả lại giỏ cho khách thử lại
            throw e;
        }
    }

    private DonHang taoDonTuGio(int idBan, GioHang gio, String ghiChu, String nguoiDat) {
        // Phải đánh dấu bàn "Có khách" TRƯỚC khi tạo đơn. Lúc bàn chuyển sang "Có khách", thời điểm đó được lưu làm
        // mốc bắt đầu lượt khách (thoiGianDatBan). Mọi chỗ phân biệt "đơn của khách hiện tại hay khách trước"
        // (donCuaLuotKhach, traBanNeuDaXong) đều so thời gian đơn với mốc này, nên đơn phải tạo sau mốc.
        banAnService.capNhatTrangThai(idBan, BanAn.CO_KHACH);

        DonHang don = new DonHang();
        don.setIdBan(idBan);
        don.setLoai(DonHang.LOAI_TAI_BAN);
        don.setItems(new ArrayList<>(gio.getItems()));
        don.setTongTien(gio.getTongTien());
        don.setGhiChu(TextUtil.lamSach(ghiChu, 300));
        don.setNguoiDat(nguoiDat);
        don.setIdDonHang(sequenceService.next(COUNTER_DON_HANG));

        DonHang saved = repo.save(don);
        events.donMoi(saved);
        return saved;
    }

    /** Thu ngân tạo đơn mua tại quầy (idBan = 0). */
    public DonHang taoDonTaiQuay(TaiQuayRequest req) {
        if (req == null || req.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chưa có món nào");
        }
        List<MonDaChon> lines = new ArrayList<>();
        for (ThemMonRequest r : req.getItems()) {
            lines.add(monAnService.taoDong(r));
        }
        DonHang don = new DonHang();
        don.setIdBan(0);
        don.setLoai(DonHang.LOAI_TAI_QUAY);
        don.setItems(lines);
        don.setTongTien(lines.stream().mapToLong(MonDaChon::getThanhTien).sum());
        don.setGhiChu(TextUtil.lamSach(req.getGhiChu(), 300));
        don.setIdDonHang(sequenceService.next(COUNTER_DON_HANG));

        DonHang saved = repo.save(don);
        events.donMoi(saved);
        return saved;
    }

    public Optional<DonHang> donMoiNhatCuaBan(int idBan) {
        return repo.findFirstByIdBanOrderByThoiGianDesc(idBan);
    }

    /**
     * Mọi đơn của lượt khách hiện tại ở bàn (mới nhất trước) – để khách xem lại / huỷ từng đơn đã đặt.
     * Lượt khách bắt đầu khi bàn chuyển "Có khách"; khách sau vào bàn sẽ không thấy đơn của khách trước.
     */
    public List<DonHang> donCuaLuotKhach(int idBan) {
        Instant batDau = banAnRepository.findByIdBan(idBan)
                .map(b -> thoiDiem(b.getThoiGianDatBan()))
                .orElse(Instant.EPOCH);
        if (batDau.equals(Instant.EPOCH)) {
            batDau = LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant(); // bàn chưa có mốc (dữ liệu cũ) -> đơn hôm nay
        }
        return repo.findByIdBanAndThoiGianGreaterThanEqualOrderByThoiGianDesc(idBan, batDau);
    }

    /** Đơn của phiên vừa kết thúc (bàn đã trả) – cho máy của phiên đó xem lại, không lẫn đơn khách sau. */
    public List<DonHang> donCuaPhienCu(int idBan) {
        BanAn ban = banAnService.layBan(idBan);
        Instant tu = thoiDiem(ban.getPhienCuBatDau());
        Instant den = ban.getPhienCuKetThuc() == null ? Instant.now() : thoiDiem(ban.getPhienCuKetThuc());
        return mongo.find(Query.query(Criteria.where("idBan").is(idBan).and("thoiGian").gte(tu).lte(den))
                .with(Sort.by(Sort.Direction.DESC, "thoiGian")), DonHang.class);
    }

    private static final int DANH_SACH_MAX = 500;

    /** Đơn chưa xong quá số ngày này (khách bỏ về, quên bấm Hoàn thành) không hiện trên màn hình bếp nữa. */
    private static final int BEP_GIU_DON_CHUA_XONG_NGAY = 2;

    /**
     * Danh sách đơn cho màn hình bếp, mới nhất trước.
     * Mặc định (tu = null): đơn HÔM NAY + đơn CHƯA XONG của 2 ngày gần nhất (đơn tối qua còn dở vẫn thấy).
     * Không tải cả lịch sử: trang bếp tải lại danh sách mỗi khi có đơn mới, dữ liệu 1 năm (~180.000 đơn, ~100 MB)
     * sẽ làm trang đơ. Xem đơn cũ: truyền tu = ngày bắt đầu, hoặc dùng trang Doanh thu (/api/don-hang/bao-cao).
     */
    public List<DonHang> danhSach(Integer trangThai, LocalDate tu) {
        if (trangThai != null && trangThai != DonHang.DA_HUY) kiemTraTrangThai(trangThai);
        LocalDate homNay = LocalDate.now(ZONE);
        Instant tuLuc = (tu == null ? homNay : tu).atStartOfDay(ZONE).toInstant();
        Criteria dieuKien = tu == null
                ? new Criteria().orOperator(Criteria.where("thoiGian").gte(tuLuc),
                        Criteria.where("trangThai").lt(DonHang.HOAN_THANH)
                                .and("thoiGian").gte(homNay.minusDays(BEP_GIU_DON_CHUA_XONG_NGAY).atStartOfDay(ZONE).toInstant()))
                : Criteria.where("thoiGian").gte(tuLuc);
        if (trangThai != null) {
            dieuKien = new Criteria().andOperator(dieuKien, Criteria.where("trangThai").is(trangThai));
        }
        return mongo.find(Query.query(dieuKien).with(Sort.by(Sort.Direction.DESC, "thoiGian")).limit(DANH_SACH_MAX),
                DonHang.class);
    }

    public DonHang layDon(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
    }

    public DonHang capNhatTrangThai(String id, Integer trangThai) {
        kiemTraTrangThai(trangThai);
        DonHang don = layDon(id);
        if (don.getTrangThai() != null && don.getTrangThai() == DonHang.DA_HUY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn #DH-" + don.getIdDonHang() + " đã bị huỷ");
        }
        DonHang saved = doiTrangThai(don, new Update()
                .set("trangThai", trangThai)
                .set("thoiGianCapNhat", Instant.now()));

        if (trangThai == DonHang.HOAN_THANH) {
            ghiLichSu(saved);
        }
        events.trangThaiDon(saved);
        if (trangThai == DonHang.HOAN_THANH) {
            traBanNeuDaXong(saved);
        }
        return saved;
    }

    /**
     * Huỷ đơn.
     *  - Khách (laKhach = true): chỉ huỷ được khi đơn còn "Chờ xác nhận"; bếp đã nhận thì phải nhắn tin nhân viên.
     *  - Nhân viên: huỷ được mọi đơn chưa hoàn thành (vd. khách nhắn tin xin huỷ).
     */
    public DonHang huyDon(String id, String lyDo, String nguoiHuy, boolean laKhach) {
        DonHang don = layDon(id);
        int hienTai = don.getTrangThai() == null ? DonHang.CHO_XAC_NHAN : don.getTrangThai();
        if (hienTai == DonHang.DA_HUY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn này đã được huỷ trước đó");
        }
        if (hienTai == DonHang.HOAN_THANH) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn đã hoàn thành, không thể huỷ");
        }
        if (laKhach && hienTai != DonHang.CHO_XAC_NHAN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Quán đã bắt đầu pha chế đơn này. Vui lòng nhắn tin cho nhân viên nếu muốn huỷ.");
        }

        String lyDoSach = TextUtil.lamSach(lyDo, 200);
        DonHang saved = doiTrangThai(don, new Update()
                .set("trangThai", DonHang.DA_HUY)
                .set("thoiGianCapNhat", Instant.now())
                .set("lyDoHuy", lyDoSach)
                .set("nguoiHuy", nguoiHuy));

        AuditLog.info("ORDER_CANCELLED", "don=" + saved.getIdDonHang() + " ban=" + saved.getIdBan() + " boi=" + nguoiHuy);
        events.trangThaiDon(saved);
        if (saved.getIdBan() != null && saved.getIdBan() > 0) {
            chatService.thongBao(saved.getIdBan(), "❌ Đơn #DH-" + saved.getIdDonHang() + " đã được huỷ bởi " + nguoiHuy
                    + (lyDoSach != null ? ". Lý do: " + lyDoSach : ""));
            traBanNeuDaXong(saved);
        }
        return saved;
    }

    /**
     * Đổi trạng thái đơn an toàn khi nhiều người bấm cùng lúc (khoá lạc quan).
     *
     * Tình huống cần chặn: khách bấm "Huỷ" đúng lúc bếp bấm "Nhận đơn". Cả 2 request đều đọc thấy đơn đang
     * "Chờ xác nhận" và đều hợp lệ; nếu ghi bình thường thì request ghi sau thắng: đơn có thể thành
     * "Đang pha chế" dù khách đã được báo huỷ thành công.
     *
     * Cách chặn: findAndModify với điều kiện "trangThai vẫn bằng giá trị vừa đọc". MongoDB kiểm tra điều kiện
     * và ghi trong cùng 1 thao tác nguyên tử, nên chỉ request đến trước ghi được; request sau không khớp
     * điều kiện (saved == null) và nhận lỗi 409 "vui lòng tải lại".
     */
    private DonHang doiTrangThai(DonHang don, Update update) {
        DonHang saved = mongo.findAndModify(
                Query.query(Criteria.where("_id").is(don.getId()).and("trangThai").is(don.getTrangThai())),
                update,
                FindAndModifyOptions.options().returnNew(true),
                DonHang.class);
        if (saved == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn vừa được cập nhật ở nơi khác, vui lòng tải lại");
        }
        return saved;
    }

    /**
     * Tự trả bàn về "Trống" khi khách đã nhận đủ món (gọi sau khi 1 đơn hoàn thành hoặc bị huỷ).
     * Chỉ trả bàn khi ĐỦ cả 4 điều kiện, xét trong lượt khách hiện tại (từ mốc thoiGianDatBan):
     *   1. Bàn đang "Có khách" và đơn vừa đổi thuộc lượt khách này (không phải đơn cũ của khách trước).
     *   2. Không còn đơn nào đang xử lý (Chờ xác nhận / Pha chế / Mang ra).
     *   3. Có ít nhất 1 đơn đã hoàn thành. Khách huỷ hết mà chưa nhận món nào -> giữ bàn, khách có thể gọi lại.
     *   4. Giỏ của bàn đang trống. Khách đang chọn món gọi thêm -> giữ bàn, nếu không giỏ đang chọn dở sẽ mất.
     * Trả bàn = kết thúc phiên bàn: các máy của khách cũ không đặt món vào bàn này được nữa.
     */
    private void traBanNeuDaXong(DonHang don) {
        Integer idBan = don.getIdBan();
        if (idBan == null || idBan <= 0) return;
        BanAn ban = banAnRepository.findByIdBan(idBan).orElse(null);
        if (ban == null || ban.getTrangThai() != BanAn.CO_KHACH) return;

        Instant batDau = thoiDiem(ban.getThoiGianDatBan());
        if (don.getThoiGian() != null && don.getThoiGian().isBefore(batDau)) return; // đơn của lượt khách trước
        if (repo.existsByIdBanAndTrangThaiLessThanAndThoiGianGreaterThanEqual(idBan, DonHang.HOAN_THANH, batDau)) return;
        if (!repo.existsByIdBanAndTrangThaiAndThoiGianCapNhatGreaterThanEqual(idBan, DonHang.HOAN_THANH, batDau)) return;
        if (mongo.exists(Query.query(Criteria.where("idBan").is(idBan).and("items.0").exists(true)), GioHang.class)) return;

        banAnService.traBanTuDong(idBan);
    }

    private static Instant thoiDiem(String iso) {
        if (iso == null || iso.isBlank()) return Instant.EPOCH;
        try {
            return Instant.parse(iso);
        } catch (Exception e) {
            return Instant.EPOCH;
        }
    }

    /** Số liệu cho trang tổng quan: doanh thu 7 ngày, đơn hôm nay, bàn có khách, món bán chạy. */
    public Map<String, Object> thongKe() {
        LocalDate homNay = LocalDate.now(ZONE);
        Instant tu = homNay.minusDays(6).atStartOfDay(ZONE).toInstant();
        List<DonHang> dons = repo.findByThoiGianGreaterThanEqual(tu);

        Map<LocalDate, Long> doanhThuTheoNgay = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            doanhThuTheoNgay.put(homNay.minusDays(i), 0L);
        }
        long doanhThuHomNay = 0;
        int soDonHomNay = 0;
        Map<String, Integer> soLuongTheoMon = new HashMap<>();

        for (DonHang d : dons) {
            if (d.getThoiGian() == null) continue;
            if (d.getTrangThai() != null && d.getTrangThai() == DonHang.DA_HUY) continue; // đơn huỷ không tính doanh thu
            LocalDate ngay = d.getThoiGian().atZone(ZONE).toLocalDate();
            long tien = d.getTongTien() == null ? 0 : d.getTongTien();
            doanhThuTheoNgay.merge(ngay, tien, Long::sum);
            if (ngay.equals(homNay)) {
                doanhThuHomNay += tien;
                soDonHomNay++;
            }
            for (MonDaChon m : d.getItems()) {
                if (m.getTenMon() != null) soLuongTheoMon.merge(m.getTenMon(), m.getSoLuong(), Integer::sum);
            }
        }

        List<Map<String, Object>> bieuDo = new ArrayList<>();
        doanhThuTheoNgay.forEach((ngay, tien) -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("ngay", ngay.toString());
            p.put("doanhThu", tien);
            bieuDo.add(p);
        });

        List<Map<String, Object>> monBanChay = soLuongTheoMon.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("tenMon", e.getKey());
                    p.put("soLuong", e.getValue());
                    return p;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("doanhThuHomNay", doanhThuHomNay);
        result.put("soDonHomNay", soDonHomNay);
        result.put("donDangXuLy", repo.countByTrangThaiLessThan(DonHang.HOAN_THANH));
        result.put("banCoKhach", banAnRepository.countByTrangThai(BanAn.CO_KHACH));
        result.put("tongSoBan", banAnRepository.count());
        result.put("doanhThu7Ngay", bieuDo);
        result.put("monBanChay", monBanChay);
        return result;
    }

    /** Tối đa số đơn trả về trong bảng chi tiết của báo cáo (tránh trả quá nhiều dữ liệu). */
    private static final int BAO_CAO_MAX_DON = 500;

    /**
     * Báo cáo doanh thu chi tiết trong khoảng ngày [tu, den] (giờ Việt Nam).
     * Doanh thu = tổng tiền các đơn ĐÃ HOÀN THÀNH; đơn đang xử lý và đơn huỷ được thống kê riêng.
     */
    public Map<String, Object> baoCao(LocalDate tu, LocalDate den) {
        if (tu == null) tu = LocalDate.now(ZONE);
        if (den == null) den = tu;
        if (den.isBefore(tu)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải sau ngày bắt đầu");
        }
        long soNgay = ChronoUnit.DAYS.between(tu, den) + 1;
        if (soNgay > 366) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ xem được tối đa 1 năm mỗi lần");
        }
        Document f = tongHop(tu, den);

        // Theo trạng thái: hoàn thành = doanh thu; huỷ / đang xử lý (gồm đơn cũ thiếu trạng thái) thống kê riêng
        long doanhThu = 0, tienDangXuLy = 0, tienHuy = 0;
        long soDonHoanThanh = 0, soDonDangXuLy = 0, soDonHuy = 0, tongSoDon = 0;
        for (Document g : f.getList("theoTrangThai", Document.class)) {
            Object tt = g.get("_id");
            long n = so(g.get("soDon")), tien = so(g.get("tien"));
            tongSoDon += n;
            if (tt instanceof Number t && t.intValue() == DonHang.HOAN_THANH) { soDonHoanThanh = n; doanhThu = tien; }
            else if (tt instanceof Number t && t.intValue() == DonHang.DA_HUY) { soDonHuy = n; tienHuy = tien; }
            else { soDonDangXuLy += n; tienDangXuLy += tien; }
        }
        List<Document> soLyDoc = f.getList("soLy", Document.class);
        long soLy = soLyDoc.isEmpty() ? 0 : so(soLyDoc.get(0).get("n"));

        // Theo ngày: đủ mọi ngày trong khoảng (ngày không có đơn = 0)
        Map<String, Document> ngayCo = new HashMap<>();
        for (Document g : f.getList("theoNgay", Document.class)) ngayCo.put(g.getString("_id"), g);
        List<Map<String, Object>> ngayList = new ArrayList<>();
        for (LocalDate d = tu; !d.isAfter(den); d = d.plusDays(1)) {
            Document g = ngayCo.get(d.toString());
            ngayList.add(dong("ngay", d.toString(), "doanhThu", g == null ? 0L : so(g.get("doanhThu")),
                    "soDon", g == null ? 0L : so(g.get("soDon")), "soDonHuy", g == null ? 0L : so(g.get("soDonHuy"))));
        }

        long[][] theoGio = new long[24][2];
        for (Document g : f.getList("theoGio", Document.class)) {
            int h = ((Number) g.get("_id")).intValue();
            theoGio[h][0] = so(g.get("doanhThu"));
            theoGio[h][1] = so(g.get("soDon"));
        }
        List<Map<String, Object>> gioList = new ArrayList<>();
        for (int h = 0; h < 24; h++) gioList.add(dong("gio", h, "doanhThu", theoGio[h][0], "soDon", theoGio[h][1]));

        Map<String, long[]> theoKenh = new LinkedHashMap<>();
        theoKenh.put("Tại bàn (QR)", new long[2]);
        theoKenh.put("Tại quầy (POS)", new long[2]);
        for (Document g : f.getList("theoKenh", Document.class)) {
            long[] k = theoKenh.get(Boolean.TRUE.equals(g.get("_id")) ? "Tại quầy (POS)" : "Tại bàn (QR)");
            k[0] = so(g.get("doanhThu"));
            k[1] = so(g.get("soDon"));
        }
        List<Map<String, Object>> kenhList = new ArrayList<>();
        theoKenh.forEach((ten, v) -> kenhList.add(dong("kenh", ten, "doanhThu", v[0], "soDon", v[1])));

        List<Map<String, Object>> monList = f.getList("theoMon", Document.class).stream()
                .map(g -> dong("tenMon", g.getString("_id"), "soLuong", so(g.get("soLuong")), "doanhThu", so(g.get("doanhThu"))))
                .toList();

        Map<Integer, String> tenBan = new HashMap<>();
        banAnRepository.findAll().forEach(b -> tenBan.put(b.getIdBan(), b.getTenBan()));
        List<Map<String, Object>> banList = f.getList("theoBan", Document.class).stream()
                .map(g -> {
                    int idBan = ((Number) g.get("_id")).intValue();
                    return dong("idBan", idBan, "tenBan", tenBan.getOrDefault(idBan, "Bàn " + idBan),
                            "doanhThu", so(g.get("doanhThu")), "soDon", so(g.get("soDon")));
                })
                .toList();

        // So sánh với kỳ trước cùng độ dài (vd. 7 ngày này so với 7 ngày trước đó)
        long doanhThuKyTruoc = doanhThuHoanThanh(tu.minusDays(soNgay), tu.minusDays(1));

        // Bảng chi tiết chỉ lấy BAO_CAO_MAX_DON đơn mới nhất; các con số tổng ở trên vẫn tính trên toàn bộ khoảng
        List<DonHang> dons = mongo.find(Query.query(khoangNgay(tu, den))
                .with(Sort.by(Sort.Direction.DESC, "thoiGian")).limit(BAO_CAO_MAX_DON), DonHang.class);

        Map<String, Object> tongQuan = new LinkedHashMap<>();
        tongQuan.put("doanhThu", doanhThu);
        tongQuan.put("doanhThuKyTruoc", doanhThuKyTruoc);
        tongQuan.put("thayDoiPhanTram", doanhThuKyTruoc > 0 ? Math.round((doanhThu - doanhThuKyTruoc) * 1000.0 / doanhThuKyTruoc) / 10.0 : null);
        tongQuan.put("soDonHoanThanh", soDonHoanThanh);
        tongQuan.put("giaTriTrungBinh", soDonHoanThanh > 0 ? doanhThu / soDonHoanThanh : 0);
        tongQuan.put("soLy", soLy);
        tongQuan.put("soDonDangXuLy", soDonDangXuLy);
        tongQuan.put("tienDangXuLy", tienDangXuLy);
        tongQuan.put("soDonHuy", soDonHuy);
        tongQuan.put("tienHuy", tienHuy);
        tongQuan.put("tyLeHuy", tongSoDon > 0 ? Math.round(soDonHuy * 1000.0 / tongSoDon) / 10.0 : 0);
        tongQuan.put("tongSoDon", tongSoDon);

        List<Map<String, Object>> donList = dons.stream()
                .map(d -> dong("id", d.getId(), "idDonHang", d.getIdDonHang(), "thoiGian", d.getThoiGian(),
                        "idBan", d.getIdBan(), "soMon", d.getItems().stream().mapToInt(MonDaChon::getSoLuong).sum(),
                        "tongTien", d.getTongTien(), "trangThai", d.getTrangThai(),
                        "nguoiHuy", d.getNguoiHuy(), "lyDoHuy", d.getLyDoHuy()))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tu", tu.toString());
        result.put("den", den.toString());
        result.put("soNgay", soNgay);
        result.put("tongQuan", tongQuan);
        result.put("theoNgay", ngayList);
        result.put("theoGio", gioList);
        result.put("theoKenh", kenhList);
        result.put("theoMon", monList);
        result.put("theoBan", banList);
        result.put("donHang", donList);
        result.put("donHangBiCat", tongSoDon > BAO_CAO_MAX_DON);
        result.put("hienTai", dong("banCoKhach", banAnRepository.countByTrangThai(BanAn.CO_KHACH),
                "tongSoBan", banAnRepository.count(),
                "donDangXuLy", repo.countByTrangThaiLessThan(DonHang.HOAN_THANH)));
        return result;
    }

    /** Đơn đặt trong các ngày [tu, den] theo giờ Việt Nam. */
    private static Criteria khoangNgay(LocalDate tu, LocalDate den) {
        return Criteria.where("thoiGian").gte(tu.atStartOfDay(ZONE).toInstant())
                .lt(den.plusDays(1).atStartOfDay(ZONE).toInstant());
    }

    /**
     * Tính mọi con số của báo cáo ngay trong MongoDB (aggregation), không tải từng đơn về Java.
     *
     * Vì sao: tải hết đơn rồi cộng trong Java thì 1 năm dữ liệu (~180.000 đơn) mất ~14 giây và tốn nhiều RAM.
     * Để database tự cộng thì chỉ trả về vài trăm con số tổng, cả năm ~3 giây.
     *
     * Pipeline: $match lọc đơn trong khoảng ngày (dùng index thoiGian), rồi $facet chạy nhiều phép thống kê
     * song song trên cùng tập đơn đó, mỗi nhánh cho 1 bảng kết quả:
     *   theoTrangThai, soLy (tổng số ly đã bán), theoNgay, theoGio, theoKenh (QR / POS), theoBan, theoMon.
     * Ngày / giờ tính theo múi giờ Việt Nam (tham số timezone), nếu không đơn lúc 0h–7h sẽ bị tính sang ngày hôm trước.
     * "Tại quầy" = loai TAI_QUAY, hoặc idBan <= 0 / không có idBan (đơn dữ liệu cũ chưa có trường loai).
     */
    private Document tongHop(LocalDate tu, LocalDate den) {
        String tz = ZONE.getId();
        Document xong = new Document("trangThai", DonHang.HOAN_THANH);
        Document laXong = new Document("$eq", List.of("$trangThai", DonHang.HOAN_THANH));
        Document taiQuay = new Document("$or", List.of(
                new Document("$eq", List.of("$loai", DonHang.LOAI_TAI_QUAY)),
                new Document("$lte", List.of(new Document("$ifNull", List.of("$idBan", 0)), 0))));
        Document facet = new Document()
                .append("theoTrangThai", List.of(new Document("$group", new Document("_id", "$trangThai")
                        .append("soDon", new Document("$sum", 1)).append("tien", new Document("$sum", "$tongTien")))))
                .append("soLy", List.of(new Document("$match", xong), new Document("$unwind", "$items"),
                        new Document("$group", new Document("_id", null).append("n", new Document("$sum", "$items.soLuong")))))
                .append("theoNgay", List.of(new Document("$group", new Document("_id",
                                new Document("$dateToString", new Document("format", "%Y-%m-%d").append("date", "$thoiGian").append("timezone", tz)))
                        .append("doanhThu", new Document("$sum", new Document("$cond", List.of(laXong, "$tongTien", 0))))
                        .append("soDon", new Document("$sum", new Document("$cond", List.of(laXong, 1, 0))))
                        .append("soDonHuy", new Document("$sum", new Document("$cond",
                                List.of(new Document("$eq", List.of("$trangThai", DonHang.DA_HUY)), 1, 0)))))))
                .append("theoGio", List.of(new Document("$match", xong), new Document("$group",
                        new Document("_id", new Document("$hour", new Document("date", "$thoiGian").append("timezone", tz)))
                                .append("doanhThu", new Document("$sum", "$tongTien")).append("soDon", new Document("$sum", 1)))))
                .append("theoKenh", List.of(new Document("$match", xong), new Document("$group",
                        new Document("_id", taiQuay)
                                .append("doanhThu", new Document("$sum", "$tongTien")).append("soDon", new Document("$sum", 1)))))
                .append("theoBan", List.of(new Document("$match", new Document("$expr", new Document("$and",
                                List.of(laXong, new Document("$not", List.of(taiQuay)))))),
                        new Document("$group", new Document("_id", "$idBan")
                                .append("doanhThu", new Document("$sum", "$tongTien")).append("soDon", new Document("$sum", 1))),
                        new Document("$sort", new Document("doanhThu", -1))))
                .append("theoMon", List.of(new Document("$match", xong), new Document("$unwind", "$items"),
                        new Document("$match", new Document("items.tenMon", new Document("$ne", null))),
                        new Document("$group", new Document("_id", "$items.tenMon")
                                .append("soLuong", new Document("$sum", "$items.soLuong"))
                                .append("doanhThu", new Document("$sum", "$items.thanhTien"))),
                        new Document("$sort", new Document("doanhThu", -1).append("_id", 1))));
        List<Document> pipeline = List.of(
                new Document("$match", Query.query(khoangNgay(tu, den)).getQueryObject()),
                new Document("$facet", facet));
        Document kq = mongo.getCollection(mongo.getCollectionName(DonHang.class)).aggregate(pipeline).first();
        return kq != null ? kq : new Document();
    }

    /** Tổng tiền đơn hoàn thành trong các ngày [tu, den] (tính trong MongoDB). */
    private long doanhThuHoanThanh(LocalDate tu, LocalDate den) {
        List<Document> pipeline = List.of(
                new Document("$match", Query.query(khoangNgay(tu, den).and("trangThai").is(DonHang.HOAN_THANH)).getQueryObject()),
                new Document("$group", new Document("_id", null).append("tong", new Document("$sum", "$tongTien"))));
        Document kq = mongo.getCollection(mongo.getCollectionName(DonHang.class)).aggregate(pipeline).first();
        return kq == null ? 0 : so(kq.get("tong"));
    }

    private static long so(Object v) {
        return v instanceof Number n ? n.longValue() : 0;
    }

    /** Map giữ thứ tự từ các cặp khoá–giá trị: dong("a", 1, "b", 2). */
    private static Map<String, Object> dong(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private void ghiLichSu(DonHang don) {
        if (don.getIdDonHang() == null || lichSuRepository.existsByIdDonHang(don.getIdDonHang())) return;
        long tong = don.getTongTien() == null ? 0 : don.getTongTien();
        LichSuBanHang ls = new LichSuBanHang();
        ls.setIdDonHang(don.getIdDonHang());
        ls.setIdBan(don.getIdBan());
        ls.setTongTienGoc(tong);
        ls.setTienGiam(0);
        ls.setTongTienThanhToan(tong);
        ls.setThoiGian(Instant.now());
        lichSuRepository.save(ls);
    }

    private static void kiemTraTrangThai(Integer trangThai) {
        if (!DonHang.trangThaiHopLe(trangThai)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái đơn phải từ 0 đến 3");
        }
    }
}
