package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class FulfillmentAssignmentRequestTest {

  @Test
  void shouldCreateRequestWithDefaultConstructor() {
    FulfillmentAssignmentRequest request = new FulfillmentAssignmentRequest();

    assertNull(request.productId);
    assertNull(request.warehouseBusinessUnitCode);
    assertNull(request.storeId);
  }

  @Test
  void shouldCreateRequestWithParameterizedConstructor() {
    FulfillmentAssignmentRequest request = new FulfillmentAssignmentRequest(1L, "MWH.001", 2L);

    assertEquals(1L, request.productId);
    assertEquals("MWH.001", request.warehouseBusinessUnitCode);
    assertEquals(2L, request.storeId);
  }

  @Test
  void shouldSetAllFields() {
    FulfillmentAssignmentRequest request = new FulfillmentAssignmentRequest();
    request.productId = 5L;
    request.warehouseBusinessUnitCode = "MWH.TEST";
    request.storeId = 10L;

    assertEquals(5L, request.productId);
    assertEquals("MWH.TEST", request.warehouseBusinessUnitCode);
    assertEquals(10L, request.storeId);
  }
}
