package chandanv.local.chandanv.controllers.api;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import chandanv.local.chandanv.models.entity.BanAn;
import chandanv.local.chandanv.services.BanAnService;

@RestController
@RequestMapping("/api/ban-an")
@CrossOrigin
public class BanAnApiController {

    private final BanAnService service;

    public BanAnApiController(BanAnService service) {
        this.service = service;
    }

    @GetMapping
    public List<BanAn> layDanhSachBan() {
        return service.layDanhSachBan();
    }

    @PostMapping("/luu-qr-image/{idBan}")
    public void luuQrImage(
            @PathVariable int idBan,
            @RequestBody String base64Image) {

        service.luuMaQR(idBan, base64Image);
    }

    @PutMapping("/{idBan}/trang-thai/{trangThai}")
    public void capNhatTrangThai(
            @PathVariable int idBan,
            @PathVariable int trangThai) {

        service.capNhatTrangThai(idBan, trangThai);
    }

    @PutMapping("/reset")
    public void resetTrangThaiTatCaBan() {
        service.resetTatCaBan();
    }
}
