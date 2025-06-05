package com.WasteWise.WasteCollectionLogs.Utility;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

import com.WasteWise.WasteCollectionLogs.Repository.WasteLogRepository;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;


/**
 * Utility class to generate unique Log IDs in the format 001, 002, 003, ...
 * The counter persists across application restarts by initializing from the max
 * existing logId in the database.
 */
@Component
@RequiredArgsConstructor // Lombok generates constructor for final fields (like wasteLogRepository)
public class IdGenerator {

    private final WasteLogRepository wasteLogRepository;

    // Single global counter for log IDs
    private AtomicInteger logCounter;

    /**
     * Initializes the global counter based on the maximum existing Log ID from the database.
     * This method runs once after the bean is constructed.
     */
    @PostConstruct
    public void init() {
        String maxLogIdStr = wasteLogRepository.findMaxLogId();
        int initialCounterValue = 0; // Default if no logs exist

        if (maxLogIdStr != null && !maxLogIdStr.trim().isEmpty()) {
            try {
                // Parse the string ID to an integer
                initialCounterValue = Integer.parseInt(maxLogIdStr);
            } catch (NumberFormatException e) {
                // Log an error if maxLogIdStr is not a valid number (e.g., "XYZ")
                System.err.println("Warning: Could not parse max log ID '" + maxLogIdStr + "' to an integer. Starting counter from 0. Error: " + e.getMessage());
            }
        }
        // Initialize the AtomicInteger with the max found ID
        this.logCounter = new AtomicInteger(initialCounterValue);
        System.out.println("LogIdGenerator initialized. Last ID in database: " + initialCounterValue);
    }

    /**
     * Generates a new unique Log ID in the format 001, 002, 003, ...
     * This method is thread-safe due to AtomicInteger.
     *
     * @return generated Log ID (e.g., "004")
     */
    public String generateLogId() {
        int nextLogNumber = logCounter.incrementAndGet();
        // Format the number with leading zeros to at least 3 digits
        return String.format("%03d", nextLogNumber);
    }
}

