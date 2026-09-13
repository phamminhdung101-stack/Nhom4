package chandanv.local.chandanv.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import chandanv.local.chandanv.models.entity.ChiTietDonHang;

@Repository
public interface ChiTietDonHangRepository
        extends MongoRepository<ChiTietDonHang, String> {

    List<ChiTietDonHang> findByIdDonHang(int idDonHang);

    void deleteByIdDonHang(int idDonHang);
}