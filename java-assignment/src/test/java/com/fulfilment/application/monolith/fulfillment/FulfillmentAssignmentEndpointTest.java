package com.fulfilment.application.monolith.fulfillment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration tests for the Fulfillment Assignment REST API.
 *
 * <p>Tests the constraints:
 * <ul>
 *   <li>Each Product can be fulfilled by max 2 different Warehouses per Store</li>
 *   <li>Each Store can be fulfilled by max 3 different Warehouses</li>
 *   <li>Each Warehouse can store max 5 types of Products</li>
 * </ul>
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FulfillmentAssignmentEndpointTest {

  private static final String PATH = "fulfillment";

  // Test data from import.sql:
  // Products: 1 (TONSTAD), 2 (KALLAX), 3 (BESTÅ)
  // Stores: 1 (TONSTAD), 2 (KALLAX), 3 (BESTÅ)
  // Warehouses: MWH.001 (ZWOLLE-001), MWH.012 (AMSTERDAM-001), MWH.023 (TILBURG-001)

  @Test
  @Order(1)
  public void testListAllAssignmentsEmpty() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(is("[]"));
  }

  @Test
  @Order(2)
  public void testCreateAssignment() {
    String body =
        """
        {
          "productId": 1,
          "warehouseBusinessUnitCode": "MWH.001",
          "storeId": 1
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("MWH.001"))
        .body(containsString("\"productId\":1"))
        .body(containsString("\"storeId\":1"));
  }

  @Test
  @Order(3)
  public void testCreateDuplicateAssignmentFails() {
    String body =
        """
        {
          "productId": 1,
          "warehouseBusinessUnitCode": "MWH.001",
          "storeId": 1
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body(containsString("already exists"));
  }

  @Test
  @Order(4)
  public void testCreateSecondWarehouseForSameProductAndStore() {
    // Add second warehouse for product 1 at store 1 - should succeed (max 2 allowed)
    String body =
        """
        {
          "productId": 1,
          "warehouseBusinessUnitCode": "MWH.012",
          "storeId": 1
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201);
  }

  @Test
  @Order(5)
  public void testMaxWarehousesPerProductPerStoreReached() {
    // Try to add third warehouse for product 1 at store 1 - should fail (max 2)
    String body =
        """
        {
          "productId": 1,
          "warehouseBusinessUnitCode": "MWH.023",
          "storeId": 1
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body(containsString("already fulfilled by 2 warehouses"));
  }

  @Test
  @Order(6)
  public void testCreateAssignmentWithDifferentProduct() {
    // Add product 2 to warehouse MWH.023 for store 1 - should succeed
    String body =
        """
        {
          "productId": 2,
          "warehouseBusinessUnitCode": "MWH.023",
          "storeId": 1
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201);
  }

  @Test
  @Order(7)
  public void testMaxWarehousesPerStoreReached() {
    // Store 1 now has 3 warehouses: MWH.001, MWH.012, MWH.023
    // Create a new warehouse to test the limit - use EINDHOVEN-001 to avoid
    // interfering with AMSTERDAM-001 capacity limits used in other tests
    String warehouseBody =
        """
        {
          "businessUnitCode": "MWH.TEST",
          "location": "EINDHOVEN-001",
          "capacity": 20,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(warehouseBody)
        .when()
        .post("warehouse")
        .then()
        .statusCode(200);

    // Now try to assign this new warehouse to store 1 - should fail (max 3)
    String body =
        """
        {
          "productId": 3,
          "warehouseBusinessUnitCode": "MWH.TEST",
          "storeId": 1
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body(containsString("already fulfilled by 3 different warehouses"));
  }

  @Test
  @Order(8)
  public void testGetAssignmentById() {
    given()
        .when()
        .get(PATH + "/1")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"));
  }

  @Test
  @Order(9)
  public void testGetNonExistentAssignmentReturns404() {
    given()
        .when()
        .get(PATH + "/999")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(10)
  public void testGetAssignmentsByStore() {
    given()
        .when()
        .get(PATH + "/by-store/1")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"))
        .body(containsString("MWH.012"));
  }

  @Test
  @Order(11)
  public void testGetAssignmentsByWarehouse() {
    given()
        .when()
        .get(PATH + "/by-warehouse/MWH.001")
        .then()
        .statusCode(200)
        .body(containsString("\"storeId\":1"));
  }

  @Test
  @Order(12)
  public void testGetAssignmentsByProduct() {
    given()
        .when()
        .get(PATH + "/by-product/1")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"))
        .body(containsString("MWH.012"));
  }

  @Test
  @Order(13)
  public void testCreateAssignmentWithNonExistentProductFails() {
    String body =
        """
        {
          "productId": 999,
          "warehouseBusinessUnitCode": "MWH.001",
          "storeId": 1
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body(containsString("Product with id 999 does not exist"));
  }

  @Test
  @Order(14)
  public void testCreateAssignmentWithNonExistentStoreFails() {
    String body =
        """
        {
          "productId": 1,
          "warehouseBusinessUnitCode": "MWH.001",
          "storeId": 999
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body(containsString("Store with id 999 does not exist"));
  }

  @Test
  @Order(15)
  public void testCreateAssignmentWithNonExistentWarehouseFails() {
    String body =
        """
        {
          "productId": 1,
          "warehouseBusinessUnitCode": "NONEXISTENT",
          "storeId": 1
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body(containsString("does not exist or is archived"));
  }

  @Test
  @Order(16)
  public void testCreateAssignmentWithMissingFieldsFails() {
    String body =
        """
        {
          "productId": 1
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(17)
  public void testMaxProductsPerWarehouse() {
    // Create additional products for testing
    for (int i = 4; i <= 8; i++) {
      String productBody =
          """
          {
            "name": "TESTPROD%d",
            "stock": 10
          }
          """.formatted(i);
      given()
          .contentType(ContentType.JSON)
          .body(productBody)
          .when()
          .post("product")
          .then()
          .statusCode(201);
    }

    // Store 3 has no assignments yet, use MWH.001 for this test
    // MWH.001 already has product 1 assigned (to store 1)
    // Add products 4, 5, 6, 7 to MWH.001 for store 3
    for (int productId = 4; productId <= 7; productId++) {
      String body =
          """
          {
            "productId": %d,
            "warehouseBusinessUnitCode": "MWH.001",
            "storeId": 3
          }
          """.formatted(productId);

      given()
          .contentType(ContentType.JSON)
          .body(body)
          .when()
          .post(PATH)
          .then()
          .statusCode(201);
    }

    // Now MWH.001 has 5 products (1, 4, 5, 6, 7)
    // Try to add product 8 - should fail (max 5 products per warehouse)
    String body =
        """
        {
          "productId": 8,
          "warehouseBusinessUnitCode": "MWH.001",
          "storeId": 3
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body(containsString("already stores 5 different products"));
  }

  @Test
  @Order(18)
  public void testDeleteAssignmentById() {
    // Delete assignment with id 1
    given()
        .when()
        .delete(PATH + "/1")
        .then()
        .statusCode(204);

    // Verify it's deleted
    given()
        .when()
        .get(PATH + "/1")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(19)
  public void testDeleteNonExistentAssignmentReturns404() {
    given()
        .when()
        .delete(PATH + "/999")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(20)
  public void testDeleteAssignmentByQuery() {
    // First create an assignment to delete
    String body =
        """
        {
          "productId": 3,
          "warehouseBusinessUnitCode": "MWH.012",
          "storeId": 3
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    // Now delete by query
    given()
        .queryParam("productId", 3)
        .queryParam("warehouseBusinessUnitCode", "MWH.012")
        .queryParam("storeId", 3)
        .when()
        .delete(PATH)
        .then()
        .statusCode(204);
  }

  @Test
  @Order(21)
  public void testDeleteByQueryWithMissingParamsFails() {
    given()
        .queryParam("productId", 1)
        .when()
        .delete(PATH)
        .then()
        .statusCode(400);
  }
}
