package chandanv.local.chandanv.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import chandanv.local.chandanv.models.entity.Admin;

public interface AdminRepository extends MongoRepository<Admin, String> {

    Optional<Admin> findByUsername(String username);

    Optional<Admin> findByToken(String token);
}
