package ec.digitalbanking.linking.adapters.out.persistence;

import ec.digitalbanking.linking.application.port.OnboardingRepository;
import ec.digitalbanking.linking.domain.OnboardingRequest;
import ec.digitalbanking.linking.domain.OnboardingState;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.postgresql.util.PSQLException;

/**
 * JDBC adapter. Connections are obtained inside the caller's JTA transaction (the application
 * service opens it), so the state, the idempotency record and the outbox row commit together.
 */
@ApplicationScoped
public class JdbcOnboardingRepository implements OnboardingRepository {

  static final String UNIQUE_VIOLATION = "23505";
  static final String IDEMPOTENCY_PK = "idempotency_record_pkey";
  static final String ACTIVE_IDENTIFICATION_UQ = "onboarding_request_active_identification_uq";

  private final DataSource dataSource;

  public JdbcOnboardingRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Optional<OnboardingRequest> find(UUID requestId) {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT identification_digest, state, version, updated_at"
                    + " FROM onboarding_request WHERE request_id = ?")) {
      ps.setObject(1, requestId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return Optional.empty();
        }
        return Optional.of(
            new OnboardingRequest(
                requestId,
                rs.getString(1),
                OnboardingState.valueOf(rs.getString(2)),
                rs.getLong(3),
                rs.getTimestamp(4).toInstant()));
      }
    } catch (SQLException e) {
      throw new IllegalStateException("repository read failed", e);
    }
  }

  @Override
  public void insert(OnboardingRequest request) {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO onboarding_request"
                    + " (request_id, identification_digest, state, version, created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?)")) {
      Timestamp at = Timestamp.from(request.updatedAt());
      ps.setObject(1, request.requestId());
      ps.setString(2, request.identificationDigest());
      ps.setString(3, request.state().name());
      ps.setLong(4, request.version());
      ps.setTimestamp(5, at);
      ps.setTimestamp(6, at);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw translate(e);
    }
  }

  @Override
  public boolean update(OnboardingRequest updated, long expectedVersion) {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "UPDATE onboarding_request SET state = ?, version = ?, updated_at = ?"
                    + " WHERE request_id = ? AND version = ?")) {
      ps.setString(1, updated.state().name());
      ps.setLong(2, updated.version());
      ps.setTimestamp(3, Timestamp.from(updated.updatedAt()));
      ps.setObject(4, updated.requestId());
      ps.setLong(5, expectedVersion);
      return ps.executeUpdate() == 1;
    } catch (SQLException e) {
      throw translate(e);
    }
  }

  @Override
  public Optional<IdempotencyRecord> findIdempotency(String scope, String key) {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT fingerprint, request_id FROM idempotency_record"
                    + " WHERE scope = ? AND idem_key = ?")) {
      ps.setString(1, scope);
      ps.setString(2, key);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next()
            ? Optional.of(
                new IdempotencyRecord(scope, key, rs.getString(1), rs.getObject(2, UUID.class)))
            : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("repository read failed", e);
    }
  }

  @Override
  public void insertIdempotency(IdempotencyRecord record) {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO idempotency_record"
                    + " (scope, idem_key, fingerprint, request_id, created_at)"
                    + " VALUES (?, ?, ?, ?, ?)")) {
      ps.setString(1, record.scope());
      ps.setString(2, record.key());
      ps.setString(3, record.fingerprint());
      ps.setObject(4, record.requestId());
      ps.setTimestamp(5, Timestamp.from(Instant.now()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw translate(e);
    }
  }

  @Override
  public void appendOutbox(String eventId, String eventType, String body) {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO outbox_event (event_id, event_type, body, created_at)"
                    + " VALUES (?, ?, ?, ?)")) {
      ps.setString(1, eventId);
      ps.setString(2, eventType);
      ps.setString(3, body);
      ps.setTimestamp(4, Timestamp.from(Instant.now()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw translate(e);
    }
  }

  /** Maps the two uniqueness rules the service relies on; anything else is a technical failure. */
  static RuntimeException translate(SQLException e) {
    if (UNIQUE_VIOLATION.equals(e.getSQLState())
        && e instanceof PSQLException psql
        && psql.getServerErrorMessage() != null) {
      String constraint = psql.getServerErrorMessage().getConstraint();
      if (IDEMPOTENCY_PK.equals(constraint)) {
        return new UniquenessViolation(UniquenessViolation.Rule.IDEMPOTENCY_KEY);
      }
      if (ACTIVE_IDENTIFICATION_UQ.equals(constraint)) {
        return new UniquenessViolation(UniquenessViolation.Rule.ACTIVE_IDENTIFICATION);
      }
    }
    return new IllegalStateException("repository write failed", e);
  }
}
