package chandanv.local.chandanv.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import chandanv.local.chandanv.models.entity.DonHang;
import chandanv.local.chandanv.services.DonHangService;

@RestController
@RequestMapping("/api/don-hang")
@CrossOrigin(origins = "*")
public class DonHangController {

    private final DonHangService service;

    public DonHangController(DonHangService service) {
        this.service = service;
    }

    @PostMapping
    public DonHang create(@RequestBody DonHang donHang) {
        return service.create(donHang);
    }

    @GetMapping("/ban/{idBan}")
public DonHang getLatestByBan(@PathVariable Integer idBan) {
    return service.getLatestByIdBan(idBan);
}
}