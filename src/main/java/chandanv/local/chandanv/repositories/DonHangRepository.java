package chandanv.local.chandanv.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import chandanv.local.chandanv.models.entity.DonHang;

@Repository
public interface DonHangRepository extends MongoRepository<DonHang, String> {

    List<DonHang> findByIdGioHangOrderByIdDonHangDesc(int idGioHang);

}