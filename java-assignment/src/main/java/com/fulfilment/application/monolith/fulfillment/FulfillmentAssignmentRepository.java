package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FulfillmentAssignmentRepository implements PanacheRepository<FulfillmentAssignment> {

  /**
   * Counts how many warehouses fulfill a specific product for a specific store.
   *
   * @param productId the product ID
   * @param storeId the store ID
   * @return number of distinct warehouses fulfilling this product for this store
   */
  public long countWarehousesForProductAtStore(Long productId, Long storeId) {
    return count("productId = ?1 and storeId = ?2", productId, storeId);
  }

  /**
   * Counts how many distinct warehouses fulfill a specific store.
   *
   * @param storeId the store ID
   * @return number of distinct warehouses fulfilling this store
   */
  public long countDistinctWarehousesForStore(Long storeId) {
    return find("storeId = ?1", storeId)
        .stream()
        .map(a -> a.warehouseBusinessUnitCode)
        .distinct()
        .count();
  }

  /**
   * Counts how many distinct products a warehouse stores.
   *
   * @param warehouseBusinessUnitCode the warehouse business unit code
   * @return number of distinct products stored in this warehouse
   */
  public long countDistinctProductsInWarehouse(String warehouseBusinessUnitCode) {
    return find("warehouseBusinessUnitCode = ?1", warehouseBusinessUnitCode)
        .stream()
        .map(a -> a.productId)
        .distinct()
        .count();
  }

  /**
   * Finds all assignments for a specific store.
   *
   * @param storeId the store ID
   * @return list of fulfillment assignments for the store
   */
  public List<FulfillmentAssignment> findByStoreId(Long storeId) {
    return find("storeId = ?1", storeId).list();
  }

  /**
   * Finds all assignments for a specific warehouse.
   *
   * @param warehouseBusinessUnitCode the warehouse business unit code
   * @return list of fulfillment assignments for the warehouse
   */
  public List<FulfillmentAssignment> findByWarehouse(String warehouseBusinessUnitCode) {
    return find("warehouseBusinessUnitCode = ?1", warehouseBusinessUnitCode).list();
  }

  /**
   * Finds all assignments for a specific product.
   *
   * @param productId the product ID
   * @return list of fulfillment assignments for the product
   */
  public List<FulfillmentAssignment> findByProductId(Long productId) {
    return find("productId = ?1", productId).list();
  }

  /**
   * Checks if a specific assignment already exists.
   *
   * @param productId the product ID
   * @param warehouseBusinessUnitCode the warehouse business unit code
   * @param storeId the store ID
   * @return true if the assignment exists, false otherwise
   */
  public boolean existsAssignment(
      Long productId, String warehouseBusinessUnitCode, Long storeId) {
    return count(
            "productId = ?1 and warehouseBusinessUnitCode = ?2 and storeId = ?3",
            productId,
            warehouseBusinessUnitCode,
            storeId)
        > 0;
  }

  /**
   * Checks if a warehouse is already assigned to a store (for any product).
   *
   * @param warehouseBusinessUnitCode the warehouse business unit code
   * @param storeId the store ID
   * @return true if the warehouse is already assigned to this store
   */
  public boolean isWarehouseAssignedToStore(String warehouseBusinessUnitCode, Long storeId) {
    return count(
            "warehouseBusinessUnitCode = ?1 and storeId = ?2",
            warehouseBusinessUnitCode,
            storeId)
        > 0;
  }

  /**
   * Checks if a product is already stored in a warehouse (for any store).
   *
   * @param productId the product ID
   * @param warehouseBusinessUnitCode the warehouse business unit code
   * @return true if the product is already in this warehouse
   */
  public boolean isProductInWarehouse(Long productId, String warehouseBusinessUnitCode) {
    return count(
            "productId = ?1 and warehouseBusinessUnitCode = ?2",
            productId,
            warehouseBusinessUnitCode)
        > 0;
  }
}
