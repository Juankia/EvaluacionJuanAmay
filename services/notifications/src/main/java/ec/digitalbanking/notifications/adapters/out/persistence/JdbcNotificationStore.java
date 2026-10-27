package ec.digitalbanking.notifications.adapters.out.persistence;

import ec.digitalbanking.notifications.application.NotificationStore;
import ec.digitalbanking.notifications.domain.Channel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

@ApplicationScoped
public class JdbcNotificationStore implements NotificationStore {

  private final DataSource dataSource;

  public JdbcNotificationStore(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public boolean alreadyProcessed(String eventId) {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT 1 FROM processed_event WHERE event_id = ?")) {
      ps.setString(1, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("store read failed", e);
    }
  }

  @Override
  public Optional<Long> lastSequence(String source, String subjectId) {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT last_sequence FROM subject_state WHERE source = ? AND subject_id = ?")) {
      ps.setString(1, source);
      ps.setString(2, subjectId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("store read failed", e);
    }
  }

  @Override
  @Transactional
  public void complete(
      String eventId,
      String outcome,
      String source,
      String subjectId,
      long sequence,
      List<DeliveryRecord> deliveries) {
    Timestamp now = Timestamp.from(Instant.now());
    try (Connection c = dataSource.getConnection()) {
      try (PreparedStatement ps =
          c.prepareStatement(
              "INSERT INTO processed_event (event_id, outcome, processed_at) VALUES (?, ?, ?)")) {
        ps.setString(1, eventId);
        ps.setString(2, outcome);
        ps.setTimestamp(3, now);
        ps.executeUpdate();
      }
      try (PreparedStatement ps =
          c.prepareStatement(
              "INSERT INTO subject_state (source, subject_id, last_sequence) VALUES (?, ?, ?)"
                  + " ON CONFLICT (source, subject_id) DO UPDATE"
                  + " SET last_sequence = GREATEST(subject_state.last_sequence, EXCLUDED.last_sequence)")) {
        ps.setString(1, source);
        ps.setString(2, subjectId);
        ps.setLong(3, sequence);
        ps.executeUpdate();
      }
      try (PreparedStatement ps =
          c.prepareStatement(
              "INSERT INTO delivery (event_id, channel, status, attempts, recorded_at)"
                  + " VALUES (?, ?, ?, ?, ?)")) {
        for (DeliveryRecord d : deliveries) {
          ps.setString(1, d.eventId());
          ps.setString(2, d.channel().name());
          ps.setString(3, d.status());
          ps.setInt(4, d.attempts());
          ps.setTimestamp(5, now);
          ps.addBatch();
        }
        ps.executeBatch();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("store write failed", e);
    }
  }

  @Override
  public List<DeliveryRecord> deliveries(String eventId) {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT channel, status, attempts FROM delivery WHERE event_id = ? ORDER BY id")) {
      ps.setString(1, eventId);
      List<DeliveryRecord> out = new ArrayList<>();
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(
              new DeliveryRecord(
                  eventId, Channel.valueOf(rs.getString(1)), rs.getString(2), rs.getInt(3)));
        }
      }
      return out;
    } catch (SQLException e) {
      throw new IllegalStateException("store read failed", e);
    }
  }
}
