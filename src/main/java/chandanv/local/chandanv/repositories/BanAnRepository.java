package chandanv.local.chandanv.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import chandanv.local.chandanv.models.entity.BanAn;

public interface BanAnRepository extends MongoRepository<BanAn, String> {

    Optional<BanAn> findByIdBan(int idBan);

    List<BanAn> findAllByOrderByIdBanAsc();

    long countByTrangThai(int trangThai);
}
