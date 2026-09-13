package chandanv.local.chandanv.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import chandanv.local.chandanv.models.entity.ChiTietDonHang;
import chandanv.local.chandanv.repositories.ChiTietDonHangRepository;

@Service
public class ChiTietDonHangService {

    private final ChiTietDonHangRepository repository;

    public ChiTietDonHangService(ChiTietDonHangRepository repository) {
        this.repository = repository;
    }

    public List<ChiTietDonHang> getAll() {
        return repository.findAll();
    }

    public Optional<ChiTietDonHang> getById(String id) {
        return repository.findById(id);
    }

    public ChiTietDonHang save(ChiTietDonHang chiTietDonHang) {
        return repository.save(chiTietDonHang);
    }

    public ChiTietDonHang update(String id, ChiTietDonHang chiTietDonHang) {
        chiTietDonHang.set_id(id);
        return repository.save(chiTietDonHang);
    }

    public ChiTietDonHang updateTrangThai(String id, int trangThai) {
        ChiTietDonHang ct = repository.findById(id).orElseThrow();
        ct.setTrangThai(trangThai);
        return repository.save(ct);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    public List<ChiTietDonHang> getByIdDonHang(int idDonHang) {
    return repository.findByIdDonHang(idDonHang);
}

public void deleteByIdDonHang(int idDonHang) {
    repository.deleteByIdDonHang(idDonHang);
}
}
