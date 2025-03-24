package org.tursunkulov.servicetest;

import static org.junit.Assert.assertThrows;

import java.util.UUID;

import org.testcontainers.junit.jupiter.Testcontainers;
import org.tursunkulov.exception.UserNotFoundException;
import org.tursunkulov.service.UserAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Testcontainers
class UserAuditServiceTest {
  @Autowired private UserAuditService userAuditService;

  private static final UUID uuid = UUID.randomUUID();

  @Test
  void shouldSuccessfullyCreateAudit() {
    userAuditService.insertUserAction(uuid);
  }

  @Test
  void shouldFailToCreateAuditWithNullUuid() {
    assertThrows(
        IllegalArgumentException.class,
        () -> userAuditService.insertUserAction(null));
  }

  @Test
  void shouldFailToGetAudit() {
    assertThrows(
        UserNotFoundException.class, () -> userAuditService.readUserAction(UUID.randomUUID()));
  }
}
