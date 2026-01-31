package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LegacyStoreManagerGateway {

  private static final Logger LOGGER = Logger.getLogger(LegacyStoreManagerGateway.class.getName());

  /**
   * Observes store events and propagates changes to the legacy system only after the database
   * transaction has committed successfully. This guarantees the downstream legacy system receives
   * confirmed data.
   */
  public void onStoreEvent(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreEvent event) {
    switch (event.getType()) {
      case CREATED -> createStoreOnLegacySystem(event.getStore());
      case UPDATED -> updateStoreOnLegacySystem(event.getStore());
    }
  }

  public void createStoreOnLegacySystem(Store store) {
    LOGGER.infof("Propagating store creation to legacy system: %s", store.name);
    writeToFile(store);
  }

  public void updateStoreOnLegacySystem(Store store) {
    LOGGER.infof("Propagating store update to legacy system: %s", store.name);
    writeToFile(store);
  }

  private void writeToFile(Store store) {
    try {
      Path tempFile = Files.createTempFile(store.name, ".txt");
      LOGGER.debugf("Temporary file created at: %s", tempFile.toString());

      String content =
          "Store created. [ name ="
              + store.name
              + " ] [ items on stock ="
              + store.quantityProductsInStock
              + "]";
      Files.write(tempFile, content.getBytes());

      String readContent = new String(Files.readAllBytes(tempFile));
      LOGGER.debugf("Data read from temporary file: %s", readContent);

      Files.delete(tempFile);

    } catch (Exception e) {
      LOGGER.error("Failed to propagate store change to legacy system", e);
    }
  }
}
