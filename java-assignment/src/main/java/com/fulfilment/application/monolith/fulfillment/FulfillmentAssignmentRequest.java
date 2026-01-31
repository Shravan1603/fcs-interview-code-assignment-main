package com.fulfilment.application.monolith.fulfillment;

/**
 * Request DTO for creating a fulfillment assignment.
 */
public class FulfillmentAssignmentRequest {

  public Long productId;

  public String warehouseBusinessUnitCode;

  public Long storeId;

  public FulfillmentAssignmentRequest() {}

  public FulfillmentAssignmentRequest(
      Long productId, String warehouseBusinessUnitCode, Long storeId) {
    this.productId = productId;
    this.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
    this.storeId = storeId;
  }
}
