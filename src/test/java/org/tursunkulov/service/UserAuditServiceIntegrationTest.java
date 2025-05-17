package org.tursunkulov.service;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.tursunkulov.dto.AuditEventDto;
import org.tursunkulov.entity.User;
import org.tursunkulov.exception.UserNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


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

  @DynamicPropertySource
  static void cassandraProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.cassandra.contact-points", cassandra::getHost);
    registry.add("spring.data.cassandra.port", () -> cassandra.getMappedPort(9042));
    registry.add("spring.data.cassandra.keyspace-name", () -> "audit_keyspace");
    registry.add("spring.data.cassandra.local-datacenter", () -> "dc1");
    registry.add("spring.data.cassandra.schema-action", () -> "CREATE_IF_NOT_EXISTS");
  }

  @SneakyThrows
  @Test
  void whenInsertThenCanReadBack() {
    UUID userId = UUID.randomUUID();
    AuditEventDto event = AuditEventDto.builder()
            .eventId(UUID.randomUUID())
            .userId(userId)
            .action("INSERT")
            .timestamp(Instant.now())
            .build();
    service.saveEvent(event);

    List<User> audits = service.readUserActions(userId);
    assertFalse(audits.isEmpty(), "Audit list must not be empty");

    User u = audits.get(0);
    assertEquals(userId, u.getUserId());
    assertEquals("INSERT", u.getEventType());
    assertTrue(u.getEventDetails().contains(event.getEventId().toString()));

    Integer ttl = service.getTtlForAudit(u.getUserId(), u.getEventTime());
    assertNotNull(ttl);
    assertTrue(ttl <= 31_536_000, "TTL should be ≤ 1 year in seconds");
  }

  @Test
  void whenReadNonexistent_thenThrow() {
    UUID userId = UUID.randomUUID();
    assertThrows(UserNotFoundException.class, () -> service.readUserActions(userId));
  }
}
