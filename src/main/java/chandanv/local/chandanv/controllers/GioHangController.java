package chandanv.local.chandanv.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import chandanv.local.chandanv.models.dto.ThemMonRequest;
import chandanv.local.chandanv.models.entity.GioHang;
import chandanv.local.chandanv.services.AuthService;
import chandanv.local.chandanv.services.GioHangService;
import jakarta.servlet.http.HttpServletRequest;

/** Giỏ hàng chung của bàn – cần mã riêng của máy đã được duyệt ở bàn (header X-Table-Token) hoặc token nhân viên. */
@RestController
@RequestMapping("/api/gio-hang/{idBan}")
public class GioHangController {

    private final GioHangService service;
    private final AuthService auth;

    public GioHangController(GioHangService service, AuthService auth) {
        this.service = service;
        this.auth = auth;
    }

    @GetMapping
    public GioHang get(@PathVariable int idBan, HttpServletRequest req) {
        auth.yeuCauBanHoacNhanVien(req, idBan);
        return service.lay(idBan);
    }

    @PostMapping("/items")
    public GioHang themMon(@PathVariable int idBan, @RequestBody ThemMonRequest body, HttpServletRequest req) {
        auth.yeuCauBanHoacNhanVien(req, idBan);
        return service.themMon(idBan, body);
    }

    /** body {"soLuong": n}; n <= 0 sẽ xoá dòng. */
    @PatchMapping("/items/{itemId}")
    public GioHang capNhatSoLuong(@PathVariable int idBan, @PathVariable String itemId,
                                  @RequestBody Map<String, Integer> body, HttpServletRequest req) {
        auth.yeuCauBanHoacNhanVien(req, idBan);
        return service.capNhatSoLuong(idBan, itemId, body.get("soLuong"));
    }

    @DeleteMapping("/items/{itemId}")
    public GioHang xoaMon(@PathVariable int idBan, @PathVariable String itemId, HttpServletRequest req) {
        auth.yeuCauBanHoacNhanVien(req, idBan);
        return service.xoaMon(idBan, itemId);
    }

    @DeleteMapping
    public ResponseEntity<Void> xoaGio(@PathVariable int idBan, HttpServletRequest req) {
        auth.yeuCauBanHoacNhanVien(req, idBan);
        service.xoaGio(idBan);
        return ResponseEntity.noContent().build();
    }
}
