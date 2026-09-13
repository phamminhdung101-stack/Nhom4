package chandanv.local.chandanv.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import chandanv.local.chandanv.models.entity.BanAn;
import chandanv.local.chandanv.repositories.BanAnRepository;

@Service
@Transactional
public class BanAnService {

    private final BanAnRepository repo;

    public BanAnService(BanAnRepository repo) {
        this.repo = repo;
    }

    public List<BanAn> layDanhSachBan() {
        return repo.findAll();
    }

    public void luuMaQR(int idBan, String maQR) {

        BanAn ban = repo.findByIdBan(idBan)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy bàn với idBan = " + idBan));

        ban.setMaQR(maQR);
        repo.save(ban);
    }

    public void capNhatTrangThai(int idBan, int trangThai) {

        if (trangThai != 0 && trangThai != 1) {
            throw new IllegalArgumentException("Trạng thái bàn không hợp lệ: " + trangThai);
        }

        BanAn ban = repo.findByIdBan(idBan)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy bàn với idBan = " + idBan));

        ban.setTrangThai(trangThai);
        repo.save(ban);
    }

    public void resetTatCaBan() {

        List<BanAn> dsBan = repo.findAll();

        for (BanAn ban : dsBan) {
            ban.setTrangThai(0);
        }

        repo.saveAll(dsBan);
    }

    public void luuAnhQR(int idBan, String base64Image) {

        BanAn ban = repo.findByIdBan(idBan)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy bàn với idBan = " + idBan));

        if (base64Image != null && base64Image.contains(",")) {
            base64Image = base64Image.split(",")[1];
        }

        ban.setQrImageBase64(base64Image);
        repo.save(ban);
    }

    public BanAn timBanTheoIdBan(int idBan) {
        return repo.findByIdBan(idBan).orElse(null);
    }
}