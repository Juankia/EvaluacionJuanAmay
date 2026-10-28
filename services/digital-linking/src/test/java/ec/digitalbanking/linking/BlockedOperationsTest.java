package ec.digitalbanking.linking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.digitalbanking.linking.adapters.out.blocked.DisabledCustomerCreationAdapter;
import ec.digitalbanking.linking.adapters.out.blocked.DisabledDigitalAccessActivationAdapter;
import ec.digitalbanking.linking.application.port.OperationBlockedException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * ARG03-M02: the blocked ports exist only with disabled adapters, and no other compiled class
 * (use case, resource, domain) references them, so E3 and E5 are unreachable.
 */
class BlockedOperationsTest {

  private static final Path CLASSES = Path.of("target", "classes");
  private static final String PORTS = "ec/digitalbanking/linking/application/port/";

  @Test
  void disabledAdaptersFailClosed() {
    assertThrows(
        OperationBlockedException.class,
        () -> new DisabledCustomerCreationAdapter().createCustomer("req-1"));
    assertThrows(
        OperationBlockedException.class,
        () -> new DisabledDigitalAccessActivationAdapter().activate("req-1"));
  }

  @Test
  void onlyDisabledAdaptersReferenceCustomerCreationPort() throws IOException {
    assertEquals(
        Set.of(
            PORTS + "CustomerCreationPort.class",
            "ec/digitalbanking/linking/adapters/out/blocked/DisabledCustomerCreationAdapter.class"),
        classesReferencing(PORTS + "CustomerCreationPort"));
  }

  @Test
  void onlyDisabledAdaptersReferenceActivationPort() throws IOException {
    assertEquals(
        Set.of(
            PORTS + "DigitalAccessActivationPort.class",
            "ec/digitalbanking/linking/adapters/out/blocked/DisabledDigitalAccessActivationAdapter.class"),
        classesReferencing(PORTS + "DigitalAccessActivationPort"));
  }

  private static Set<String> classesReferencing(String internalName) throws IOException {
    assertTrue(Files.isDirectory(CLASSES), "compiled classes not found at " + CLASSES);
    List<Path> classFiles;
    try (Stream<Path> files = Files.walk(CLASSES)) {
      classFiles = files.filter(p -> p.toString().endsWith(".class")).toList();
    }
    Set<String> referencing = new java.util.TreeSet<>();
    for (Path file : classFiles) {
      // Class names live in the constant pool as modified UTF-8; ASCII names match byte for byte.
      String content = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
      if (content.contains(internalName)) {
        referencing.add(CLASSES.relativize(file).toString().replace('\\', '/'));
      }
    }
    return referencing;
  }
}
