package ec.digitalbanking.linking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ec.digitalbanking.linking.application.Digests;
import org.junit.jupiter.api.Test;

class DigestsTest {

  private final Digests digests = new Digests("k".repeat(32));

  @Test
  void rejectsShortKeys() {
    assertThrows(IllegalStateException.class, () -> new Digests("short"));
  }

  @Test
  void isDeterministicKeyedAndOpaque() {
    String d = digests.digest("identification", "CEDULA", "0102030405");
    assertEquals(64, d.length());
    assertEquals(d, digests.digest("identification", "CEDULA", "0102030405"));
    assertNotEquals(d, new Digests("x".repeat(32)).digest("identification", "CEDULA", "0102030405"));
    assertFalse(d.contains("0102030405"));
  }

  @Test
  void partsAreSeparated() {
    assertNotEquals(digests.digest("ab", "c"), digests.digest("a", "bc"));
  }
}
