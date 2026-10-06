package chandanv.local.chandanv.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import chandanv.local.chandanv.models.entity.ChiTietDonHang;
import chandanv.local.chandanv.models.entity.LichSuBanHang;
import chandanv.local.chandanv.services.ChiTietDonHangService;
import chandanv.local.chandanv.services.LichSuBanHangService;
import chandanv.local.chandanv.websocket.OrderWebSocketHandler;

@RestController
@RequestMapping("/api/chi-tiet-don")
@CrossOrigin
public class ChiTietDonHangController {

    private final ChiTietDonHangService service;
    private final LichSuBanHangService lichSuBanHangService;
    private final OrderWebSocketHandler webSocketHandler;

    public ChiTietDonHangController(
            ChiTietDonHangService service,
            LichSuBanHangService lichSuBanHangService,
            OrderWebSocketHandler webSocketHandler) {
        this.service = service;
        this.lichSuBanHangService = lichSuBanHangService;
        this.webSocketHandler = webSocketHandler;
    }

    @GetMapping
    public List<ChiTietDonHang> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Optional<ChiTietDonHang> getById(@PathVariable String id) {
        return service.getById(id);
    }

    @PostMapping
    public ChiTietDonHang create(@RequestBody ChiTietDonHang chiTietDonHang) {
        ChiTietDonHang saved = service.save(chiTietDonHang);
        webSocketHandler.broadcast(String.format("{\"type\":\"NEW_ORDER\",\"orderId\":\"%s\"}", saved.getIdChiTiet()));
        return saved;
    }

    @PutMapping("/{id}")
    public ChiTietDonHang update(
            @PathVariable String id,
            @RequestBody ChiTietDonHang chiTietDonHang) {
        return service.update(id, chiTietDonHang);
    }

    @PatchMapping("/{id}/trang-thai")
    public ChiTietDonHang updateTrangThai(
            @PathVariable String id,
            @RequestBody Map<String, Integer> body) {
        ChiTietDonHang updated = service.updateTrangThai(id, body.get("trangThai"));
        webSocketHandler.broadcast(String.format("{\"type\":\"ORDER_STATUS_UPDATED\",\"orderId\":\"%s\",\"status\":%d}", id, updated.getTrangThai()));
        return updated;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    /*  THÊM MỚI – KHÔNG ẢNH HƯỞNG CODE CŨ  */
    @PostMapping("/hoan-thanh/{id}")
    public void hoanThanh(@PathVariable String id) {
        ChiTietDonHang ct = service.getById(id).orElseThrow();

        LichSuBanHang ls = new LichSuBanHang();
        ls.setIdLichSu((int) (System.currentTimeMillis() % 100000));
        ls.setIdChiTiet(ct.getIdChiTiet());
        ls.setTongTienGoc(ct.getDonGiaThanhTien().getThanhTien());
        ls.setTienGiam(0);
        ls.setTongTienThanhToan(ct.getDonGiaThanhTien().getThanhTien());

        lichSuBanHangService.save(ls);
        service.delete(id);
        webSocketHandler.broadcast(String.format("{\"type\":\"ORDER_STATUS_UPDATED\",\"orderId\":\"%s\",\"status\":3}", id));
    }

    @GetMapping("/don-hang/{idDonHang}")
public List<ChiTietDonHang> getByIdDonHang(@PathVariable int idDonHang) {
    return service.getByIdDonHang(idDonHang);
}
}
