package org.tursunkulov.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tursunkulov.dto.AuditEventDto;
import org.tursunkulov.entity.User;
import org.tursunkulov.exception.UserNotFoundException;
import org.tursunkulov.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuditService {
  private final CqlSession session;
  private final UserRepository userRepository;

  private PreparedStatement insertStmt;

    @PostConstruct
  public void init() {
    // optional: для TTL-проверок, если нужно
    insertStmt = session.prepare("""
        INSERT INTO my_keyspace.user_audit
           (user_id, event_time, event_type, event_details)
         VALUES (?, ?, ?, ?)
      """);
        PreparedStatement selectStmt = session.prepare("""
                  SELECT * FROM my_keyspace.user_audit
                   WHERE user_id = ?
                """);
  }


  @Transactional
  public void saveEvent(AuditEventDto event) {
    // вставляем как через CQL, так и через Spring Data Repository
    session.execute(insertStmt.bind(
            event.getUserId(),
            event.getTimestamp(),
            event.getAction(),
            "EventId=" + event.getEventId()
    ));
    User entity = new User(
            event.getUserId(),
            event.getTimestamp(),
            event.getAction(),
            "EventId=" + event.getEventId()
    );
    userRepository.save(entity);
  }

  @Transactional
  public List<User> readUserActions(UUID userId) throws UserNotFoundException {
    if (userId == null) throw new IllegalArgumentException("userId must not be null");
    List<User> audits = userRepository.findByUserId(userId);
    if (audits.isEmpty()) throw new UserNotFoundException("User " + userId + " not found");
    return audits;
  }

  public Integer getTtlForAudit(UUID userId, Instant eventTime) {
    return session.execute(
            session.prepare(
                    "SELECT TTL(event_details) as ttl FROM my_keyspace.user_audit WHERE user_id = ? AND event_time = ?"
            ).bind(userId, eventTime)
    ).one().getInt("ttl");
  }
}
