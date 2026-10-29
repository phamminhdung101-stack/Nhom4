package chandanv.local.chandanv.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import chandanv.local.chandanv.models.entity.MonAn;

public interface MonAnRepository extends MongoRepository<MonAn, String> {

    List<MonAn> findAllByOrderByIdLoaiAscIdMonAsc();

    List<MonAn> findByIdLoaiOrderByIdMonAsc(int idLoai);

    Optional<MonAn> findFirstByIdMon(int idMon);
}
