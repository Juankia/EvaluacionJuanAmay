package ec.digitalbanking.audit.adapters.out.persistence;

import ec.digitalbanking.audit.application.AuditStore;
import ec.digitalbanking.audit.domain.AuditRecord;
import ec.digitalbanking.audit.domain.ChainHasher;
import ec.digitalbanking.integration.events.AuditCategory;
import ec.digitalbanking.integration.events.EventEnvelope;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;

@ApplicationScoped
public class JdbcAuditStore implements AuditStore {

  private static final String COLUMNS =
      "event_id, event_type, schema_version, source, subject_type, subject_id, sequence,"
          + " audit_category, occurred_at, recorded_at, correlation_id, payload_json,"
          + " sequence_gap, previous_hash, chain_hash";

  private final DataSource dataSource;

  public JdbcAuditStore(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public boolean exists(String eventId) {
    return query(
        "SELECT 1 FROM audit_record WHERE event_id = ?", ps -> ps.setString(1, eventId), rs -> true)
        .stream()
        .findFirst()
        .orElse(false);
  }

  @Override
  public String lockChainHead(String source) {
    update(
        "INSERT INTO audit_chain_head (source, last_hash, record_count) VALUES (?, ?, 0)"
            + " ON CONFLICT (source) DO NOTHING",
        ps -> {
          ps.setString(1, source);
          ps.setString(2, ChainHasher.GENESIS);
        });
    return query(
            "SELECT last_hash FROM audit_chain_head WHERE source = ? FOR UPDATE",
            ps -> ps.setString(1, source),
            rs -> rs.getString(1))
        .getFirst();
  }

  @Override
  public Optional<Long> lastSequence(String source, String subjectId) {
    return query(
            "SELECT max(sequence) FROM audit_record WHERE source = ? AND subject_id = ?",
            ps -> {
              ps.setString(1, source);
              ps.setString(2, subjectId);
            },
            rs -> {
              long value = rs.getLong(1);
              return rs.wasNull() ? null : value;
            })
        .stream()
        .filter(v -> v != null)
        .findFirst();
  }

  @Override
  public void append(AuditRecord r) {
    EventEnvelope e = r.event();
    update(
        "INSERT INTO audit_record (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        ps -> {
          ps.setString(1, e.eventId());
          ps.setString(2, e.eventType());
          ps.setInt(3, e.schemaVersion());
          ps.setString(4, e.source());
          ps.setString(5, e.subjectType());
          ps.setString(6, e.subjectId());
          ps.setLong(7, e.sequence());
          ps.setString(8, e.auditCategory().name());
          ps.setTimestamp(9, Timestamp.from(e.occurredAt()));
          ps.setTimestamp(10, Timestamp.from(r.recordedAt()));
          ps.setString(11, e.correlationId());
          ps.setString(12, r.payloadJson());
          ps.setBoolean(13, r.sequenceGap());
          ps.setString(14, r.previousHash());
          ps.setString(15, r.chainHash());
        });
    update(
        "UPDATE audit_chain_head SET last_hash = ?, record_count = record_count + 1"
            + " WHERE source = ?",
        ps -> {
          ps.setString(1, r.chainHash());
          ps.setString(2, e.source());
        });
  }

  @Override
  public List<AuditRecord> findBySubject(
      String subjectId, Optional<AuditCategory> category, int limit) {
    String sql =
        "SELECT "
            + COLUMNS
            + " FROM audit_record WHERE subject_id = ?"
            + (category.isPresent() ? " AND audit_category = ?" : "")
            + " ORDER BY occurred_at DESC, position DESC LIMIT ?";
    return query(
        sql,
        ps -> {
          int i = 1;
          ps.setString(i++, subjectId);
          if (category.isPresent()) {
            ps.setString(i++, category.get().name());
          }
          ps.setInt(i, limit);
        },
        JdbcAuditStore::map);
  }

  @Override
  public List<AuditRecord> chain(String source) {
    return query(
        "SELECT " + COLUMNS + " FROM audit_record WHERE source = ? ORDER BY position",
        ps -> ps.setString(1, source),
        JdbcAuditStore::map);
  }

  @Override
  public String chainHead(String source) {
    return query(
            "SELECT last_hash FROM audit_chain_head WHERE source = ?",
            ps -> ps.setString(1, source),
            rs -> rs.getString(1))
        .stream()
        .findFirst()
        .orElse(ChainHasher.GENESIS);
  }

  private static AuditRecord map(ResultSet rs) throws SQLException {
    EventEnvelope event =
        new EventEnvelope(
            rs.getString("event_id"),
            rs.getString("event_type"),
            rs.getInt("schema_version"),
            rs.getString("source"),
            rs.getString("subject_type"),
            rs.getString("subject_id"),
            rs.getLong("sequence"),
            rs.getTimestamp("occurred_at").toInstant(),
            rs.getString("correlation_id"),
            AuditCategory.valueOf(rs.getString("audit_category")),
            Map.of());
    return new AuditRecord(
        event,
        rs.getString("payload_json"),
        rs.getTimestamp("recorded_at").toInstant(),
        rs.getBoolean("sequence_gap"),
        rs.getString("previous_hash"),
        rs.getString("chain_hash"));
  }

  @FunctionalInterface
  private interface Binder {
    void bind(PreparedStatement ps) throws SQLException;
  }

  @FunctionalInterface
  private interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
  }

  private <T> List<T> query(String sql, Binder binder, RowMapper<T> mapper) {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement(sql)) {
      binder.bind(ps);
      try (ResultSet rs = ps.executeQuery()) {
        List<T> out = new ArrayList<>();
        while (rs.next()) {
          out.add(mapper.map(rs));
        }
        return out;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("audit store query failed", e);
    }
  }

  private void update(String sql, Binder binder) {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement(sql)) {
      binder.bind(ps);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("audit store update failed", e);
    }
  }
}
