package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class StoreEventTest {

  @Test
  void shouldCreateStoreEventWithCreatedType() {
    Store store = new Store("TEST_STORE");
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.CREATED);

    assertEquals(store, event.getStore());
    assertEquals(StoreEvent.Type.CREATED, event.getType());
  }

  @Test
  void shouldCreateStoreEventWithUpdatedType() {
    Store store = new Store("TEST_STORE");
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.UPDATED);

    assertEquals(store, event.getStore());
    assertEquals(StoreEvent.Type.UPDATED, event.getType());
  }

  @Test
  void shouldHaveAllEventTypes() {
    StoreEvent.Type[] types = StoreEvent.Type.values();

    assertEquals(2, types.length);
    assertNotNull(StoreEvent.Type.valueOf("CREATED"));
    assertNotNull(StoreEvent.Type.valueOf("UPDATED"));
  }
}
