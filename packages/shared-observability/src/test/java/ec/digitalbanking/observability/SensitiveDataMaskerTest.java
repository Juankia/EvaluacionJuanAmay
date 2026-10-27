package ec.digitalbanking.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTest {

  @Test
  void masksAllButLastCharacters() {
    assertEquals("********7890", SensitiveDataMasker.maskIdentifier("ACC-12347890"));
  }

  @Test
  void toleratesEmptyValues() {
    assertEquals("", SensitiveDataMasker.maskIdentifier(""));
  }
}
