package org.tursunkulov.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tursunkulov.dto.AuditEventDto;
import org.tursunkulov.entity.User;
import org.tursunkulov.exception.UserNotFoundException;
import org.tursunkulov.repository.UserRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  private AuditEventDto eventDto;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    now = Instant.now();
    eventDto = AuditEventDto.builder()
            .eventId(UUID.randomUUID())
            .userId(userId)
            .action("INSERT")
            .timestamp(now)
            .build();
  }

  @Test
  void shouldSuccessfullyCreateAudit() {
    service.saveEvent(eventDto);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();

    assertEquals(userId, saved.getUserId());
    assertEquals(now, saved.getEventTime());
    assertEquals("INSERT", saved.getEventType());
    assertTrue(saved.getEventDetails().contains(eventDto.getEventId().toString()));
  }

  @Test
  void shouldReadUserActions() throws UserNotFoundException {
    User mockUser = new User(
            userId,
            now,
            "INSERT",
            "EventId=" + eventDto.getEventId()
    );
    when(userRepository.findByUserId(userId)).thenReturn(List.of(mockUser));

    List<User> result = service.readUserActions(userId);

    assertEquals(1, result.size());
    User read = result.get(0);
    assertEquals(userId, read.getUserId());
    assertEquals(now, read.getEventTime());
    assertEquals("INSERT", read.getEventType());
    assertTrue(read.getEventDetails().contains(eventDto.getEventId().toString()));
  }

  @Test
  void shouldThrowIfNotFound() {
    when(userRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

    assertThrows(UserNotFoundException.class, () -> service.readUserActions(userId));
  }
}
