package chandanv.local.chandanv.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import chandanv.local.chandanv.models.entity.MonAn;

public interface MonAnRepository extends MongoRepository<MonAn, String> {
    List<MonAn> findByIdLoai(int idLoai);
}
