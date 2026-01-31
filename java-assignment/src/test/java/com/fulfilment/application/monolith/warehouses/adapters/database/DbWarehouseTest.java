package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class DbWarehouseTest {

  @Test
  void shouldCreateDbWarehouseWithDefaultConstructor() {
    DbWarehouse dbWarehouse = new DbWarehouse();

    assertNull(dbWarehouse.id);
    assertNull(dbWarehouse.businessUnitCode);
    assertNull(dbWarehouse.location);
    assertNull(dbWarehouse.capacity);
    assertNull(dbWarehouse.stock);
    assertNull(dbWarehouse.createdAt);
    assertNull(dbWarehouse.archivedAt);
  }

  @Test
  void shouldConvertToWarehouseDomainModel() {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.id = 1L;
    dbWarehouse.businessUnitCode = "MWH.001";
    dbWarehouse.location = "AMSTERDAM-001";
    dbWarehouse.capacity = 100;
    dbWarehouse.stock = 50;
    dbWarehouse.createdAt = LocalDateTime.of(2024, 7, 1, 0, 0);
    dbWarehouse.archivedAt = null;

    Warehouse warehouse = dbWarehouse.toWarehouse();

    assertEquals("MWH.001", warehouse.businessUnitCode);
    assertEquals("AMSTERDAM-001", warehouse.location);
    assertEquals(100, warehouse.capacity);
    assertEquals(50, warehouse.stock);
    assertEquals(LocalDateTime.of(2024, 7, 1, 0, 0), warehouse.createdAt);
    assertNull(warehouse.archivedAt);
  }

  @Test
  void shouldConvertArchivedWarehouseToDomainModel() {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = "MWH.002";
    dbWarehouse.location = "ZWOLLE-001";
    dbWarehouse.capacity = 50;
    dbWarehouse.stock = 25;
    dbWarehouse.createdAt = LocalDateTime.of(2023, 1, 1, 0, 0);
    dbWarehouse.archivedAt = LocalDateTime.of(2024, 6, 1, 0, 0);

    Warehouse warehouse = dbWarehouse.toWarehouse();

    assertEquals("MWH.002", warehouse.businessUnitCode);
    assertEquals(LocalDateTime.of(2024, 6, 1, 0, 0), warehouse.archivedAt);
  }

  @Test
  void shouldSetAllFields() {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.id = 10L;
    dbWarehouse.businessUnitCode = "MWH.TEST";
    dbWarehouse.location = "TILBURG-001";
    dbWarehouse.capacity = 75;
    dbWarehouse.stock = 30;
    dbWarehouse.createdAt = LocalDateTime.now();
    dbWarehouse.archivedAt = LocalDateTime.now().plusDays(1);

    assertEquals(10L, dbWarehouse.id);
    assertEquals("MWH.TEST", dbWarehouse.businessUnitCode);
    assertEquals("TILBURG-001", dbWarehouse.location);
    assertEquals(75, dbWarehouse.capacity);
    assertEquals(30, dbWarehouse.stock);
  }
}
