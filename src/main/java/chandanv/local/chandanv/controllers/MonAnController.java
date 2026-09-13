package chandanv.local.chandanv.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import chandanv.local.chandanv.models.entity.MonAn;
import chandanv.local.chandanv.services.MonAnService;

@RestController
@RequestMapping("/api/mon-an")
@CrossOrigin(origins = "*")
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
}
