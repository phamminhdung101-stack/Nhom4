package chandanv.local.chandanv.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import chandanv.local.chandanv.models.entity.GioHang;

@Repository
public interface GioHangRepository extends MongoRepository<GioHang, String> {

    Optional<GioHang> findByIdBan(Integer idBan);

    boolean existsByIdBan(Integer idBan);

    void deleteByIdBan(Integer idBan);
}