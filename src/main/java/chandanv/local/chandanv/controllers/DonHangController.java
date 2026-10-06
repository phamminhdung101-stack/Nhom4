package chandanv.local.chandanv.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

import chandanv.local.chandanv.models.entity.DonHang;
import chandanv.local.chandanv.services.DonHangService;
import chandanv.local.chandanv.websocket.OrderWebSocketHandler;

@RestController
@RequestMapping("/api/don-hang")
@CrossOrigin(origins = "*")
public class DonHangController {

    private final DonHangService service;
    private final OrderWebSocketHandler webSocketHandler;

    public DonHangController(DonHangService service, OrderWebSocketHandler webSocketHandler) {
        this.service = service;
        this.webSocketHandler = webSocketHandler;
    }

    @PostMapping
    public DonHang create(@RequestBody DonHang donHang) {
        DonHang saved = service.create(donHang);
        webSocketHandler.broadcast(String.format("{\"type\":\"NEW_ORDER\",\"orderId\":\"%s\",\"tableId\":%d}", saved.getIdDonHang(), saved.getIdGioHang()));
        return saved;
    }

    @GetMapping("/ban/{idBan}")
    public DonHang getLatestByBan(@PathVariable Integer idBan) {
        return service.getLatestByIdBan(idBan);
    }

    @GetMapping
    public List<DonHang> getAll() {
        return service.getAll();
    }

    @PatchMapping("/{id}/trang-thai")
    public DonHang updateTrangThai(
            @PathVariable String id,
            @RequestBody Map<String, Integer> body) {
        DonHang updated = service.updateTrangThai(id, body.get("trangThai"));
        webSocketHandler.broadcast(String.format("{\"type\":\"ORDER_STATUS_UPDATED\",\"orderId\":\"%s\",\"status\":%d}", id, updated.getTrangThai()));
        return updated;
    }
}