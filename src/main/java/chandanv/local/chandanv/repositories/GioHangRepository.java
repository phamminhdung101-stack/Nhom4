package chandanv.local.chandanv.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import chandanv.local.chandanv.models.entity.GioHang;

public interface GioHangRepository extends MongoRepository<GioHang, String> {

    Optional<GioHang> findByIdBan(Integer idBan);

    void deleteByIdBan(Integer idBan);
}
