package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

  private LocationGateway locationGateway;

  @BeforeEach
  void setUp() {
    locationGateway = new LocationGateway();
  }

  @Test
  public void testWhenResolveExistingLocationShouldReturn() {
    Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

    assertNotNull(location);
    assertEquals("ZWOLLE-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  public void testResolveAmsterdamLocation() {
    Location location = locationGateway.resolveByIdentifier("AMSTERDAM-001");

    assertNotNull(location);
    assertEquals("AMSTERDAM-001", location.identification);
    assertEquals(5, location.maxNumberOfWarehouses);
    assertEquals(100, location.maxCapacity);
  }

  @Test
  public void testResolveAllKnownLocations() {
    String[] identifiers = {
      "ZWOLLE-001", "ZWOLLE-002", "AMSTERDAM-001", "AMSTERDAM-002",
      "TILBURG-001", "HELMOND-001", "EINDHOVEN-001", "VETSBY-001"
    };

    for (String id : identifiers) {
      Location location = locationGateway.resolveByIdentifier(id);
      assertNotNull(location, "Location should exist for identifier: " + id);
      assertEquals(id, location.identification);
    }
  }

  @Test
  public void testResolveNonExistentLocationReturnsNull() {
    Location location = locationGateway.resolveByIdentifier("NONEXISTENT-001");

    assertNull(location);
  }

  @Test
  public void testResolveWithEmptyStringReturnsNull() {
    Location location = locationGateway.resolveByIdentifier("");

    assertNull(location);
  }

  @Test
  public void testResolveCaseSensitive() {
    // Identifiers are case-sensitive; lowercase should not match
    Location location = locationGateway.resolveByIdentifier("zwolle-001");

    assertNull(location);
  }

  @Test
  public void testLocationCapacityValues() {
    Location zwolle2 = locationGateway.resolveByIdentifier("ZWOLLE-002");
    assertNotNull(zwolle2);
    assertEquals(2, zwolle2.maxNumberOfWarehouses);
    assertEquals(50, zwolle2.maxCapacity);

    Location eindhoven = locationGateway.resolveByIdentifier("EINDHOVEN-001");
    assertNotNull(eindhoven);
    assertEquals(2, eindhoven.maxNumberOfWarehouses);
    assertEquals(70, eindhoven.maxCapacity);
  }
}
