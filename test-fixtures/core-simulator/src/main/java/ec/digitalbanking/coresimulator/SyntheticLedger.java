package ec.digitalbanking.coresimulator;

import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SYNTHETIC data set. Every customer, account and movement here is fictitious and exists only to
 * exercise the platform locally. It is NOT the banking Core and holds no real data.
 */
@ApplicationScoped
public class SyntheticLedger {

  private static final LocalDate LATEST_BOOKING = LocalDate.of(2026, 10, 20);

  private final Map<String, List<LegacyAccount>> accountsByCustomer =
      Map.of(
          "CUST-0001",
          List.of(
              new LegacyAccount(
                  "ACC-1001", "AHO", "USD", new BigDecimal("1520.35"), new BigDecimal("1600.00"), "A"),
              new LegacyAccount(
                  "ACC-1002", "AHO", "USD", new BigDecimal("80.00"), new BigDecimal("80.00"), "A")),
          "CUST-0002",
          List.of(
              new LegacyAccount(
                  "ACC-2001", "AHO", "USD", new BigDecimal("300.10"), new BigDecimal("300.10"), "B")));

  private final Map<String, List<LegacyMovement>> movementsByAccount =
      Map.of(
          "ACC-1001", generate("ACC-1001", 45),
          "ACC-1002", generate("ACC-1002", 3),
          "ACC-2001", generate("ACC-2001", 5));

  public Optional<List<LegacyAccount>> accountsOf(String customerId) {
    return Optional.ofNullable(accountsByCustomer.get(customerId));
  }

  public Optional<List<LegacyMovement>> movementsOf(String accountId) {
    return Optional.ofNullable(movementsByAccount.get(accountId));
  }

  private static List<LegacyMovement> generate(String accountId, int count) {
    List<LegacyMovement> movements = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      LocalDate booking = LATEST_BOOKING.minusDays(i);
      boolean debit = i % 3 != 0;
      BigDecimal amount = BigDecimal.valueOf(5 + (i * 7L) % 120).setScale(2);
      movements.add(
          new LegacyMovement(
              accountId + "-" + String.format("%05d", count - i),
              booking,
              booking,
              amount,
              "USD",
              debit ? "D" : "C",
              debit ? "PAGO SERVICIO SINTETICO" : "DEPOSITO SINTETICO"));
    }
    return List.copyOf(movements);
  }

  /** Legacy-style account record: field names mimic a Core contract, on purpose. */
  public record LegacyAccount(
      String nroCuenta,
      String codProducto,
      String moneda,
      BigDecimal saldoDisp,
      BigDecimal saldoCont,
      String estado) {}

  /** Legacy-style movement record. {@code signo} is D (debit) or C (credit). */
  public record LegacyMovement(
      String secuencia,
      LocalDate fechaContable,
      LocalDate fechaValor,
      BigDecimal monto,
      String moneda,
      String signo,
      String glosa) {}
}
