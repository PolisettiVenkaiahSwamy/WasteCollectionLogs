package com.WasteWise.WasteCollectionLogs.ServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.WasteWise.WasteCollectionLogs.Dto.VehicleReportDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogResponseDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogStartRequestDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogUpdateRequestDto;
import com.WasteWise.WasteCollectionLogs.Dto.ZoneReportDto;
import com.WasteWise.WasteCollectionLogs.Handler.InvalidInputException;
import com.WasteWise.WasteCollectionLogs.Handler.LogAlreadyCompletedException;
import com.WasteWise.WasteCollectionLogs.Handler.NoReportDataFoundException;
import com.WasteWise.WasteCollectionLogs.Handler.ResourceNotFoundException;
import com.WasteWise.WasteCollectionLogs.Model.WasteLog;
import com.WasteWise.WasteCollectionLogs.Repository.WasteLogRepository;
import com.WasteWise.WasteCollectionLogs.Service.WasteLogService;
import com.WasteWise.WasteCollectionLogs.Utility.IdGenerator;


/**
 * Implementation of the {@link WasteLogService} interface.
 * This service class handles the core business logic for managing waste collection logs,
 * including starting, ending, and generating reports. It interacts with the {@link WasteLogRepository}
 * for data persistence and uses {@link IdGenerator} for unique ID creation.
 */

@Service
public class WasteLogServiceImpl implements WasteLogService {
    private static final Logger logger = LoggerFactory.getLogger(WasteLogServiceImpl.class);

    private final WasteLogRepository wasteLogRepository;
    private final IdGenerator idGenerator;


    /**
     * Constructs a new WasteLogServiceImpl with the given repository and ID generator.
     * Spring automatically injects these dependencies.
     *
     * @param wasteLogRepository The repository for WasteLog entities.
     * @param idGenerator The utility for generating unique log IDs.
     */
    public WasteLogServiceImpl(WasteLogRepository wasteLogRepository, IdGenerator idGenerator) {
        this.wasteLogRepository = wasteLogRepository;
        this.idGenerator = idGenerator;
    }


    /**
     * Validates if the provided zone ID is not null or empty.
     *
     * @param id The zone ID to validate.
     * @return true if the ID is valid, false otherwise.
     */

    private boolean isZoneIdValid(String id) {
        logger.debug("Simulating validation for Zone Id:{}", id);
        return id != null && !id.trim().isEmpty();
    }

    private boolean isVehicleIdValid(String id) {
        logger.debug("Simulating validation for Vehicle Id:{}", id);
        return id != null && !id.trim().isEmpty();
    }

    private boolean isWorkerIdValid(String id) {
        logger.debug("Simulating validation for Worker Id:{}", id);
        return id != null && !id.trim().isEmpty();
    }

    /**
     * Initiates a new waste collection log.
     * Performs validation on provided IDs and automatically sets the collection start time.
     *
     * @param request A {@link WasteLogStartRequestDto} containing the zone ID, vehicle ID, and worker ID.
     * @return A {@link WasteLogResponseDto} with a success message and the newly generated log ID.
     * @throws InvalidInputException If any of the provided IDs (zone, vehicle, worker) are invalid.
     * @throws com.Management.WasteCollection.Handler.CollectionTimeOverlapException (Note: This was discussed
     * as a potential exception to add if you implement overlap logic here).
     */

    @Override
    @Transactional
    public WasteLogResponseDto startCollection(WasteLogStartRequestDto request) {
        logger.info("Attempting to start the Waste collection log for Zone:{}, vehicle:{},worker:{}", request.getZoneId(), request.getVehicleId(), request.getWorkerId());

        if (!isZoneIdValid(request.getZoneId())) {
            throw new InvalidInputException("Invalid Zone ID:" + request.getZoneId());
        }
        if (!isVehicleIdValid(request.getVehicleId())) {
            throw new InvalidInputException("Invalid Vehicle ID :" + request.getVehicleId());
        }
        if (!isWorkerIdValid(request.getWorkerId())) {
            throw new InvalidInputException("Invalid Worker ID :" + request.getWorkerId());
        }

        WasteLog wasteLog = new WasteLog();
        wasteLog.setLogId(idGenerator.generateLogId());
        wasteLog.setZoneId(request.getZoneId());
        wasteLog.setVehicleId(request.getVehicleId());
        wasteLog.setWorkerId(request.getWorkerId());

        LocalDateTime now = LocalDateTime.now(); // Capture current time once

        // Automatically set collectionStartTime
        wasteLog.setCollectionStartTime(now);

        // --- NEW: Manually set createdDate and updatedDate for a NEW record ---
        wasteLog.setCreatedDate(now);
        wasteLog.setUpdatedDate(now); // For a new record, updatedDate is also the current time
        // You might set createdBy and updatedBy based on authenticated user or a default here
         wasteLog.setCreatedBy("System");
         wasteLog.setUpdatedBy("System");


        WasteLog savedWasteLog = wasteLogRepository.save(wasteLog);
        logger.info("Waste Collection Log Initiated Successfully with ID :{}", savedWasteLog.getLogId());

        return new WasteLogResponseDto("Waste Collection Log Recorded Successfully", savedWasteLog.getLogId());
    }

    /**
     * Completes an existing waste collection log by setting its end time and collected weight.
     * Performs various validations to ensure the log exists and is in a valid state for completion.
     *
     * @param request A {@link WasteLogUpdateRequestDto} containing the log ID and the weight collected.
     * @return A {@link WasteLogResponseDto} with a completion success message and the log ID.
     * @throws ResourceNotFoundException If the waste log with the given ID does not exist.
     * @throws LogAlreadyCompletedException If the waste log has already been marked as completed.
     * @throws InvalidInputException If the calculated collection end time is before the start time.
     */

    @Override
    @Transactional
    public WasteLogResponseDto endCollection(WasteLogUpdateRequestDto request) {
        logger.info("Attempting to complete Waste Collection Log with ID : {}", request.getLogId());
        WasteLog existingLog = wasteLogRepository.findById(request.getLogId()).orElseThrow(() -> {
            String errorMessage = "Waste Log Not Found With Id{}" + request.getLogId();
            logger.warn(errorMessage);
            return new ResourceNotFoundException(errorMessage);
        });

        if (existingLog.getCollectionEndTime() != null) {
            logger.warn("Attempted to update the already completed Log {}", request.getLogId());
            throw new LogAlreadyCompletedException("Waste Log with ID " + request.getLogId() + " has already been completed.");
        }

        LocalDateTime currentEndTime = LocalDateTime.now(); // Capture current time once

        if (currentEndTime.isBefore(existingLog.getCollectionStartTime())) {
            logger.warn("Invalid End Time for log {} : End Time {} is Before the Start time{}", request.getLogId(), currentEndTime, existingLog.getCollectionStartTime());
            throw new InvalidInputException("Collection End Time cannot be before start time.");
        }

        existingLog.setCollectionEndTime(currentEndTime); // Use the automatically set time
        existingLog.setWeightCollected(request.getWeightCollected());

        // --- NEW: Manually set updatedDate for an UPDATED record ---
        existingLog.setUpdatedDate(LocalDateTime.now()); // Set updated date to now
        // You might set updatedBy based on authenticated user or a default here
        // existingLog.setUpdatedBy("CurrentUser");


        wasteLogRepository.save(existingLog);
        logger.info("Waste collection log with ID:{} completed successfully", request.getLogId());
        return new WasteLogResponseDto("Waste Collection Log Completed Successfully", request.getLogId());
    }


    /**
     * Generates a daily summary report for a specific waste collection zone over a given date range.
     * The summary includes total weight collected and unique vehicles per day for completed logs.
     *
     * @param zoneId The ID of the zone for which to generate the report.
     * @param startDate The start date of the report period (inclusive).
     * @param endDate The end date of the report period (inclusive).
     * @return A list of {@link ZoneReportDto} objects, each summarizing a day's collection for the zone.
     * @throws InvalidInputException If the provided zone ID is invalid.
     */

    @Override
    @Transactional(readOnly = true)
    public List<ZoneReportDto> getZoneLogs(String zoneId, LocalDate startDate, LocalDate endDate) {
        logger.info("Generating daily summary for Zone: {} between {} and {}", zoneId, startDate, endDate);

        if (!isZoneIdValid(zoneId)) {
            throw new InvalidInputException("Invalid Zone ID provided: " + zoneId);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<WasteLog> relevantLogs = wasteLogRepository.findByZoneIdAndCollectionStartTimeBetween(zoneId, startDateTime, endDateTime);

        List<WasteLog> completedLogs = relevantLogs.stream()
                .filter(log -> log.getCollectionEndTime() != null && log.getWeightCollected() != null)
                .collect(Collectors.toList());
        if (completedLogs.isEmpty()) {
            String errorMessage = String.format("No completed waste logs found for Zone ID: %s between %s and %s.", zoneId, startDate, endDate);
            logger.info(errorMessage); 
            throw new NoReportDataFoundException(errorMessage); 
        }
        logger.debug("Found {} completed logs for daily aggregation for Zone {}", completedLogs.size(), zoneId);

        Map<LocalDate, Map<String, List<WasteLog>>> groupedByDateAndZone = completedLogs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getCollectionStartTime().toLocalDate(),
                        Collectors.groupingBy(WasteLog::getZoneId)
                ));

        List<ZoneReportDto> dailyReports = new ArrayList<>();

        groupedByDateAndZone.forEach((date, zoneMap) -> {
            zoneMap.forEach((currentZoneId, logsForDateAndZone) -> {
                double totalWeight = logsForDateAndZone.stream()
                        .mapToDouble(log -> log.getWeightCollected() != null ? log.getWeightCollected() : 0.0)
                        .sum();

                long uniqueVehicles = logsForDateAndZone.stream()
                        .map(WasteLog::getVehicleId)
                        .distinct()
                        .count();

                dailyReports.add(new ZoneReportDto(
                        currentZoneId,
                        date,
                        uniqueVehicles,
                        totalWeight
                ));
            });
        });

        dailyReports.sort((r1, r2) -> r1.getDate().compareTo(r2.getDate()));

        logger.info("Generated daily summary report for Zone {} with {} entries.", zoneId, dailyReports.size());
        return dailyReports;
    }

    /**
     * Retrieves a report of completed waste collection logs for a specific vehicle within a given date range.
     *
     * @param vehicleId The ID of the vehicle for which to generate the report.
     * @param startDate The start date of the report period (inclusive). If null, defaults to today.
     * @param endDate The end date of the report period (inclusive). If null, defaults to today.
     * @return A list of {@link VehicleReportDto} objects, each representing a collection log for the vehicle.
     * @throws InvalidInputException If the provided vehicle ID is invalid or if the start date is after the end date.
     */

    @Override
    @Transactional(readOnly = true)
    public List<VehicleReportDto> getVehicleLogs(String vehicleId, LocalDate startDate, LocalDate endDate) {
        logger.info("Generating vehicle logs for Vehicle: {} between {} and {}", vehicleId, startDate, endDate);

        if (!isVehicleIdValid(vehicleId)) {
            throw new InvalidInputException("Invalid Vehicle ID provided: " + vehicleId);
        }

        LocalDate effectiveStartDate = (startDate != null) ? startDate : LocalDate.now();
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();

        if (effectiveStartDate.isAfter(effectiveEndDate)) {
            throw new InvalidInputException("Start date cannot be after end date.");
        }

        LocalDateTime startDateTime = effectiveStartDate.atStartOfDay();
        LocalDateTime endDateTime = effectiveEndDate.atTime(LocalTime.MAX);

        List<WasteLog> relevantLogs = wasteLogRepository.findByVehicleIdAndCollectionStartTimeBetween(vehicleId, startDateTime, endDateTime);

        List<WasteLog> completedLogs = relevantLogs.stream()
                .filter(log -> log.getCollectionEndTime() != null && log.getWeightCollected() != null)
                .collect(Collectors.toList());
        if (completedLogs.isEmpty()) {
            String errorMessage = String.format("No completed waste logs found for Vehicle ID: %s between %s and %s.", vehicleId, effectiveStartDate, effectiveEndDate);
            logger.info(errorMessage); 
            throw new NoReportDataFoundException(errorMessage); 
        }

        logger.debug("Found {} completed logs for vehicle {}", completedLogs.size(), vehicleId);

        List<VehicleReportDto> vehicleReports = completedLogs.stream()
                .map(log -> new VehicleReportDto(
                        log.getVehicleId(),
                        log.getZoneId(),
                        log.getWeightCollected(),
                        log.getCollectionStartTime().toLocalDate()
                ))
                .collect(Collectors.toList());

        vehicleReports.sort((r1, r2) -> r1.getCollectionDate().compareTo(r2.getCollectionDate()));

        logger.info("Generated vehicle report for Vehicle {} with {} entries.", vehicleId, vehicleReports.size());
        return vehicleReports;
    }
}