package chandanv.local.chandanv.services;

import org.springframework.stereotype.Service;

import chandanv.local.chandanv.models.entity.LichSuBanHang;
import chandanv.local.chandanv.repositories.LichSuBanHangRepository;

@Service
public class LichSuBanHangService {

    private final LichSuBanHangRepository repo;

    public LichSuBanHangService(LichSuBanHangRepository repo) {
        this.repo = repo;
    }

    public LichSuBanHang save(LichSuBanHang ls) {
        return repo.save(ls);
    }
}
