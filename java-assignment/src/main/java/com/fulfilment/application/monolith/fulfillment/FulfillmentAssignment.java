package com.fulfilment.application.monolith.fulfillment;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Represents an assignment of a Warehouse as a fulfillment unit for a Product to a Store.
 *
 * <p>Constraints:
 * <ul>
 *   <li>Each Product can be fulfilled by max 2 different Warehouses per Store</li>
 *   <li>Each Store can be fulfilled by max 3 different Warehouses</li>
 *   <li>Each Warehouse can store max 5 types of Products</li>
 * </ul>
 */
@Entity
@Table(
    name = "fulfillment_assignment",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_fulfillment_product_warehouse_store",
          columnNames = {"productId", "warehouseBusinessUnitCode", "storeId"})
    })
@Cacheable
public class FulfillmentAssignment {

  @Id @GeneratedValue public Long id;

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = false)
  public String warehouseBusinessUnitCode;

  @Column(nullable = false)
  public Long storeId;

  public LocalDateTime createdAt;

  public FulfillmentAssignment() {}

  public FulfillmentAssignment(Long productId, String warehouseBusinessUnitCode, Long storeId) {
    this.productId = productId;
    this.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
    this.storeId = storeId;
    this.createdAt = LocalDateTime.now();
  }
}
