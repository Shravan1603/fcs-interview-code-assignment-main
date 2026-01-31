package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CreateWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private CreateWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = Mockito.mock(WarehouseStore.class);
    locationResolver = Mockito.mock(LocationResolver.class);
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  void shouldCreateWarehouseSuccessfully() {
    Warehouse warehouse = createWarehouse("MWH.100", "AMSTERDAM-001", 50, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
    assertNotNull(warehouse.createdAt);
  }

  @Test
  void shouldFailWhenBusinessUnitCodeAlreadyExists() {
    Warehouse warehouse = createWarehouse("MWH.001", "AMSTERDAM-001", 50, 10);
    Warehouse existing = createWarehouse("MWH.001", "ZWOLLE-001", 100, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    WarehouseValidationException ex =
        assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));

    assertEquals(
        "A warehouse with business unit code 'MWH.001' already exists.", ex.getMessage());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldFailWhenLocationIsInvalid() {
    Warehouse warehouse = createWarehouse("MWH.100", "INVALID-001", 50, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("INVALID-001")).thenReturn(null);

    WarehouseValidationException ex =
        assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));

    assertEquals("Location 'INVALID-001' is not a valid location.", ex.getMessage());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldFailWhenMaxWarehousesReachedAtLocation() {
    Warehouse warehouse = createWarehouse("MWH.100", "TILBURG-001", 30, 5);

    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("TILBURG-001"))
        .thenReturn(new Location("TILBURG-001", 1, 40));

    // One warehouse already exists at TILBURG-001 (max is 1)
    Warehouse existingAtLocation = createWarehouse("MWH.023", "TILBURG-001", 30, 27);
    when(warehouseStore.findActiveByLocation("TILBURG-001"))
        .thenReturn(List.of(existingAtLocation));

    WarehouseValidationException ex =
        assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));

    assert ex.getMessage().contains("Maximum number of warehouses");
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldFailWhenCapacityExceedsLocationMax() {
    Warehouse warehouse = createWarehouse("MWH.100", "ZWOLLE-001", 50, 5);

    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 2, 40));
    // Existing warehouse uses 30 of 40 max capacity
    Warehouse existingAtLocation = createWarehouse("MWH.001", "ZWOLLE-001", 30, 10);
    when(warehouseStore.findActiveByLocation("ZWOLLE-001"))
        .thenReturn(List.of(existingAtLocation));

    WarehouseValidationException ex =
        assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));

    assert ex.getMessage().contains("would exceed the maximum capacity");
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldFailWhenStockExceedsCapacity() {
    Warehouse warehouse = createWarehouse("MWH.100", "AMSTERDAM-001", 20, 30);

    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    WarehouseValidationException ex =
        assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));

    assert ex.getMessage().contains("cannot exceed warehouse capacity");
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldCreateWarehouseWithZeroStock() {
    Warehouse warehouse = createWarehouse("MWH.100", "AMSTERDAM-001", 50, 0);

    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
  }

  @Test
  void shouldCreateWarehouseAtLocationWithRemainingCapacity() {
    Warehouse warehouse = createWarehouse("MWH.100", "AMSTERDAM-001", 30, 5);

    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));

    Warehouse existing1 = createWarehouse("MWH.001", "AMSTERDAM-001", 40, 10);
    Warehouse existing2 = createWarehouse("MWH.002", "AMSTERDAM-001", 20, 5);
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(List.of(existing1, existing2));

    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
  }

  @Test
  void shouldCreateWarehouseWithNullStock() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.NULL";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50;
    warehouse.stock = null;

    when(warehouseStore.findByBusinessUnitCode("MWH.NULL")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
    assertNotNull(warehouse.createdAt);
  }

  @Test
  void shouldCreateWarehouseWithExactCapacityMatch() {
    // Create warehouse that uses exact remaining capacity
    Warehouse warehouse = createWarehouse("MWH.EXACT", "AMSTERDAM-001", 40, 5);

    when(warehouseStore.findByBusinessUnitCode("MWH.EXACT")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));

    // Existing warehouse uses 60 of 100 capacity, new one uses 40 (exact fit)
    Warehouse existing = createWarehouse("MWH.001", "AMSTERDAM-001", 60, 10);
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(List.of(existing));

    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
  }

  @Test
  void shouldCreateWarehouseWithStockEqualToCapacity() {
    Warehouse warehouse = createWarehouse("MWH.FULL", "AMSTERDAM-001", 50, 50);

    when(warehouseStore.findByBusinessUnitCode("MWH.FULL")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
  }

  private Warehouse createWarehouse(String buCode, String location, int capacity, int stock) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = buCode;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }
}
