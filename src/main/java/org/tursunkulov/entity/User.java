package org.tursunkulov.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;
import org.tursunkulov.service.Action;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@Table(value = "user_audit")
public class User {

  @Getter
  @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
  private UUID user_id;

  @Getter
  @PrimaryKeyColumn(name = "event_time", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
  private Instant event_time;

  @Getter
  @Column(value = "event_type")
  private Action event_type;

  @Getter
  @Column(value = "event_details")
  private String event_details;
}
