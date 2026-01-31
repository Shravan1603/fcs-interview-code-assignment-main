package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ReplaceWarehouseUseCase.class.getName());

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    // 1. Find existing active warehouse by BU code
    Warehouse existing =
        warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      throw new WarehouseValidationException(
          "No active warehouse found with business unit code '"
              + newWarehouse.businessUnitCode
              + "' to replace.");
    }

    // 2. Capacity accommodation - new capacity must hold the old warehouse's stock
    if (newWarehouse.capacity < existing.stock) {
      throw new WarehouseValidationException(
          "New warehouse capacity ("
              + newWarehouse.capacity
              + ") cannot accommodate the stock ("
              + existing.stock
              + ") from the warehouse being replaced.");
    }

    // 3. Stock matching - new warehouse stock must equal old warehouse stock
    if (!newWarehouse.stock.equals(existing.stock)) {
      throw new WarehouseValidationException(
          "New warehouse stock ("
              + newWarehouse.stock
              + ") must match the stock ("
              + existing.stock
              + ") of the warehouse being replaced.");
    }

    // 4. Validate the new warehouse's location
    Location newLocation = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (newLocation == null) {
      throw new WarehouseValidationException(
          "Location '" + newWarehouse.location + "' is not a valid location.");
    }

    // 5. Archive the existing warehouse
    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);

    LOGGER.infof("Archived warehouse with BU code '%s' for replacement", existing.businessUnitCode);

    // 6. Check warehouse count feasibility at new location (after archiving)
    List<Warehouse> warehousesAtNewLocation =
        warehouseStore.findActiveByLocation(newWarehouse.location);
    if (warehousesAtNewLocation.size() >= newLocation.maxNumberOfWarehouses) {
      throw new WarehouseValidationException(
          "Maximum number of warehouses ("
              + newLocation.maxNumberOfWarehouses
              + ") already reached at location '"
              + newWarehouse.location
              + "'.");
    }

    // 7. Check capacity at new location (after archiving)
    int currentTotalCapacity =
        warehousesAtNewLocation.stream().mapToInt(w -> w.capacity).sum();
    if (currentTotalCapacity + newWarehouse.capacity > newLocation.maxCapacity) {
      throw new WarehouseValidationException(
          "Warehouse capacity ("
              + newWarehouse.capacity
              + ") would exceed the maximum capacity ("
              + newLocation.maxCapacity
              + ") for location '"
              + newWarehouse.location
              + "'.");
    }

    // 8. Create the new warehouse with the same BU code
    newWarehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(newWarehouse);

    LOGGER.infof(
        "Created replacement warehouse with BU code '%s' at location '%s'",
        newWarehouse.businessUnitCode, newWarehouse.location);
  }
}
