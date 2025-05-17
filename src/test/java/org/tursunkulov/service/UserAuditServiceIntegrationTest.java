package org.tursunkulov.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.tursunkulov.entity.User;
import org.tursunkulov.exception.UserNotFoundException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class UserAuditServiceIntegrationTest {

  @Container
  static CassandraContainer<?> cassandra =
      new CassandraContainer<>(DockerImageName.parse("cassandra:4.1"))
          .withExposedPorts(9042);

  @Autowired
  private UserAuditService service;

  @Test
  void whenInsertThenCanReadBack() {
    UUID id = UUID.randomUUID();
    service.insertUserAction(id);

    List<User> audits = service.readUserActions(id);
    assertFalse(audits.isEmpty(), "Audit list must not be empty");

    User u = audits.get(0);
    assertEquals(id, u.getUserId());
    assertEquals("INSERT", u.getEventType());
    assertTrue(u.getEventDetails().contains(id.toString()));

    Integer ttl = service.getTtlForAudit(u.getUserId(), u.getEventTime());
    assertNotNull(ttl);
    assertTrue(ttl <= 31536000);
  }

  @Test
  void whenReadNonexistent_thenThrow() {
    UUID id = UUID.randomUUID();
    assertThrows(UserNotFoundException.class, () -> service.readUserActions(id));
  }
}
