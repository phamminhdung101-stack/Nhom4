package chandanv.local.chandanv.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import chandanv.local.chandanv.models.entity.LichSuBanHang;

public interface LichSuBanHangRepository
        extends MongoRepository<LichSuBanHang, String> {
}
