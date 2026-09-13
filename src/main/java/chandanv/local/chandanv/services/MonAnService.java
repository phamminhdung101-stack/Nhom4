package chandanv.local.chandanv.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import chandanv.local.chandanv.models.entity.MonAn;
import chandanv.local.chandanv.repositories.MonAnRepository;

@Service
public class MonAnService {

    private final MonAnRepository repository;
    
    // Cấu hình URL tĩnh trỏ vào thư mục upload
    private static final String BASE_IMAGE_URL = "http://localhost:8080/uploads/";

    public MonAnService(MonAnRepository repository) {
        this.repository = repository;
    }

    public List<MonAn> getAll() {
        // Xử lý stream để map thêm URL ảnh cho từng phần tử
        return repository.findAll().stream()
                .map(this::attachImageUrl)
                .collect(Collectors.toList());
    }

    public List<MonAn> getByIdLoai(int idLoai) {
        // Xử lý stream để map thêm URL ảnh cho từng phần tử
        return repository.findByIdLoai(idLoai).stream()
                .map(this::attachImageUrl)
                .collect(Collectors.toList());
    }

    // Hàmset thuộc tính image theo định dạng "BASE_URL + idMon + .jpg"
    private MonAn attachImageUrl(MonAn monAn) {
        monAn.setImage(BASE_IMAGE_URL + monAn.getIdMon() + ".jpg");
        return monAn;
    }
}