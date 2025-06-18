package com.WasteWise.WasteCollectionLogs.Constants;

public class WasteLogConstants {
      
	    public static final String WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY = "Waste Collection Log Recorded Successfully";
	    public static final String WASTE_COLLECTION_LOG_COMPLETED_SUCCESSFULLY = "Waste Collection Log Completed Successfully";
	    public static final String WASTE_LOG_NOT_FOUND_MESSAGE = "Waste Log Not Found With Id{}";
	    public static final String LOG_ALREADY_COMPLETED_MESSAGE = "Waste Log with ID %s has already been completed.";
	    public static final String COLLECTION_END_TIME_BEFORE_START_TIME = "Collection End Time cannot be before start time.";
	    public static final String INVALID_ZONE_ID_PROVIDED = "Invalid Zone ID provided %s"; 
	    public static final String INVALID_VEHICLE_ID_PROVIDED = "Invalid Vehicle ID provided %s";
	    public static final String INVALID_WORKER_ID_PROVIDED = "Invalid Worker ID provided %s"; 
	    public static final String START_DATE_CANNOT_BE_AFTER_END_DATE = "Start date cannot be after end date.";
	    public static final String NO_COMPLETED_LOGS_FOUND_ZONE = "No active completed logs found for zone ID: %s between %s and %s.";
	    public static final String NO_COMPLETED_LOGS_FOUND_VEHICLE = "No active completed logs found for vehicle ID: %s in the period %s to %s. Returning empty list.";
	    public static final String VEHICLE_REPORT_GENERATED_SUCCESSFULLY = "Vehicle report generated successfully.";
	    public static final String NO_VEHICLE_REPORT_ENTRIES_FOUND = "No vehicle report entries found for vehicleId: %s in the specified date range.";

}
