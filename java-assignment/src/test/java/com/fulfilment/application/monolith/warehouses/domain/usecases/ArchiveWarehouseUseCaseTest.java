package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class ArchiveWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = Mockito.mock(WarehouseStore.class);
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  void shouldArchiveWarehouseSuccessfully() {
    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "MWH.001";
    existing.location = "ZWOLLE-001";
    existing.capacity = 100;
    existing.stock = 10;
    existing.createdAt = LocalDateTime.of(2024, 7, 1, 0, 0);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    Warehouse toArchive = new Warehouse();
    toArchive.businessUnitCode = "MWH.001";

    useCase.archive(toArchive);

    ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(captor.capture());

    Warehouse archived = captor.getValue();
    assertNotNull(archived.archivedAt);
  }

  @Test
  void shouldFailWhenWarehouseNotFound() {
    when(warehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);

    Warehouse toArchive = new Warehouse();
    toArchive.businessUnitCode = "MWH.999";

    WarehouseValidationException ex =
        assertThrows(WarehouseValidationException.class, () -> useCase.archive(toArchive));

    assert ex.getMessage().contains("No active warehouse found");
    verify(warehouseStore, never()).update(any());
  }

  @Test
  void shouldSetArchivedAtTimestamp() {
    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "MWH.012";
    existing.location = "AMSTERDAM-001";
    existing.capacity = 50;
    existing.stock = 5;
    existing.createdAt = LocalDateTime.of(2023, 7, 1, 0, 0);

    when(warehouseStore.findByBusinessUnitCode("MWH.012")).thenReturn(existing);

    Warehouse toArchive = new Warehouse();
    toArchive.businessUnitCode = "MWH.012";

    LocalDateTime before = LocalDateTime.now();
    useCase.archive(toArchive);
    LocalDateTime after = LocalDateTime.now();

    assertNotNull(existing.archivedAt);
    assert !existing.archivedAt.isBefore(before);
    assert !existing.archivedAt.isAfter(after);
  }
}
