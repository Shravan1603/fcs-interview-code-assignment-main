package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class StoreTest {

  @Test
  void shouldCreateStoreWithDefaultConstructor() {
    Store store = new Store();

    assertNull(store.name);
    assertEquals(0, store.quantityProductsInStock);
  }

  @Test
  void shouldCreateStoreWithNameConstructor() {
    Store store = new Store("TEST_STORE");

    assertEquals("TEST_STORE", store.name);
    assertEquals(0, store.quantityProductsInStock);
  }

  @Test
  void shouldSetAllFields() {
    Store store = new Store();
    store.name = "FULL_STORE";
    store.quantityProductsInStock = 50;

    assertEquals("FULL_STORE", store.name);
    assertEquals(50, store.quantityProductsInStock);
  }
}
