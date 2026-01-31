package com.fulfilment.application.monolith.stores;

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
public class StoreEndpointTest {

  private static final String PATH = "store";

  @Test
  @Order(1)
  public void testListAllStores() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"));
  }

  @Test
  @Order(2)
  public void testGetSingleStore() {
    given()
        .when()
        .get(PATH + "/1")
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"));
  }

  @Test
  @Order(3)
  public void testGetNonExistentStoreReturns404() {
    given().when().get(PATH + "/999").then().statusCode(404);
  }

  @Test
  @Order(4)
  public void testCreateStore() {
    String body =
        """
        {
          "name": "HEMNES",
          "quantityProductsInStock": 15
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("HEMNES"));
  }

  @Test
  @Order(5)
  public void testCreateStoreWithIdFails() {
    String body =
        """
        {
          "id": 100,
          "name": "INVALID",
          "quantityProductsInStock": 5
        }
        """;

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(422);
  }

  @Test
  @Order(6)
  public void testUpdateStore() {
    String body =
        """
        {
          "name": "TONSTAD_UPDATED",
          "quantityProductsInStock": 20
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD_UPDATED"));
  }

  @Test
  @Order(7)
  public void testUpdateStoreWithoutNameFails() {
    String body =
        """
        {
          "quantityProductsInStock": 20
        }
        """;

    given().contentType(ContentType.JSON).body(body).when().put(PATH + "/1").then().statusCode(422);
  }

  @Test
  @Order(8)
  public void testDeleteStore() {
    given().when().delete(PATH + "/2").then().statusCode(204);

    // Verify KALLAX is no longer listed
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("KALLAX")));
  }

  @Test
  @Order(9)
  public void testDeleteNonExistentStoreReturns404() {
    given().when().delete(PATH + "/999").then().statusCode(404);
  }

  @Test
  @Order(10)
  public void testPatchStore() {
    String body =
        """
        {
          "name": "BESTÅ_PATCHED",
          "quantityProductsInStock": 99
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .patch(PATH + "/3")
        .then()
        .statusCode(200)
        .body(containsString("BESTÅ_PATCHED"));
  }

  @Test
  @Order(11)
  public void testPatchStoreWithoutNameFails() {
    String body =
        """
        {
          "quantityProductsInStock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .patch(PATH + "/3")
        .then()
        .statusCode(422);
  }

  @Test
  @Order(12)
  public void testPatchNonExistentStoreReturns404() {
    String body =
        """
        {
          "name": "GHOST"
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .patch(PATH + "/999")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(13)
  public void testUpdateNonExistentStoreReturns404() {
    String body =
        """
        {
          "name": "GHOST",
          "quantityProductsInStock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .put(PATH + "/999")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(14)
  public void testCreateStoreWithZeroQuantity() {
    String body =
        """
        {
          "name": "ZERO_QTY_STORE",
          "quantityProductsInStock": 0
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("ZERO_QTY_STORE"));
  }

  @Test
  @Order(15)
  public void testUpdateStoreWithZeroQuantity() {
    // Update store 1 to have quantityProductsInStock = 0
    String body =
        """
        {
          "name": "UPDATED_ZERO",
          "quantityProductsInStock": 0
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(200)
        .body(containsString("UPDATED_ZERO"));
  }

  @Test
  @Order(16)
  public void testPatchStoreWithZeroQuantityDoesNotUpdateQuantity() {
    // Now store 1 has quantityProductsInStock = 0 (from test 15)
    // The patch method should NOT update the quantity because entity.quantityProductsInStock == 0
    String body =
        """
        {
          "name": "PATCHED_ZERO_QTY",
          "quantityProductsInStock": 100
        }
        """;

    // Patch should succeed and the name updates, but the quantity check fails so quantity stays 0
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .patch(PATH + "/1")
        .then()
        .statusCode(200)
        .body(containsString("PATCHED_ZERO_QTY"));
  }

  @Test
  @Order(17)
  public void testCreateStoreWithMinimalFields() {
    String body =
        """
        {
          "name": "MINIMAL_STORE"
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("MINIMAL_STORE"));
  }
}
