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

@RestController
@RequestMapping("/api/chi-tiet-don")
@CrossOrigin
public class ChiTietDonHangController {

    private final ChiTietDonHangService service;
    private final LichSuBanHangService lichSuBanHangService;

    public ChiTietDonHangController(
            ChiTietDonHangService service,
            LichSuBanHangService lichSuBanHangService) {
        this.service = service;
        this.lichSuBanHangService = lichSuBanHangService;
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
        return service.save(chiTietDonHang);
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
        return service.updateTrangThai(id, body.get("trangThai"));
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
    }

    @GetMapping("/don-hang/{idDonHang}")
public List<ChiTietDonHang> getByIdDonHang(@PathVariable int idDonHang) {
    return service.getByIdDonHang(idDonHang);
}
}
