package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WarehouseEndpointTest {

  private static final String PATH = "warehouse";

  @Test
  @Order(1)
  public void testListAllWarehouses() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  @Order(2)
  public void testGetWarehouseByBusinessUnitCode() {
    given()
        .when()
        .get(PATH + "/MWH.001")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("ZWOLLE-001"));
  }

  @Test
  @Order(3)
  public void testGetNonExistentWarehouseReturns404() {
    given().when().get(PATH + "/MWH.999").then().statusCode(404);
  }

  @Test
  @Order(4)
  public void testCreateWarehouse() {
    String body =
        """
        {
          "businessUnitCode": "MWH.100",
          "location": "AMSTERDAM-001",
          "capacity": 20,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.100"), containsString("AMSTERDAM-001"));
  }

  @Test
  @Order(5)
  public void testCreateWarehouseWithDuplicateBuCodeFails() {
    String body =
        """
        {
          "businessUnitCode": "MWH.001",
          "location": "AMSTERDAM-001",
          "capacity": 20,
          "stock": 5
        }
        """;

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(400);
  }

  @Test
  @Order(6)
  public void testCreateWarehouseWithInvalidLocationFails() {
    String body =
        """
        {
          "businessUnitCode": "MWH.200",
          "location": "NONEXISTENT-001",
          "capacity": 20,
          "stock": 5
        }
        """;

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(400);
  }

  @Test
  @Order(7)
  public void testCreateWarehouseExceedingCapacityFails() {
    // TILBURG-001 has max 1 warehouse and already has MWH.023
    String body =
        """
        {
          "businessUnitCode": "MWH.300",
          "location": "TILBURG-001",
          "capacity": 20,
          "stock": 5
        }
        """;

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(400);
  }

  @Test
  @Order(8)
  public void testArchiveWarehouse() {
    given().when().delete(PATH + "/MWH.023").then().statusCode(204);

    // Verify it's no longer listed
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("MWH.023")));
  }

  @Test
  @Order(9)
  public void testArchiveNonExistentWarehouseReturns404() {
    given().when().delete(PATH + "/MWH.999").then().statusCode(404);
  }

  @Test
  @Order(10)
  public void testReplaceWarehouse() {
    // Replace MWH.012 (AMSTERDAM-001, capacity=50, stock=5) with a new warehouse
    String body =
        """
        {
          "location": "AMSTERDAM-001",
          "capacity": 80,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH + "/MWH.012/replacement")
        .then()
        .statusCode(200)
        .body(containsString("MWH.012"), containsString("AMSTERDAM-001"));
  }

  @Test
  @Order(11)
  public void testReplaceWarehouseWithStockMismatchFails() {
    // MWH.001 has stock=10, trying to replace with stock=20
    String body =
        """
        {
          "location": "AMSTERDAM-001",
          "capacity": 50,
          "stock": 20
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH + "/MWH.001/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  @Order(12)
  public void testCreateWarehouseExceedingLocationMaxCapacityFails() {
    // AMSTERDAM-001 has maxCapacity=100, MWH.012 has capacity=50, MWH.100 added capacity=20
    // Total used: 70, so adding capacity > 30 should fail
    String body =
        """
        {
          "businessUnitCode": "MWH.400",
          "location": "AMSTERDAM-001",
          "capacity": 50,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body(containsString("exceed the maximum capacity"));
  }

  @Test
  @Order(13)
  public void testCreateWarehouseWithStockExceedingCapacityFails() {
    // Stock (100) > Capacity (50) should fail
    String body =
        """
        {
          "businessUnitCode": "MWH.500",
          "location": "EINDHOVEN-001",
          "capacity": 50,
          "stock": 100
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body(containsString("cannot exceed warehouse capacity"));
  }

  @Test
  @Order(14)
  public void testReplaceNonExistentWarehouseReturns400() {
    // WarehouseValidationException is thrown and caught, returning 400
    String body =
        """
        {
          "location": "AMSTERDAM-001",
          "capacity": 50,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH + "/MWH.999/replacement")
        .then()
        .statusCode(400)
        .body(containsString("No active warehouse found"));
  }

  @Test
  @Order(15)
  public void testCreateWarehouseWithNullStock() {
    String body =
        """
        {
          "businessUnitCode": "MWH.600",
          "location": "EINDHOVEN-001",
          "capacity": 30
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.600"));
  }

  @Test
  @Order(16)
  public void testReplaceWarehouseWithInsufficientCapacityFails() {
    // MWH.001 has stock=10, trying to replace with capacity=5 (less than stock)
    String body =
        """
        {
          "location": "AMSTERDAM-001",
          "capacity": 5,
          "stock": 10
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH + "/MWH.001/replacement")
        .then()
        .statusCode(400)
        .body(containsString("cannot accommodate the stock"));
  }

  @Test
  @Order(17)
  public void testReplaceWarehouseWithInvalidLocationFails() {
    // MWH.001 has stock=10, trying to replace with invalid location
    String body =
        """
        {
          "location": "INVALID-LOCATION",
          "capacity": 50,
          "stock": 10
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH + "/MWH.001/replacement")
        .then()
        .statusCode(400)
        .body(containsString("not a valid location"));
  }
}
