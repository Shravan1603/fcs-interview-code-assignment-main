package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class FulfillmentAssignmentTest {

  @Test
  void shouldCreateFulfillmentAssignmentWithDefaultConstructor() {
    FulfillmentAssignment assignment = new FulfillmentAssignment();

    assertNull(assignment.id);
    assertNull(assignment.productId);
    assertNull(assignment.warehouseBusinessUnitCode);
    assertNull(assignment.storeId);
    assertNull(assignment.createdAt);
  }

  @Test
  void shouldCreateFulfillmentAssignmentWithParameterizedConstructor() {
    FulfillmentAssignment assignment = new FulfillmentAssignment(1L, "MWH.001", 2L);

    assertEquals(1L, assignment.productId);
    assertEquals("MWH.001", assignment.warehouseBusinessUnitCode);
    assertEquals(2L, assignment.storeId);
    assertNotNull(assignment.createdAt);
  }

  @Test
  void shouldSetAllFields() {
    FulfillmentAssignment assignment = new FulfillmentAssignment();
    assignment.id = 100L;
    assignment.productId = 5L;
    assignment.warehouseBusinessUnitCode = "MWH.TEST";
    assignment.storeId = 10L;

    assertEquals(100L, assignment.id);
    assertEquals(5L, assignment.productId);
    assertEquals("MWH.TEST", assignment.warehouseBusinessUnitCode);
    assertEquals(10L, assignment.storeId);
  }
}
