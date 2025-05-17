package org.tursunkulov.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.tursunkulov.dto.AuditEventDto;
import org.tursunkulov.service.UserAuditService;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditConsumer {

    private final UserAuditService auditService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${topic.audit}", containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String payload) {
        try {
            AuditEventDto event = objectMapper.readValue(payload, AuditEventDto.class);
            auditService.saveEvent(event);
            log.info("Consumed audit event: {}", event);
        } catch (Exception ex) {
            log.error("Failed to process audit payload [{}]", payload, ex);
        }
    }
}
