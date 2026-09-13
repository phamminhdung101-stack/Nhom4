package chandanv.local.chandanv.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import chandanv.local.chandanv.models.entity.Admin;

@Repository
public interface AdminRepository extends MongoRepository<Admin, String> {

    Optional<Admin> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByIdAdmin(String idAdmin);
}