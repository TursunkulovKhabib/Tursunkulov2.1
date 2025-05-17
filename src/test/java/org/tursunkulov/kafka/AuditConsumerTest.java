package org.tursunkulov.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.tursunkulov.dto.AuditEventDto;
import org.tursunkulov.service.UserAuditService;

@SpringBootTest
@Testcontainers
class AuditConsumerTest {

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("topic.audit", () -> "test-topic");
    }

    @MockBean
    private UserAuditService auditService;

    private KafkaProducer<String, String> producer;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUpProducer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producer = new KafkaProducer<>(props);
    }

    @Test
    void shouldProcessValidMessage() throws Exception {
        AuditEventDto event = AuditEventDto.builder()
                .eventId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .action("TEST_ACTION")
                .timestamp(Instant.now())
                .build();
        String payload = objectMapper.writeValueAsString(event);

        producer.send(new ProducerRecord<>("test-topic", payload));

        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        verify(auditService, times(1)).saveEvent(event)
                );
    }

    @Test
    void shouldNotCallServiceOnInvalidJson() {
        producer.send(new ProducerRecord<>("test-topic", "this_is_not_json"));

        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        verify(auditService, never()).saveEvent(any(AuditEventDto.class))
                );
    }
}
