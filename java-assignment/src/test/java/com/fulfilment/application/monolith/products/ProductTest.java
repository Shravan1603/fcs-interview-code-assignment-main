package com.fulfilment.application.monolith.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class ProductTest {

  @Test
  void shouldCreateProductWithDefaultConstructor() {
    Product product = new Product();

    assertNull(product.id);
    assertNull(product.name);
    assertNull(product.description);
    assertNull(product.price);
    assertEquals(0, product.stock);
  }

  @Test
  void shouldCreateProductWithNameConstructor() {
    Product product = new Product("TEST_PRODUCT");

    assertEquals("TEST_PRODUCT", product.name);
    assertNull(product.id);
    assertNull(product.description);
    assertNull(product.price);
    assertEquals(0, product.stock);
  }

  @Test
  void shouldSetAllFields() {
    Product product = new Product();
    product.id = 1L;
    product.name = "FULL_PRODUCT";
    product.description = "A test product";
    product.price = new BigDecimal("99.99");
    product.stock = 100;

    assertEquals(1L, product.id);
    assertEquals("FULL_PRODUCT", product.name);
    assertEquals("A test product", product.description);
    assertEquals(new BigDecimal("99.99"), product.price);
    assertEquals(100, product.stock);
  }
}
