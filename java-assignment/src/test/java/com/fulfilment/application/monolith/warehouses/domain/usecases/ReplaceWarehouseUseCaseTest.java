package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ReplaceWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = Mockito.mock(WarehouseStore.class);
    locationResolver = Mockito.mock(LocationResolver.class);
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  void shouldReplaceWarehouseSuccessfully() {
    Warehouse existing = createWarehouse("MWH.001", "ZWOLLE-001", 100, 10);
    existing.createdAt = LocalDateTime.of(2024, 7, 1, 0, 0);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    Warehouse newWarehouse = createWarehouse("MWH.001", "AMSTERDAM-001", 50, 10);

    useCase.replace(newWarehouse);

    // Should archive old warehouse and create new one
    verify(warehouseStore).update(existing);
    verify(warehouseStore).create(newWarehouse);
    assertNotNull(existing.archivedAt);
    assertNotNull(newWarehouse.createdAt);
  }

  @Test
  void shouldFailWhenNoExistingWarehouseFound() {
    when(warehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);

    Warehouse newWarehouse = createWarehouse("MWH.999", "AMSTERDAM-001", 50, 10);

    WarehouseValidationException ex =
        assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));

    assert ex.getMessage().contains("No active warehouse found");
    verify(warehouseStore, never()).update(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldFailWhenNewCapacityCannotAccommodateOldStock() {
    Warehouse existing = createWarehouse("MWH.001", "ZWOLLE-001", 100, 50);
    existing.createdAt = LocalDateTime.of(2024, 7, 1, 0, 0);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    // New capacity (30) is less than old stock (50)
    Warehouse newWarehouse = createWarehouse("MWH.001", "AMSTERDAM-001", 30, 50);

    WarehouseValidationException ex =
        assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));

    assert ex.getMessage().contains("cannot accommodate the stock");
    verify(warehouseStore, never()).update(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldFailWhenStockDoesNotMatch() {
    Warehouse existing = createWarehouse("MWH.001", "ZWOLLE-001", 100, 10);
    existing.createdAt = LocalDateTime.of(2024, 7, 1, 0, 0);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    // New stock (20) does not match old stock (10)
    Warehouse newWarehouse = createWarehouse("MWH.001", "AMSTERDAM-001", 50, 20);

    WarehouseValidationException ex =
        assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));

    assert ex.getMessage().contains("must match the stock");
    verify(warehouseStore, never()).update(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldFailWhenNewLocationIsInvalid() {
    Warehouse existing = createWarehouse("MWH.001", "ZWOLLE-001", 100, 10);
    existing.createdAt = LocalDateTime.of(2024, 7, 1, 0, 0);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("INVALID-LOC")).thenReturn(null);

    Warehouse newWarehouse = createWarehouse("MWH.001", "INVALID-LOC", 50, 10);

    WarehouseValidationException ex =
        assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));

    assert ex.getMessage().contains("not a valid location");
  }

  @Test
  void shouldFailWhenMaxWarehousesReachedAtNewLocation() {
    Warehouse existing = createWarehouse("MWH.001", "ZWOLLE-001", 100, 10);
    existing.createdAt = LocalDateTime.of(2024, 7, 1, 0, 0);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("TILBURG-001"))
        .thenReturn(new Location("TILBURG-001", 1, 40));

    // Tilburg already has 1 warehouse (max is 1), and the archived one was at ZWOLLE
    Warehouse existingAtTilburg = createWarehouse("MWH.023", "TILBURG-001", 30, 27);
    when(warehouseStore.findActiveByLocation("TILBURG-001"))
        .thenReturn(List.of(existingAtTilburg));

    Warehouse newWarehouse = createWarehouse("MWH.001", "TILBURG-001", 10, 10);

    WarehouseValidationException ex =
        assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));

    assert ex.getMessage().contains("Maximum number of warehouses");
    // update should have been called (archive step) before the location check
    verify(warehouseStore, times(1)).update(any());
  }

  @Test
  void shouldFailWhenCapacityExceededAtNewLocation() {
    Warehouse existing = createWarehouse("MWH.001", "ZWOLLE-001", 100, 10);
    existing.createdAt = LocalDateTime.of(2024, 7, 1, 0, 0);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    // EINDHOVEN-001 has maxCapacity=70
    when(locationResolver.resolveByIdentifier("EINDHOVEN-001"))
        .thenReturn(new Location("EINDHOVEN-001", 3, 70));

    // There is already a warehouse consuming 60 of the 70 capacity
    Warehouse existingAtEindhoven = createWarehouse("MWH.050", "EINDHOVEN-001", 60, 20);
    when(warehouseStore.findActiveByLocation("EINDHOVEN-001"))
        .thenReturn(List.of(existingAtEindhoven));

    // New warehouse wants 20 capacity, but only 10 is available (70-60=10)
    Warehouse newWarehouse = createWarehouse("MWH.001", "EINDHOVEN-001", 20, 10);

    WarehouseValidationException ex =
        assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));

    assert ex.getMessage().contains("would exceed the maximum capacity");
    // Archive step should have been called before the capacity check
    verify(warehouseStore, times(1)).update(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldReplaceAtSameLocation() {
    Warehouse existing = createWarehouse("MWH.001", "AMSTERDAM-001", 50, 10);
    existing.createdAt = LocalDateTime.of(2024, 7, 1, 0, 0);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    // After archiving MWH.001, location query returns the remaining warehouses
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(Collections.emptyList());

    Warehouse newWarehouse = createWarehouse("MWH.001", "AMSTERDAM-001", 80, 10);

    useCase.replace(newWarehouse);

    verify(warehouseStore).update(existing);
    verify(warehouseStore).create(newWarehouse);
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
