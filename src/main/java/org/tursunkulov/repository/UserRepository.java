package org.tursunkulov.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;
import org.tursunkulov.entity.User;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepository extends CassandraRepository<User, UUID> {

  @Query("SELECT * FROM user_audit WHERE user_id = ?0")
  List<User> findByUserId(UUID userId);
}
