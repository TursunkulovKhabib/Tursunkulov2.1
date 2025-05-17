package org.tursunkulov.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tursunkulov.entity.User;
import org.tursunkulov.exception.UserNotFoundException;
import org.tursunkulov.repository.UserRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuditServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserAuditService service;

  private UUID userId;
  private Instant now;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    now = Instant.now();
  }

  @Test
  void shouldSuccessfullyCreateAudit() {
    service.insertUserAction(userId);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();

    assertEquals(userId, saved.getUserId());
    assertNotNull(saved.getEventTime());
    assertEquals("INSERT", saved.getEventType());
    assertTrue(saved.getEventDetails().contains(userId.toString()));
  }

  @Test
  void shouldReadUserActions() {
    User mockUser = new User(
        userId,
        now,
        "INSERT",
        "Performed INSERT for " + userId
    );
    when(userRepository.findByUserId(userId)).thenReturn(List.of(mockUser));

    List<User> result = service.readUserActions(userId);

    assertEquals(1, result.size());
    User read = result.get(0);
    assertEquals(userId, read.getUserId());
    assertEquals(now, read.getEventTime());
    assertEquals("INSERT", read.getEventType());
    assertTrue(read.getEventDetails().contains(userId.toString()));
  }

  @Test
  void shouldThrowIfNotFound() {
    when(userRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

    assertThrows(UserNotFoundException.class, () -> service.readUserActions(userId));
  }
}
