package chandanv.local.chandanv.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import chandanv.local.chandanv.models.entity.Admin;
import chandanv.local.chandanv.models.entity.BanAn;
import chandanv.local.chandanv.models.entity.ThietBi;
import chandanv.local.chandanv.security.AuditLog;
import chandanv.local.chandanv.security.ClientIp;
import chandanv.local.chandanv.services.AuthService;
import chandanv.local.chandanv.services.BanAnService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/ban-an")
public class BanAnController {

    private final BanAnService service;
    private final AuthService auth;

    public BanAnController(BanAnService service, AuthService auth) {
        this.service = service;
        this.auth = auth;
    }

    /** Danh sách bàn (có mã QR) – chỉ nhân viên. */
    @GetMapping
    public List<BanAn> danhSach(HttpServletRequest req) {
        auth.yeuCauNhanVien(req);
        return service.layDanhSachBan();
    }

    /** Khách kiểm tra mã QR có hợp lệ không (trang welcome gọi khi vừa quét). */
    @GetMapping("/{idBan}/xac-thuc")
    public Map<String, Object> xacThuc(@PathVariable int idBan, HttpServletRequest req) {
        auth.yeuCauBanHoacNhanVien(req, idBan);
        BanAn ban = service.layBan(idBan);
        return Map.of("idBan", ban.getIdBan(), "tenBan", ban.getTenBan(), "trangThai", ban.getTrangThai());
    }

    /**
     * Điện thoại quét QR xin vào bàn. body {"qr": "<mã in trong QR>"}.
     * Trả về mã riêng của máy (dùng cho header X-Table-Token) và trạng thái:
     * DA_DUYET (chủ bàn – dùng ngay) hoặc CHO_DUYET (bàn đã có người – chờ nhân viên cho phép).
     */
    @PostMapping("/{idBan}/vao-ban")
    public Map<String, Object> vaoBan(@PathVariable int idBan, @RequestBody(required = false) Map<String, String> body,
                                      HttpServletRequest req) {
        String qr = body == null ? null : body.get("qr");
        if (!auth.maQRHopLe(idBan, qr)) {
            AuditLog.warn("TABLE_QR_INVALID", "ban=" + idBan + " ip=" + ClientIp.of(req));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mã QR của bàn không hợp lệ, vui lòng quét lại mã trên bàn");
        }
        ThietBi may = service.vaoBan(idBan);
        Map<String, Object> res = thongTinMay(may);
        res.put("token", may.getToken());
        res.put("idBan", idBan);
        res.put("tenBan", service.layBan(idBan).getTenBan());
        return res;
    }

    /** Trạng thái máy này trong phiên bàn (máy chờ duyệt hỏi định kỳ). 403 = bị từ chối / phiên đã kết thúc. */
    @GetMapping("/{idBan}/thiet-bi/toi")
    public Map<String, Object> mayCuaToi(@PathVariable int idBan, HttpServletRequest req) {
        ThietBi may = auth.timThietBi(idBan, req.getHeader(AuthService.TABLE_HEADER))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Máy của bạn chưa được vào bàn hoặc phiên bàn đã kết thúc, vui lòng quét lại mã QR trên bàn"));
        Map<String, Object> res = thongTinMay(may);
        res.put("idBan", idBan);
        res.put("tenBan", service.layBan(idBan).getTenBan());
        return res;
    }

    /** Nhân viên cho phép máy đang chờ vào bàn. */
    @PostMapping("/{idBan}/thiet-bi/{maMay}/duyet")
    public Map<String, Object> duyetMay(@PathVariable int idBan, @PathVariable String maMay, HttpServletRequest req) {
        Admin nv = auth.yeuCauNhanVien(req);
        service.duyetMay(idBan, maMay);
        AuditLog.info("TABLE_APPROVE", "ban=" + idBan + " may=" + maMay + " boi=" + nv.getUsername());
        return Map.of("message", "Đã cho phép máy vào bàn " + idBan);
    }

    /** Nhân viên từ chối máy chờ duyệt / mời 1 máy ra khỏi bàn. */
    @DeleteMapping("/{idBan}/thiet-bi/{maMay}")
    public Map<String, Object> moiRaMay(@PathVariable int idBan, @PathVariable String maMay, HttpServletRequest req) {
        auth.yeuCauNhanVien(req);
        service.moiRaMay(idBan, maMay);
        return Map.of("message", "Đã mời máy ra khỏi bàn " + idBan);
    }

    private static Map<String, Object> thongTinMay(ThietBi may) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", may.getId());
        m.put("ten", may.getTen());
        m.put("laChu", may.isLaChu());
        m.put("trangThai", may.getTrangThai());
        return m;
    }

    /** Khách vừa vào bàn -> đánh dấu "có khách". */
    @PutMapping("/{idBan}/co-khach")
    public Map<String, Object> coKhach(@PathVariable int idBan, HttpServletRequest req) {
        auth.yeuCauBanHoacNhanVien(req, idBan);
        BanAn ban = service.capNhatTrangThai(idBan, BanAn.CO_KHACH);
        return Map.of("idBan", ban.getIdBan(), "trangThai", ban.getTrangThai());
    }

    /** Nhân viên đổi trạng thái bàn (0 = hoàn thành/trống -> xoá phiên chat). */
    @PutMapping("/{idBan}/trang-thai/{trangThai}")
    public BanAn capNhatTrangThai(@PathVariable int idBan, @PathVariable int trangThai, HttpServletRequest req) {
        auth.yeuCauNhanVien(req);
        return service.capNhatTrangThai(idBan, trangThai);
    }

    /** body (tuỳ chọn): {"idBans": [1, 3, 5]} -> chỉ reset các bàn đó; không có body -> reset toàn bộ. */
    @PutMapping("/reset")
    public Map<String, Object> reset(@RequestBody(required = false) Map<String, List<Integer>> body, HttpServletRequest req) {
        auth.yeuCauNhanVien(req);
        List<Integer> idBans = body == null ? null : body.get("idBans");
        if (idBans == null) {
            service.resetTatCaBan();
            return Map.of("message", "Đã reset toàn bộ bàn");
        }
        int soBan = service.resetCacBan(idBans);
        return Map.of("message", "Đã reset " + soBan + " bàn", "soBan", soBan);
    }

    /** Lưu ảnh QR: body {"image": "data:image/png;base64,..."} */
    @PostMapping("/{idBan}/qr-image")
    public Map<String, Object> luuAnhQR(@PathVariable int idBan, @RequestBody Map<String, String> body,
                                        HttpServletRequest req) {
        auth.yeuCauNhanVien(req);
        service.luuAnhQR(idBan, body.get("image"));
        return Map.of("message", "Đã lưu ảnh QR cho bàn " + idBan);
    }

    @GetMapping("/{idBan}/qr-image")
    public ResponseEntity<byte[]> taiAnhQR(@PathVariable int idBan, HttpServletRequest req) {
        auth.yeuCauNhanVien(req);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=QR_Ban_" + idBan + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(service.layAnhQR(idBan));
    }

    /** Cấp mã QR mới (mã in cũ hết hiệu lực). */
    @PostMapping("/{idBan}/doi-ma-qr")
    public BanAn doiMaQR(@PathVariable int idBan, HttpServletRequest req) {
        auth.yeuCauNhanVien(req);
        return service.doiMaQR(idBan);
    }
}
