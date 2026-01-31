package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(CreateWarehouseUseCase.class.getName());

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    // 1. Business Unit Code uniqueness check
    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing != null) {
      throw new WarehouseValidationException(
          "A warehouse with business unit code '"
              + warehouse.businessUnitCode
              + "' already exists.");
    }

    // 2. Location validation
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WarehouseValidationException(
          "Location '" + warehouse.location + "' is not a valid location.");
    }

    // 3. Warehouse creation feasibility - check max warehouses at location
    List<Warehouse> warehousesAtLocation =
        warehouseStore.findActiveByLocation(warehouse.location);
    if (warehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WarehouseValidationException(
          "Maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ") already reached at location '"
              + warehouse.location
              + "'.");
    }

    // 4. Capacity validation - must not exceed location max capacity
    int currentTotalCapacity =
        warehousesAtLocation.stream().mapToInt(w -> w.capacity).sum();
    if (currentTotalCapacity + warehouse.capacity > location.maxCapacity) {
      throw new WarehouseValidationException(
          "Warehouse capacity ("
              + warehouse.capacity
              + ") would exceed the maximum capacity ("
              + location.maxCapacity
              + ") for location '"
              + warehouse.location
              + "'. Current used capacity: "
              + currentTotalCapacity
              + ".");
    }

    // 5. Stock must not exceed warehouse capacity
    if (warehouse.stock != null && warehouse.stock > warehouse.capacity) {
      throw new WarehouseValidationException(
          "Warehouse stock ("
              + warehouse.stock
              + ") cannot exceed warehouse capacity ("
              + warehouse.capacity
              + ").");
    }

    // Set creation timestamp
    warehouse.createdAt = LocalDateTime.now();

    LOGGER.infof(
        "Creating warehouse with BU code '%s' at location '%s'",
        warehouse.businessUnitCode, warehouse.location);

    warehouseStore.create(warehouse);
  }
}
