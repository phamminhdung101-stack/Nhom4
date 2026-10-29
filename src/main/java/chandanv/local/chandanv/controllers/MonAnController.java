package chandanv.local.chandanv.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import chandanv.local.chandanv.models.entity.MonAn;
import chandanv.local.chandanv.services.MonAnService;
import chandanv.local.chandanv.services.TuyChonMon;

/** Thực đơn – công khai. */
@RestController
@RequestMapping("/api/mon-an")
public class MonAnController {

    private final MonAnService service;

    public MonAnController(MonAnService service) {
        this.service = service;
    }

    @GetMapping
    public List<MonAn> getAllMonAn() {
        return service.getAll();
    }

    @GetMapping("/loai/{idLoai}")
    public List<MonAn> getMonAnByLoai(@PathVariable int idLoai) {
        return service.getByIdLoai(idLoai);
    }

    /** Bảng size / topping / độ ngọt kèm giá. */
    @GetMapping("/tuy-chon")
    public Map<String, Object> tuyChon() {
        return TuyChonMon.toMap();
    }
}
