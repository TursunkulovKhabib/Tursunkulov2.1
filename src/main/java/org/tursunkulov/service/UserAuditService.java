package org.tursunkulov.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.tursunkulov.entity.User;
import org.tursunkulov.exception.UserNotFoundException;
import org.tursunkulov.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAuditService {
  private final CqlSession session;
  private final UserRepository userRepository;

  @PostConstruct
  public void init() {
    PreparedStatement insertStmt = session.prepare("""
                    INSERT INTO my_keyspace.user_audit
                      (user_id, event_time, event_type, event_details)
                    VALUES (?, ?, ?, ?)
                """);
    PreparedStatement selectStmt = session.prepare("""
                    SELECT * FROM my_keyspace.user_audit
                     WHERE user_id = ?
                """);
  }

  public void insertUserAction(UUID userId) {
    User user = new User(
        userId,
        Instant.now(), "INSERT", "Performed INSERT for " + userId);
    userRepository.save(user);
  }

  public Integer getTtlForAudit(UUID userId, Instant eventTime) {
    PreparedStatement ps = session.prepare(
        "SELECT TTL(event_details) as ttl FROM my_keyspace.user_audit WHERE user_id = ? AND event_time = ?"
    );
    Row row = session.execute(ps.bind(userId, eventTime)).one();
    return row != null ? row.getInt("ttl") : null;
  }


  @SneakyThrows
  public List<User> readUserActions(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId must not be null");
    }
    List<User> audits = userRepository.findByUserId(userId);
    if (audits.isEmpty()) {
      throw new UserNotFoundException("User " + userId + " not found");
    }
    return audits;
  }

}
