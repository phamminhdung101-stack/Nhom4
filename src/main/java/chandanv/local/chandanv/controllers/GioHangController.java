package chandanv.local.chandanv.controllers;

import java.util.ArrayList;
import java.util.List;

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

import chandanv.local.chandanv.models.entity.GioHang;
import chandanv.local.chandanv.models.entity.GioHangItem;
import chandanv.local.chandanv.services.GioHangService;

@RestController
@RequestMapping("/api/gio-hang")
@CrossOrigin(origins = "*")
public class GioHangController {

    private final GioHangService service;

    public GioHangController(GioHangService service) {
        this.service = service;
    }

    @GetMapping("/{idBan}")
    public GioHang get(@PathVariable Integer idBan) {

        GioHang gioHang = service.getByIdBan(idBan);

        if (gioHang == null) {
            GioHang empty = new GioHang();
            empty.setIdBan(idBan);
            empty.setItems(new ArrayList<>());
            return empty;
        }

        if (gioHang.getItems() == null) {
            gioHang.setItems(new ArrayList<>());
        }

        return gioHang;
    }

    @PatchMapping("/{idBan}")
    public GioHang update(
            @PathVariable Integer idBan,
            @RequestBody List<GioHangItem> items) {

        if (items == null) {
            items = new ArrayList<>();
        }

        return service.update(idBan, items);
    }

    @PostMapping("/{idBan}/add")
    public GioHang addToCart(
            @PathVariable Integer idBan,
            @RequestBody GioHangItem item) {

        if (item == null) {
            return service.getByIdBan(idBan);
        }

        return service.addToCart(idBan, item);
    }

    @PutMapping("/{idBan}/update")
    public GioHang updateQuantity(
            @PathVariable Integer idBan,
            @RequestBody GioHangItem item) {

        if (item == null || item.getIdMon() == null) {
            return service.getByIdBan(idBan);
        }

        return service.updateQuantity(
                idBan,
                item.getIdMon(),
                item.getSoLuong());
    }

    @DeleteMapping("/{idBan}/remove/{idMon}")
    public GioHang removeItem(
            @PathVariable Integer idBan,
            @PathVariable Integer idMon) {

        if (idMon == null) {
            return service.getByIdBan(idBan);
        }

        return service.removeItem(idBan, idMon);
    }

    @DeleteMapping("/{idBan}")
    public void delete(@PathVariable Integer idBan) {
        service.delete(idBan);
    }
}