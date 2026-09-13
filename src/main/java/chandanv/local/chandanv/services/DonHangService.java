package chandanv.local.chandanv.services;

import java.time.Instant;
import java.util.Random;

import org.springframework.stereotype.Service;

import chandanv.local.chandanv.models.entity.DonHang;
import chandanv.local.chandanv.repositories.DonHangRepository;

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
}