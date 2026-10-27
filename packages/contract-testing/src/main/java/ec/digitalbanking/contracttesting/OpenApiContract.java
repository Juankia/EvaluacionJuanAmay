package ec.digitalbanking.contracttesting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Minimal OpenAPI 3.0 schema checker for contract tests: required properties, types, enums,
 * patterns, arrays and {@code $ref} (including references to sibling files). It is deliberately
 * small and is not a full JSON Schema implementation; see docs/implementacion/04.
 */
public final class OpenApiContract {

  private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
  private final Path dir;
  private final Map<String, JsonNode> documents = new HashMap<>();

  private OpenApiContract(Path dir) {
    this.dir = dir;
  }

  /** Uses the {@code contracts.dir} system property set by the build. */
  public static OpenApiContract fromBuild() {
    String location = System.getProperty("contracts.dir");
    if (location == null) {
      throw new IllegalStateException("System property contracts.dir is not set");
    }
    return new OpenApiContract(Path.of(location));
  }

  public static OpenApiContract at(Path dir) {
    return new OpenApiContract(dir);
  }

  public JsonNode document(String file) {
    return documents.computeIfAbsent(
        file,
        f -> {
          try {
            return YAML.readTree(Files.readString(dir.resolve(f)));
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
  }

  /** Returns the list of violations of {@code value} against {@code file#/components/schemas/name}. */
  public List<String> validate(String file, String schemaName, JsonNode value) {
    List<String> errors = new ArrayList<>();
    JsonNode schema = document(file).path("components").path("schemas").path(schemaName);
    if (schema.isMissingNode()) {
      errors.add("schema not found: " + file + "#" + schemaName);
      return errors;
    }
    check(file, schema, value, "$", errors);
    return errors;
  }

  public void assertValid(String file, String schemaName, JsonNode value) {
    List<String> errors = validate(file, schemaName, value);
    if (!errors.isEmpty()) {
      throw new AssertionError("Contract violation " + file + "#" + schemaName + ": " + errors);
    }
  }

  /** True when the contract declares {@code status} for {@code method path}. */
  public boolean declaresResponse(String file, String path, String method, int status) {
    return !document(file)
        .path("paths")
        .path(path)
        .path(method.toLowerCase())
        .path("responses")
        .path(String.valueOf(status))
        .isMissingNode();
  }

  private void check(String file, JsonNode schema, JsonNode value, String at, List<String> errors) {
    if (schema.has("$ref")) {
      String ref = schema.get("$ref").asText();
      int hash = ref.indexOf('#');
      String targetFile = hash == 0 ? file : ref.substring(0, hash).replaceFirst("^\\./", "");
      JsonNode target = document(targetFile).at(ref.substring(hash + 1));
      check(targetFile, target, value, at, errors);
      return;
    }
    if (value == null || value.isNull()) {
      if (!schema.path("nullable").asBoolean(false)) {
        errors.add(at + " is null");
      }
      return;
    }
    String type = schema.path("type").asText("");
    switch (type) {
      case "object" -> {
        if (!value.isObject()) {
          errors.add(at + " is not an object");
          return;
        }
        for (JsonNode required : schema.path("required")) {
          if (!value.has(required.asText())) {
            errors.add(at + "." + required.asText() + " is required");
          }
        }
        Iterator<Map.Entry<String, JsonNode>> props = schema.path("properties").fields();
        while (props.hasNext()) {
          Map.Entry<String, JsonNode> p = props.next();
          if (value.has(p.getKey())) {
            check(file, p.getValue(), value.get(p.getKey()), at + "." + p.getKey(), errors);
          }
        }
      }
      case "array" -> {
        if (!value.isArray()) {
          errors.add(at + " is not an array");
          return;
        }
        for (int i = 0; i < value.size(); i++) {
          check(file, schema.path("items"), value.get(i), at + "[" + i + "]", errors);
        }
      }
      case "string" -> {
        if (!value.isTextual()) {
          errors.add(at + " is not a string");
          return;
        }
        checkEnum(schema, value, at, errors);
        if (schema.has("pattern")
            && !Pattern.compile(schema.get("pattern").asText()).matcher(value.asText()).find()) {
          errors.add(at + " does not match pattern");
        }
      }
      case "integer" -> {
        if (!value.isIntegralNumber()) {
          errors.add(at + " is not an integer");
        }
      }
      case "boolean" -> {
        if (!value.isBoolean()) {
          errors.add(at + " is not a boolean");
        } else {
          checkEnum(schema, value, at, errors);
        }
      }
      default -> {
        // untyped schema: accept
      }
    }
  }

  private static void checkEnum(JsonNode schema, JsonNode value, String at, List<String> errors) {
    if (!schema.has("enum")) {
      return;
    }
    for (JsonNode allowed : schema.get("enum")) {
      if (allowed.asText().equals(value.asText())) {
        return;
      }
    }
    errors.add(at + " value not in enum");
  }
}
