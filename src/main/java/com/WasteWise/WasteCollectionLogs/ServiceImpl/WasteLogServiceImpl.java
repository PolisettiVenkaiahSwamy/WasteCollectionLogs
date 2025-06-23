package com.WasteWise.WasteCollectionLogs.ServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page; 
import org.springframework.data.domain.PageImpl; 
import org.springframework.data.domain.Pageable;
import com.WasteWise.WasteCollectionLogs.Constants.WasteLogConstants;
import com.WasteWise.WasteCollectionLogs.Dto.VehicleReportDTO;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogResponseDTO;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogStartRequestDTO;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogUpdateRequestDTO;
import com.WasteWise.WasteCollectionLogs.Dto.ZoneReportDTO;
import com.WasteWise.WasteCollectionLogs.Handler.InvalidInputException;
import com.WasteWise.WasteCollectionLogs.Handler.LogAlreadyCompletedException;
import com.WasteWise.WasteCollectionLogs.Handler.ResourceNotFoundException;
import com.WasteWise.WasteCollectionLogs.Model.WasteLog;
import com.WasteWise.WasteCollectionLogs.Repository.WasteLogRepository;

/**
 * Service implementation for managing waste collection logs.
 * This class handles the business logic related to starting, ending, and reporting waste collection activities.
 */
@Service
public class WasteLogServiceImpl {

	private static final Logger logger = LoggerFactory.getLogger(WasteLogServiceImpl.class);
	
    private final WasteLogRepository wasteLogRepository;

    /**
     * Constructs a new WasteLogServiceImpl with the given WasteLogRepository.
     *
     * @param wasteLogRepository The repository for accessing waste log data.
     */
    public WasteLogServiceImpl(WasteLogRepository wasteLogRepository) {
        this.wasteLogRepository = wasteLogRepository;
        logger.info("WasteLogServiceImpl initialized.");
    }

    /**
     * Validates if the start date is not after the end date.
     *
     * @param startDate The start date of the reporting period.
     * @param endDate The end date of the reporting period.
     * @throws InvalidInputException if the end date is before the start date.
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
    	logger.debug("Validating date range: startDate={}, endDate={}", startDate, endDate);
        if (startDate.isAfter(endDate)) {
        	logger.warn("InvalidDateRange: End date {} cannot be before start date {}", endDate, startDate);
            throw new InvalidInputException(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE);
        }
        logger.debug("Date range validation successful.");
    }

    /**
     * Validates that no active waste collection log already exists for a given worker, zone, and vehicle.
     * An active log is one where the `collectionEndTime` is null.
     *
     * @param workerId The ID of the worker.
     * @param zoneId The ID of the zone.
     * @param vehicleId The ID of the vehicle.
     * @throws InvalidInputException if an active log is found for the given criteria.
     */
    private void validateNoActiveLogExists(String workerId, String zoneId, String vehicleId) {
    	 logger.debug("Checking for active log for workerId={}, zoneId={}, vehicleId={}", workerId, zoneId, vehicleId);
        if (wasteLogRepository.findByWorkerIdAndZoneIdAndVehicleIdAndCollectionEndTimeIsNull(
                workerId, zoneId, vehicleId).isPresent()) {
        	 logger.warn("ActiveLogExists: An active log already exists for workerId={}, zoneId={}, vehicleId={}", workerId, zoneId, vehicleId);
            throw new InvalidInputException( String.format(WasteLogConstants.ACTIVE_LOG_EXISTS_MESSAGE, workerId, zoneId, vehicleId));
        }
        logger.debug("No active log found for workerId={}, zoneId={}, vehicleId={}", workerId, zoneId, vehicleId);
    }
    // --- Public Service Methods ---

    /**
     * Starts a new waste collection log.
     * Validates that no active log exists for the given worker, zone, and vehicle before creating a new log.
     *
     * @param request The DTO containing information to start a waste collection log (worker ID, zone ID, vehicle ID).
     * @return A WasteLogResponseDto with the ID of the newly created log and a success message.
     */
    public WasteLogResponseDTO startCollection(WasteLogStartRequestDTO request) { 
    	 logger.info("Attempting to start new collection log for workerId={}, zoneId={}, vehicleId={}",
                 request.getWorkerId(), request.getZoneId(), request.getVehicleId());
        // The DTO validation ensures the request is valid before it reaches here.
    	validateNoActiveLogExists(request.getWorkerId(), request.getZoneId(), request.getVehicleId());

        WasteLog wasteLog = new WasteLog();
        wasteLog.setZoneId(request.getZoneId());
        wasteLog.setVehicleId(request.getVehicleId());
        wasteLog.setWorkerId(request.getWorkerId());
        wasteLog.setCollectionStartTime(LocalDateTime.now());
        wasteLog.setCreatedDate(LocalDateTime.now());

        wasteLog = wasteLogRepository.save(wasteLog);
        logger.info("New collection log started successfully with ID: {}", wasteLog.getLogId());

        return new WasteLogResponseDTO(wasteLog.getLogId(), WasteLogConstants.WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY);
    }

    /**
     * Ends an existing waste collection log.
     * Retrieves the log by its ID, validates that it hasn't been completed already,
     * and ensures the end time is not before the start time.
     *
     * @param request The DTO containing the log ID and the weight collected.
     * @return A WasteLogResponseDto with the ID of the updated log and a success message.
     * @throws ResourceNotFoundException if the waste log with the given ID is not found.
     * @throws LogAlreadyCompletedException if the waste log has already been completed.
     * @throws InvalidInputException if the collection end time is before the collection start time.
     */
    public WasteLogResponseDTO endCollection(WasteLogUpdateRequestDTO request) {

    	logger.info("Attempting to end collection log with ID: {} and weight: {}", request.getLogId(), request.getWeightCollected());
    	WasteLog wasteLog = wasteLogRepository.findById(request.getLogId())
                .orElseThrow(() -> {
                    logger.warn("ResourceNotFound: Waste log with ID {} not found.", request.getLogId());
                    return new ResourceNotFoundException(String.format(WasteLogConstants.WASTE_LOG_NOT_FOUND_MESSAGE, request.getLogId()));
                });
        if (wasteLog.getCollectionEndTime() != null) {
        	logger.warn("LogAlreadyCompleted: Waste log with ID {} is already completed.", request.getLogId());
            throw new LogAlreadyCompletedException(String.format(WasteLogConstants.LOG_ALREADY_COMPLETED_MESSAGE, request.getLogId()));
        }

        LocalDateTime currentEndTime = LocalDateTime.now();
        
        if (currentEndTime.isBefore(wasteLog.getCollectionStartTime())) {
        	 logger.warn("InvalidInput: Collection end time {} is before start time {}", currentEndTime, wasteLog.getCollectionStartTime());
            throw new InvalidInputException(WasteLogConstants.COLLECTION_END_TIME_BEFORE_START_TIME);
        }

        wasteLog.setCollectionEndTime(currentEndTime);
        wasteLog.setWeightCollected(request.getWeightCollected());
        wasteLog.setUpdatedDate(LocalDateTime.now());

        wasteLogRepository.save(wasteLog);
        logger.info("Collection log with ID: {} completed successfully.", wasteLog.getLogId());
        return new WasteLogResponseDTO(wasteLog.getLogId(), WasteLogConstants.WASTE_COLLECTION_LOG_COMPLETED_SUCCESSFULLY);
    }


    /**
     * Retrieves a report of waste collection logs for a specific zone within a given date range.
     * The reports are grouped by date and include total weight collected and the count of unique vehicles used.
     *
     * @param zoneId The ID of the zone to retrieve logs for.
     * @param startDate The start date of the reporting period.
     * @param endDate The end date of the reporting period.
     * @param pageable Pagination and sorting information.
     * @return A Page of ZoneReportDto objects, containing daily summaries.
     * @throws InvalidInputException if the end date is before the start date.
     */
    public Page<ZoneReportDTO> getZoneLogs(String zoneId, LocalDate startDate, LocalDate endDate, Pageable pageable) { 
    	 logger.info("Generating zone report for zoneId={}, startDate={}, endDate={}, pageable={}",
                 zoneId, startDate, endDate, pageable);
        validateDateRange(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<WasteLog> logs = wasteLogRepository.findByZoneIdAndCollectionStartTimeBetween(zoneId, startDateTime, endDateTime);
        logger.debug("Found {} waste logs for zoneId={} between {} and {}", logs.size(), zoneId, startDateTime, endDateTime);

        Map<LocalDate, List<WasteLog>> groupedByDate = logs.stream()
                .filter(log -> log.getCollectionEndTime() != null)
                .collect(Collectors.groupingBy(log -> log.getCollectionStartTime().toLocalDate()));
        logger.debug("Grouped {} completed logs by date for zoneId={}", groupedByDate.size(), zoneId);
        List<ZoneReportDTO> reports = groupedByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<WasteLog> dailyLogs = entry.getValue();
                    double totalWeight = dailyLogs.stream()
                            .mapToDouble(WasteLog::getWeightCollected)
                            .sum();
                    Set<String> uniqueVehicles = dailyLogs.stream()
                            .map(WasteLog::getVehicleId)
                            .collect(Collectors.toSet());
                    logger.trace("Daily summary for zoneId={} on {}: totalWeight={}, uniqueVehicles={}", zoneId, date, totalWeight, uniqueVehicles.size());
                    return new ZoneReportDTO(zoneId, date, (long) uniqueVehicles.size(), totalWeight);
                })
                .sorted((r1, r2) -> r1.getDate().compareTo(r2.getDate())) // Your original sorting by date
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), reports.size());

        List<ZoneReportDTO> pageContent;
        if (start > reports.size()) {
            pageContent = List.of(); 
            logger.debug("Requested page start index {} is beyond report size {} for zoneId={}. Returning empty page.", start, reports.size(), zoneId);
        } else {
            pageContent = reports.subList(start, end);
            logger.debug("Returning page {} with {} entries for zoneId={}", pageable.getPageNumber(), pageContent.size(), zoneId);
        }

        return new PageImpl<>(pageContent, pageable, reports.size());
      
    }

    /**
     * Retrieves a report of waste collection logs for a specific vehicle within a given date range.
     *
     * @param vehicleId The ID of the vehicle to retrieve logs for.
     * @param startDate The start date of the reporting period.
     * @param endDate The end date of the reporting period.
     * @param pageable Pagination and sorting information.
     * @return A Page of VehicleReportDto objects.
     * @throws InvalidInputException if the end date is before the start date.
     */
    public Page<VehicleReportDTO> getVehicleLogs(String vehicleId, LocalDate startDate, LocalDate endDate, Pageable pageable) { 
    	 logger.info("Generating vehicle report for vehicleId={}, startDate={}, endDate={}, pageable={}",
                 vehicleId, startDate, endDate, pageable);
        validateDateRange(startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<WasteLog> logs = wasteLogRepository.findByVehicleIdAndCollectionStartTimeBetween(vehicleId, startDateTime, endDateTime);
        logger.debug("Found {} waste logs for vehicleId={} between {} and {}", logs.size(), vehicleId, startDateTime, endDateTime);
        List<VehicleReportDTO> reports = logs.stream()
                .filter(log -> log.getCollectionEndTime() != null) // Only include completed logs
                .map(log -> new VehicleReportDTO(
                        log.getVehicleId(),
                        log.getZoneId(),
                        log.getWeightCollected(),
                        log.getCollectionStartTime().toLocalDate()
                ))
                .sorted((r1, r2) -> r1.getCollectionDate().compareTo(r2.getCollectionDate())) // Your original sorting by collection date
                .collect(Collectors.toList());
        logger.debug("Prepared {} VehicleReportDTO entries for vehicleId={}", reports.size(), vehicleId);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), reports.size());

        List<VehicleReportDTO> pageContent;
        if (start > reports.size()) {
            pageContent = List.of(); 
            logger.debug("Requested page start index {} is beyond report size {} for vehicleId={}. Returning empty page.", start, reports.size(), vehicleId);
        } else {
            pageContent = reports.subList(start, end);
            logger.debug("Returning page {} with {} entries for vehicleId={}", pageable.getPageNumber(), pageContent.size(), vehicleId);
        }

        return new PageImpl<>(pageContent, pageable, reports.size());
    }
}