package ec.digitalbanking.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class CorrelationIdTest {

  @Test
  void keepsWellFormedIdentifier() {
    assertEquals("abc-12345678", CorrelationId.sanitizeOrGenerate("abc-12345678"));
  }

  @Test
  void replacesMaliciousIdentifier() {
    String injected = "abc\n2026-01-01 ERROR forged log line";
    String result = CorrelationId.sanitizeOrGenerate(injected);
    assertNotEquals(injected, result);
    assertEquals(36, result.length());
  }

  @Test
  void generatesWhenMissing() {
    assertEquals(36, CorrelationId.sanitizeOrGenerate(null).length());
  }
}
