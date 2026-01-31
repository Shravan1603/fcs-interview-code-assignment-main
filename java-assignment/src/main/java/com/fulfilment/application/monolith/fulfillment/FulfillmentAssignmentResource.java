package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * REST resource for managing fulfillment assignments between Products, Warehouses, and Stores.
 *
 * <p>Constraints enforced:
 * <ul>
 *   <li>Each Product can be fulfilled by max 2 different Warehouses per Store</li>
 *   <li>Each Store can be fulfilled by max 3 different Warehouses</li>
 *   <li>Each Warehouse can store max 5 types of Products</li>
 * </ul>
 */
@Path("fulfillment")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FulfillmentAssignmentResource {

  private static final Logger LOGGER =
      Logger.getLogger(FulfillmentAssignmentResource.class.getName());

  // Constraint constants
  private static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  private static final int MAX_WAREHOUSES_PER_STORE = 3;
  private static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  @Inject FulfillmentAssignmentRepository repository;

  @Inject ProductRepository productRepository;

  @Inject WarehouseStore warehouseStore;

  @GET
  public List<FulfillmentAssignment> getAll() {
    return repository.listAll();
  }

  @GET
  @Path("{id}")
  public FulfillmentAssignment getById(@PathParam("id") Long id) {
    FulfillmentAssignment assignment = repository.findById(id);
    if (assignment == null) {
      throw new WebApplicationException("Fulfillment assignment with id " + id + " not found.", 404);
    }
    return assignment;
  }

  @GET
  @Path("by-store/{storeId}")
  public List<FulfillmentAssignment> getByStore(@PathParam("storeId") Long storeId) {
    return repository.findByStoreId(storeId);
  }

  @GET
  @Path("by-warehouse/{warehouseCode}")
  public List<FulfillmentAssignment> getByWarehouse(
      @PathParam("warehouseCode") String warehouseCode) {
    return repository.findByWarehouse(warehouseCode);
  }

  @GET
  @Path("by-product/{productId}")
  public List<FulfillmentAssignment> getByProduct(@PathParam("productId") Long productId) {
    return repository.findByProductId(productId);
  }

  @POST
  @Transactional
  public Response create(FulfillmentAssignmentRequest request) {
    validateRequest(request);

    // Validate that product exists
    Product product = productRepository.findById(request.productId);
    if (product == null) {
      throw new WebApplicationException(
          "Product with id " + request.productId + " does not exist.", 400);
    }

    // Validate that store exists
    Store store = Store.findById(request.storeId);
    if (store == null) {
      throw new WebApplicationException(
          "Store with id " + request.storeId + " does not exist.", 400);
    }

    // Validate that warehouse exists and is active
    var warehouse = warehouseStore.findByBusinessUnitCode(request.warehouseBusinessUnitCode);
    if (warehouse == null) {
      throw new WebApplicationException(
          "Warehouse with business unit code '"
              + request.warehouseBusinessUnitCode
              + "' does not exist or is archived.",
          400);
    }

    // Check if assignment already exists
    if (repository.existsAssignment(
        request.productId, request.warehouseBusinessUnitCode, request.storeId)) {
      throw new WebApplicationException(
          "This fulfillment assignment already exists.", 400);
    }

    // Constraint 1: Each Product can be fulfilled by max 2 different Warehouses per Store
    long warehousesForProductAtStore =
        repository.countWarehousesForProductAtStore(request.productId, request.storeId);
    if (warehousesForProductAtStore >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new WebApplicationException(
          "Product "
              + request.productId
              + " is already fulfilled by "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses for store "
              + request.storeId
              + ". Maximum reached.",
          400);
    }

    // Constraint 2: Each Store can be fulfilled by max 3 different Warehouses
    // Only check if this warehouse is not already assigned to this store
    if (!repository.isWarehouseAssignedToStore(request.warehouseBusinessUnitCode, request.storeId)) {
      long warehousesForStore = repository.countDistinctWarehousesForStore(request.storeId);
      if (warehousesForStore >= MAX_WAREHOUSES_PER_STORE) {
        throw new WebApplicationException(
            "Store "
                + request.storeId
                + " is already fulfilled by "
                + MAX_WAREHOUSES_PER_STORE
                + " different warehouses. Maximum reached.",
            400);
      }
    }

    // Constraint 3: Each Warehouse can store max 5 types of Products
    // Only check if this product is not already stored in this warehouse
    if (!repository.isProductInWarehouse(request.productId, request.warehouseBusinessUnitCode)) {
      long productsInWarehouse =
          repository.countDistinctProductsInWarehouse(request.warehouseBusinessUnitCode);
      if (productsInWarehouse >= MAX_PRODUCTS_PER_WAREHOUSE) {
        throw new WebApplicationException(
            "Warehouse '"
                + request.warehouseBusinessUnitCode
                + "' already stores "
                + MAX_PRODUCTS_PER_WAREHOUSE
                + " different products. Maximum reached.",
            400);
      }
    }

    // All validations passed, create the assignment
    FulfillmentAssignment assignment =
        new FulfillmentAssignment(
            request.productId, request.warehouseBusinessUnitCode, request.storeId);
    assignment.createdAt = LocalDateTime.now();
    repository.persist(assignment);

    LOGGER.infof(
        "Created fulfillment assignment: Product %d -> Warehouse '%s' -> Store %d",
        request.productId, request.warehouseBusinessUnitCode, request.storeId);

    return Response.status(201).entity(assignment).build();
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(@PathParam("id") Long id) {
    FulfillmentAssignment assignment = repository.findById(id);
    if (assignment == null) {
      throw new WebApplicationException("Fulfillment assignment with id " + id + " not found.", 404);
    }
    repository.delete(assignment);
    LOGGER.infof("Deleted fulfillment assignment with id %d", id);
    return Response.noContent().build();
  }

  @DELETE
  @Transactional
  public Response deleteByQuery(
      @QueryParam("productId") Long productId,
      @QueryParam("warehouseBusinessUnitCode") String warehouseCode,
      @QueryParam("storeId") Long storeId) {

    if (productId == null || warehouseCode == null || storeId == null) {
      throw new WebApplicationException(
          "All query parameters (productId, warehouseBusinessUnitCode, storeId) are required.", 400);
    }

    long deleted =
        repository.delete(
            "productId = ?1 and warehouseBusinessUnitCode = ?2 and storeId = ?3",
            productId,
            warehouseCode,
            storeId);

    if (deleted == 0) {
      throw new WebApplicationException("Fulfillment assignment not found.", 404);
    }

    LOGGER.infof(
        "Deleted fulfillment assignment: Product %d -> Warehouse '%s' -> Store %d",
        productId, warehouseCode, storeId);

    return Response.noContent().build();
  }

  private void validateRequest(FulfillmentAssignmentRequest request) {
    if (request.productId == null) {
      throw new WebApplicationException("productId is required.", 400);
    }
    if (request.warehouseBusinessUnitCode == null
        || request.warehouseBusinessUnitCode.isBlank()) {
      throw new WebApplicationException("warehouseBusinessUnitCode is required.", 400);
    }
    if (request.storeId == null) {
      throw new WebApplicationException("storeId is required.", 400);
    }
  }
}
