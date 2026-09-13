package chandanv.local.chandanv.controllers;

import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import chandanv.local.chandanv.models.entity.BanAn;
import chandanv.local.chandanv.services.BanAnService;

@Controller
@RequestMapping("/ban-an")
public class BanAnController {

    private final BanAnService service;

    public BanAnController(BanAnService service) {
        this.service = service;
    }

    @GetMapping
    public String danhSachQR(Model model) {
        model.addAttribute("dsBan", service.layDanhSachBan());
        model.addAttribute("baseUrl", "http://localhost:8080/menu");
        return "qrban";
    }

    @PostMapping("/cap-nhat-qr")
    public String capNhatQR(
            @RequestParam("idBan") int idBan,
            @RequestParam("maQR") String maQR) {

        service.luuMaQR(idBan, maQR);
        return "redirect:/ban-an";
    }

    @ResponseBody
    @PutMapping("/{idBan}/trang-thai/{trangThai}")
    public ResponseEntity<Void> capNhatTrangThaiBan(
            @PathVariable int idBan,
            @PathVariable int trangThai) {

        if (trangThai != 0 && trangThai != 1) {
            return ResponseEntity.badRequest().build();
        }

        service.capNhatTrangThai(idBan, trangThai);
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @PutMapping("/reset")
    public ResponseEntity<Void> resetTrangThaiBan() {
        service.resetTatCaBan();
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @PostMapping("/luu-qr-image/{idBan}")
    public ResponseEntity<Void> luuQRImage(
            @PathVariable int idBan,
            @RequestBody String base64Image) {

        service.luuAnhQR(idBan, base64Image);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tai-qr/{idBan}")
    public ResponseEntity<byte[]> taiQR(@PathVariable int idBan) {

        BanAn ban = service.timBanTheoIdBan(idBan);
        if (ban == null || ban.getQrImageBase64() == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] imageBytes = Base64.getDecoder().decode(ban.getQrImageBase64());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=QR_Ban_" + idBan + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(imageBytes);
    }
}