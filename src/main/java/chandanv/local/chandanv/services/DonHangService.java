package chandanv.local.chandanv.services;

import java.time.Instant;
import java.util.Random;

import org.springframework.stereotype.Service;

import chandanv.local.chandanv.models.entity.DonHang;
import chandanv.local.chandanv.repositories.DonHangRepository;
import java.util.List;

@Service
public class DonHangService {

    private final DonHangRepository repo;

    public DonHangService(DonHangRepository repo) {
        this.repo = repo;
    }

    public DonHang create(DonHang donHang) {

        if (donHang.getIdDonHang() == null) {
            donHang.setIdDonHang(new Random().nextInt(999999));
        }

        donHang.setTrangThai(0);
        donHang.setThoiGian(Instant.now());

        return repo.save(donHang);
    }

    public DonHang getLatestByIdBan(Integer idBan) {
    return repo
        .findByIdGioHangOrderByIdDonHangDesc(idBan)
        .stream()
        .findFirst()
        .orElse(null);
    }

    public List<DonHang> getAll() {
        return repo.findAll();
    }

    public DonHang updateTrangThai(String id, Integer trangThai) {
        DonHang dh = repo.findById(id).orElseThrow();
        dh.setTrangThai(trangThai);
        return repo.save(dh);
    }
}