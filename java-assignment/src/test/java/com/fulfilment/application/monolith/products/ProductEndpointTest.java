package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductEndpointTest {

  private static final String PATH = "product";

  @Test
  @Order(1)
  public void testListAllProducts() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));
  }

  

  @Test
  @Order(2)
  public void testGetSingleProduct() {
    given()
        .when()
        .get(PATH + "/1")
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"));
  }

  @Test
  @Order(3)
  public void testGetNonExistentProductReturns404() {
    given()
        .when()
        .get(PATH + "/999")
        .then()
        .statusCode(404)
        .body(containsString("does not exist"));
  }

  @Test
  @Order(4)
  public void testCreateProduct() {
    String body =
        """
        {
          "name": "HEMNES",
          "description": "A shelf unit",
          "price": 99.99,
          "stock": 25
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
  public void testCreateProductWithIdFails() {
    String body =
        """
        {
          "id": 100,
          "name": "INVALID",
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(422)
        .body(containsString("Id was invalidly set"));
  }

  @Test
  @Order(6)
  public void testUpdateProduct() {
    String body =
        """
        {
          "name": "TONSTAD_UPDATED",
          "description": "Updated description",
          "price": 149.99,
          "stock": 20
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
  public void testUpdateProductWithoutNameFails() {
    String body =
        """
        {
          "description": "No name provided",
          "stock": 10
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(422)
        .body(containsString("Product Name was not set"));
  }

  @Test
  @Order(8)
  public void testUpdateNonExistentProductReturns404() {
    String body =
        """
        {
          "name": "GHOST",
          "stock": 10
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .put(PATH + "/999")
        .then()
        .statusCode(404)
        .body(containsString("does not exist"));
  }

  @Test
  @Order(9)
  public void testDeleteProduct() {
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
  @Order(10)
  public void testDeleteNonExistentProductReturns404() {
    given()
        .when()
        .delete(PATH + "/999")
        .then()
        .statusCode(404)
        .body(containsString("does not exist"));
  }

  @Test
  @Order(11)
  public void testCreateProductWithAllFields() {
    String body =
        """
        {
          "name": "FULL_PRODUCT",
          "description": "Complete product with all fields",
          "price": 199.99,
          "stock": 50
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("FULL_PRODUCT"))
        .body(containsString("199.99"));
  }

  @Test
  @Order(12)
  public void testCreateProductWithMinimalFields() {
    String body =
        """
        {
          "name": "MINIMAL_PRODUCT",
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("MINIMAL_PRODUCT"));
  }

  @Test
  @Order(13)
  public void testUpdateProductWithAllFields() {
    String body =
        """
        {
          "name": "BESTÅ_FULL_UPDATE",
          "description": "Fully updated product",
          "price": 299.99,
          "stock": 100
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .put(PATH + "/3")
        .then()
        .statusCode(200)
        .body(containsString("BESTÅ_FULL_UPDATE"))
        .body(containsString("299.99"));
  }

  @Test
  @Order(14)
  public void testGetProductVerifiesAllFields() {
    given()
        .when()
        .get(PATH + "/3")
        .then()
        .statusCode(200)
        .body(containsString("id"))
        .body(containsString("name"))
        .body(containsString("stock"));
  }
}
