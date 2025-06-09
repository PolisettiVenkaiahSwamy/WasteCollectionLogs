package com.WasteWise.WasteCollectionLogs.Listener;
 
import com.WasteWise.WasteCollectionLogs.Model.WasteLog; // Import your entity
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

/**
 * An entity listener for auditing {@link WasteLog} entities.
 * This class automatically populates creation and update audit fields
 * (like created date/by and updated date/by) before an entity is persisted or updated.
 */
public class AuditListener {

    /**
     * Retrieves the current user's identifier.
     * In a real application, this would typically fetch the authenticated user
     * from Spring Security's context or a similar mechanism.
     *
     * @return A {@link String} representing the current user (currently hardcoded as "SYSTEM").
     */
    private String getCurrentUser() {
               return "SYSTEM"; // TODO: Replace with actual authenticated user retrieval (e.g., from Spring SecurityContextHolder)
    }

    /**
     * Sets createdDate and createdBy before a new WasteLog entity is persisted to the database.
     * It also initializes updatedDate and updatedBy to the same values upon creation.
     * This method is automatically invoked by Jakarta Persistence (JPA) lifecycle callbacks.
     *
     * @param wasteLog The {@link WasteLog} entity being persisted.
     */
    @PrePersist // Marks this method to be called before a new entity is persisted.
    public void prePersist(WasteLog wasteLog) {
        LocalDateTime now = LocalDateTime.now(); // Gets the current timestamp.
        String currentUser = getCurrentUser(); // Gets the current user.

        // Set creation audit fields only if they are not already set (e.g., manually in tests or initial DTO mapping)
        if (wasteLog.getCreatedDate() == null) {
            wasteLog.setCreatedDate(now); // Sets the creation timestamp.
        }
        if (wasteLog.getCreatedBy() == null || wasteLog.getCreatedBy().isEmpty()) {
            wasteLog.setCreatedBy(currentUser); // Sets the creator.
        }
        
        // On initial creation, updatedDate and updatedBy are often set to the same as created
        // This ensures these fields are not null immediately after creation.
        if (wasteLog.getUpdatedDate() == null) {
             wasteLog.setUpdatedDate(now); // Sets the update timestamp (initially same as created).
        }
        if (wasteLog.getUpdatedBy() == null || wasteLog.getUpdatedBy().isEmpty()) {
             wasteLog.setUpdatedBy(currentUser); // Sets the updater (initially same as created).
        }
    }

    /**
     * Sets updatedDate and updatedBy before an existing WasteLog entity is updated in the database.
     * This method is automatically invoked by Jakarta Persistence (JPA) lifecycle callbacks.
     *
     * @param wasteLog The {@link WasteLog} entity being updated.
     */
    @PreUpdate // Marks this method to be called before an existing entity is updated.
    public void preUpdate(WasteLog wasteLog) {
        wasteLog.setUpdatedDate(LocalDateTime.now()); // Updates the timestamp to the current time.
        wasteLog.setUpdatedBy(getCurrentUser()); // Updates the user who performed the update.
    }
}