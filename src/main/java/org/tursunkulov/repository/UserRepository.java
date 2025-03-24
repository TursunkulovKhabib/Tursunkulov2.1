package org.tursunkulov.repository;

import org.tursunkulov.entity.User;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends CassandraRepository<User, String> {
  @Query("SELECT * FROM user_audit WHERE user_id = ?0")
  Record findByUserId(UUID userId);
}
