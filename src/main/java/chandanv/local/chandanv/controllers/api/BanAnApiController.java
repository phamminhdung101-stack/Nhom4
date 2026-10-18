package chandanv.local.chandanv.controllers.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import chandanv.local.chandanv.services.InMemoryChatService;

@RestController
@RequestMapping("/api/ban-an")
@CrossOrigin
public class BanAnApiController {

    private final BanAnService service;

    @Autowired(required = false)
    private InMemoryChatService inMemoryChatService;

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

        // Clear temporary chat cache when table is marked completed/empty (trangThai = 0)
        if (trangThai == 0 && inMemoryChatService != null) {
            inMemoryChatService.clearTableChat(idBan);
        }
    }

    @PutMapping("/reset")
    public void resetTrangThaiTatCaBan() {
        service.resetTatCaBan();

        // Clear all temporary chats when all tables are reset
        if (inMemoryChatService != null) {
            inMemoryChatService.clearAll();
        }
    }
}
