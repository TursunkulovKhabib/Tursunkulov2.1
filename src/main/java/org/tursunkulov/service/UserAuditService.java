package org.tursunkulov.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tursunkulov.exception.UserNotFoundException;

import java.util.UUID;

@Service
public class UserAuditService {

  @Autowired private CqlSession session;

  public void insertUserAction(UUID userId) {
    PreparedStatement preparedStatement =
        session.prepare(
            "INSERT INTO my_keyspace.user_audit (user_id, event_time, event_type, event_details) "
                + "VALUES (?, ?, ?, ?)");

    BoundStatement boundStatement =
        preparedStatement.bind(
            java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
            java.time.Instant.now(),
            Action.DROPPED_DATABASE.toString(),
            "User DROPPED DATABASE from IP 192.168.1.1");
    session.execute(boundStatement);
  }

  public void readUserAction(UUID userId) throws UserNotFoundException {
    PreparedStatement preparedStatement =
        session.prepare("SELECT * FROM my_keyspace.user_audit WHERE user_id = ?");
    BoundStatement boundStatement = preparedStatement.bind(userId);
    ResultSet resultSet = session.execute(boundStatement);

    if (!resultSet.iterator().hasNext()) {
      throw new UserNotFoundException("User with UUID " + userId + " wasn't found");
    }
    resultSet.iterator().next();
  }
}
