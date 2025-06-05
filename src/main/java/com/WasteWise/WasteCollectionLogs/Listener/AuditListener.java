package com.WasteWise.WasteCollectionLogs.Listener;
 
import com.WasteWise.WasteCollectionLogs.Model.WasteLog; // Import your entity
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

public class AuditListener {

    // You might want to get the 'current user' from Spring Security context
    // For simplicity, we'll use a placeholder "SYSTEM" or "USER" for now.
    // In a real application, you'd use:
    // SecurityContextHolder.getContext().getAuthentication().getName();
    private String getCurrentUser() {
        // Placeholder: Replace with actual logic to get the logged-in user
        return "SYSTEM"; // Or "LoggedInUser" if you have security context
    }

    /**
     * Sets createdDate and createdBy before a new WasteLog entity is persisted to the database.
     *
     * @param wasteLog The WasteLog entity being persisted.
     */
    @PrePersist
    public void prePersist(WasteLog wasteLog) {
        LocalDateTime now = LocalDateTime.now();
        String currentUser = getCurrentUser();

        // Set creation audit fields only if they are not already set (e.g., manually in tests)
        if (wasteLog.getCreatedDate() == null) {
            wasteLog.setCreatedDate(now);
        }
        if (wasteLog.getCreatedBy() == null || wasteLog.getCreatedBy().isEmpty()) {
            wasteLog.setCreatedBy(currentUser);
        }
        
        // On initial creation, updatedDate and updatedBy are often set to the same as created
        if (wasteLog.getUpdatedDate() == null) {
             wasteLog.setUpdatedDate(now);
        }
        if (wasteLog.getUpdatedBy() == null || wasteLog.getUpdatedBy().isEmpty()) {
             wasteLog.setUpdatedBy(currentUser);
        }
    }

    /**
     * Sets updatedDate and updatedBy before an existing WasteLog entity is updated in the database.
     *
     * @param wasteLog The WasteLog entity being updated.
     */
    @PreUpdate
    public void preUpdate(WasteLog wasteLog) {
        wasteLog.setUpdatedDate(LocalDateTime.now());
        wasteLog.setUpdatedBy(getCurrentUser());
    }
}
