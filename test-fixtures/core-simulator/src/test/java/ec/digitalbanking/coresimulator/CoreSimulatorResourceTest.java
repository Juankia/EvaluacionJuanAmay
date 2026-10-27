package ec.digitalbanking.coresimulator;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CoreSimulatorResourceTest {

  @Test
  void returnsSyntheticAccounts() {
    given()
        .get("/core-sim/v1/customers/CUST-0001/accounts")
        .then()
        .statusCode(200)
        .body("$", hasSize(2))
        .body("[0].nroCuenta", equalTo("ACC-1001"));
  }

  @Test
  void paginatesMovements() {
    given()
        .queryParam("offset", 40)
        .queryParam("limit", 10)
        .get("/core-sim/v1/accounts/ACC-1001/movements")
        .then()
        .statusCode(200)
        .body("registros", hasSize(5))
        .body("total", equalTo(45));
  }

  @Test
  void injectsUnavailability() {
    given()
        .header("X-Sim-Fault", "unavailable")
        .get("/core-sim/v1/customers/CUST-0001/accounts")
        .then()
        .statusCode(503);
  }

  @Test
  void unknownCustomerIsNotFound() {
    given().get("/core-sim/v1/customers/CUST-9999/accounts").then().statusCode(404);
  }
}
