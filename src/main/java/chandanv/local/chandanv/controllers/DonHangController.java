package chandanv.local.chandanv.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import chandanv.local.chandanv.models.dto.TaiQuayRequest;
import chandanv.local.chandanv.models.entity.Admin;
import chandanv.local.chandanv.models.entity.DonHang;
import chandanv.local.chandanv.models.entity.ThietBi;
import chandanv.local.chandanv.services.AuthService;
import chandanv.local.chandanv.services.DonHangService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/don-hang")
public class DonHangController {

    private final DonHangService service;
    private final AuthService auth;

    public DonHangController(DonHangService service, AuthService auth) {
        this.service = service;
        this.auth = auth;
    }

    /** Khách gọi món: chuyển giỏ của bàn thành đơn. body (tuỳ chọn): {"ghiChu": "..."} */
    @PostMapping("/dat-mon/{idBan}")
    public ResponseEntity<DonHang> datMon(@PathVariable int idBan,
                                          @RequestBody(required = false) Map<String, String> body,
                                          HttpServletRequest req) {
        auth.yeuCauBanHoacNhanVien(req, idBan);
        String ghiChu = body == null ? null : body.get("ghiChu");
        String nguoiDat = auth.thietBiCua(req, idBan).map(ThietBi::getTen).orElse("Nhân viên");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.datMonTuGio(idBan, ghiChu, nguoiDat));
    }

    /** Đơn mới nhất của bàn; 204 nếu bàn chưa có đơn. */
    @GetMapping("/ban/{idBan}")
    public ResponseEntity<DonHang> donMoiNhat(@PathVariable int idBan, HttpServletRequest req) {
        auth.yeuCauBanHoacNhanVien(req, idBan);
        return service.donMoiNhatCuaBan(idBan)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Mọi đơn của lượt khách hiện tại ở bàn (mới nhất trước) – trang theo dõi đơn của khách. */
    @GetMapping("/ban/{idBan}/luot-nay")
    public List<DonHang> donCuaLuotKhach(@PathVariable int idBan, HttpServletRequest req) {
        // Máy của phiên vừa kết thúc (bàn đã tự trả sau khi nhận đủ món) vẫn xem lại được đơn của mình
        if (auth.laMayPhienCu(idBan, req.getHeader(AuthService.TABLE_HEADER))
                && !auth.tokenBanHopLe(idBan, req.getHeader(AuthService.TABLE_HEADER))) {
            return service.donCuaPhienCu(idBan);
        }
        auth.yeuCauBanHoacNhanVien(req, idBan);
        return service.donCuaLuotKhach(idBan);
    }

    /**
     * Danh sách đơn cho màn hình bếp (mới nhất trước) – chỉ nhân viên. Mặc định: đơn hôm nay + đơn cũ chưa xong.
     * ?trangThai=0..4 (4 = đã huỷ) để lọc; ?tu=2026-10-01 để xem từ ngày khác (tối đa 500 đơn).
     */
    @GetMapping
    public List<DonHang> danhSach(@RequestParam(required = false) Integer trangThai,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
                                  HttpServletRequest req) {
        auth.yeuCauNhanVien(req);
        return service.danhSach(trangThai, tu);
    }

    /** body {"trangThai": 0..3} */
    @PatchMapping("/{id}/trang-thai")
    public DonHang capNhatTrangThai(@PathVariable String id, @RequestBody Map<String, Integer> body,
                                    HttpServletRequest req) {
        auth.yeuCauNhanVien(req);
        return service.capNhatTrangThai(id, body.get("trangThai"));
    }

    /**
     * Huỷ đơn. body (tuỳ chọn): {"lyDo": "..."}
     *  - Nhân viên: huỷ được mọi đơn chưa hoàn thành (vd. khách nhắn tin xin huỷ).
     *  - Khách (X-Table-Token của đúng bàn): chỉ huỷ được khi đơn còn "Chờ xác nhận".
     */
    @PostMapping("/{id}/huy")
    public DonHang huyDon(@PathVariable String id, @RequestBody(required = false) Map<String, String> body,
                          HttpServletRequest req) {
        String lyDo = body == null ? null : body.get("lyDo");
        Optional<Admin> nhanVien = auth.timNhanVien(req.getHeader(AuthService.ADMIN_HEADER));
        if (nhanVien.isPresent()) {
            return service.huyDon(id, lyDo, nhanVien.get().getUsername(), false);
        }
        DonHang don = service.layDon(id);
        int idBan = don.getIdBan() == null ? 0 : don.getIdBan();
        auth.yeuCauBanHoacNhanVien(req, idBan); // đơn tại quầy (idBan = 0) -> chỉ nhân viên
        String may = auth.thietBiCua(req, idBan).map(t -> " " + t.getTen()).orElse("");
        return service.huyDon(id, lyDo, "Khách" + may + " (Bàn " + idBan + ")", true);
    }

    /** Thu ngân tạo đơn tại quầy. */
    @PostMapping("/tai-quay")
    public ResponseEntity<DonHang> taiQuay(@RequestBody TaiQuayRequest body, HttpServletRequest req) {
        auth.yeuCauNhanVien(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.taoDonTaiQuay(body));
    }

    @GetMapping("/thong-ke")
    public Map<String, Object> thongKe(HttpServletRequest req) {
        auth.yeuCauNhanVien(req);
        return service.thongKe();
    }

    /** Báo cáo doanh thu chi tiết: ?tu=2026-10-01&den=2026-10-28 (mặc định hôm nay). */
    @GetMapping("/bao-cao")
    public Map<String, Object> baoCao(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den,
                                      HttpServletRequest req) {
        auth.yeuCauNhanVien(req);
        return service.baoCao(tu, den);
    }
}
