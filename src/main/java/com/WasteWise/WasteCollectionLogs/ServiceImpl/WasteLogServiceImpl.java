package com.WasteWise.WasteCollectionLogs.ServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.WasteWise.WasteCollectionLogs.Constants.WasteLogConstants;
import com.WasteWise.WasteCollectionLogs.Dto.VehicleReportDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogResponseDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogStartRequestDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogUpdateRequestDto;
import com.WasteWise.WasteCollectionLogs.Dto.ZoneReportDto;
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

    private final WasteLogRepository wasteLogRepository;

    /**
     * Constructs a new WasteLogServiceImpl with the given WasteLogRepository.
     *
     * @param wasteLogRepository The repository for accessing waste log data.
     */
    public WasteLogServiceImpl(WasteLogRepository wasteLogRepository) {
        this.wasteLogRepository = wasteLogRepository;
    }

    /**
     * Validates if the start date is not after the end date.
     *
     * @param startDate The start date of the reporting period.
     * @param endDate The end date of the reporting period.
     * @throws InvalidInputException if the end date is before the start date.
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new InvalidInputException(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE);
        }
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
        if (wasteLogRepository.findByWorkerIdAndZoneIdAndVehicleIdAndCollectionEndTimeIsNull(
                workerId, zoneId, vehicleId).isPresent()) {
            throw new InvalidInputException( String.format(WasteLogConstants.ACTIVE_LOG_EXISTS_MESSAGE, workerId, zoneId, vehicleId));
        }
    }
    // --- Public Service Methods ---

    /**
     * Starts a new waste collection log.
     * Validates that no active log exists for the given worker, zone, and vehicle before creating a new log.
     *
     * @param request The DTO containing information to start a waste collection log (worker ID, zone ID, vehicle ID).
     * @return A WasteLogResponseDto with the ID of the newly created log and a success message.
     */
    public WasteLogResponseDto startCollection(WasteLogStartRequestDto request) {
        // The DTO validation ensures the request is valid before it reaches here.
    	validateNoActiveLogExists(request.getWorkerId(), request.getZoneId(), request.getVehicleId());

        WasteLog wasteLog = new WasteLog();
        wasteLog.setZoneId(request.getZoneId());
        wasteLog.setVehicleId(request.getVehicleId());
        wasteLog.setWorkerId(request.getWorkerId());
        wasteLog.setCollectionStartTime(LocalDateTime.now());
        wasteLog.setCreatedDate(LocalDateTime.now());

        wasteLog = wasteLogRepository.save(wasteLog);

        return new WasteLogResponseDto(wasteLog.getLogId(), WasteLogConstants.WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY);
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
    public WasteLogResponseDto endCollection(WasteLogUpdateRequestDto request) {

        WasteLog wasteLog = wasteLogRepository.findById(request.getLogId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(WasteLogConstants.WASTE_LOG_NOT_FOUND_MESSAGE, request.getLogId())));

        if (wasteLog.getCollectionEndTime() != null) {
            throw new LogAlreadyCompletedException(String.format(WasteLogConstants.LOG_ALREADY_COMPLETED_MESSAGE, request.getLogId()));
        }

        LocalDateTime currentEndTime = LocalDateTime.now();
        if (currentEndTime.isBefore(wasteLog.getCollectionStartTime())) {
            throw new InvalidInputException(WasteLogConstants.COLLECTION_END_TIME_BEFORE_START_TIME);
        }

        wasteLog.setCollectionEndTime(currentEndTime);
        wasteLog.setWeightCollected(request.getWeightCollected());
        wasteLog.setUpdatedDate(LocalDateTime.now());

        wasteLogRepository.save(wasteLog);

        return new WasteLogResponseDto(wasteLog.getLogId(), WasteLogConstants.WASTE_COLLECTION_LOG_COMPLETED_SUCCESSFULLY);
    }

    /**
     * Retrieves a report of waste collection logs for a specific zone within a given date range.
     * The reports are grouped by date and include total weight collected and the count of unique vehicles used.
     *
     * @param zoneId The ID of the zone to retrieve logs for.
     * @param startDate The start date of the reporting period.
     * @param endDate The end date of the reporting period.
     * @return A list of ZoneReportDto objects, sorted by date.
     * @throws InvalidInputException if the end date is before the start date.
     */
    public List<ZoneReportDto> getZoneLogs(String zoneId, LocalDate startDate, LocalDate endDate) {

        validateDateRange(startDate, endDate); 

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<WasteLog> logs = wasteLogRepository.findByZoneIdAndCollectionStartTimeBetween(zoneId, startDateTime, endDateTime);

        Map<LocalDate, List<WasteLog>> groupedByDate = logs.stream()
                .filter(log -> log.getCollectionEndTime() != null) 
                .collect(Collectors.groupingBy(log -> log.getCollectionStartTime().toLocalDate()));

        List<ZoneReportDto> reports = groupedByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<WasteLog> dailyLogs = entry.getValue();
                    double totalWeight = dailyLogs.stream()
                            .mapToDouble(WasteLog::getWeightCollected)
                            .sum();
                    Set<String> uniqueVehicles = dailyLogs.stream()
                            .map(WasteLog::getVehicleId)
                            .collect(Collectors.toSet());

                    return new ZoneReportDto(zoneId, date, (long) uniqueVehicles.size(), totalWeight);
                })
                .sorted((r1, r2) -> r1.getDate().compareTo(r2.getDate()))
                .collect(Collectors.toList());

        return reports;
    }

    /**
     * Retrieves a report of waste collection logs for a specific vehicle within a given date range.
     *
     * @param vehicleId The ID of the vehicle to retrieve logs for.
     * @param startDate The start date of the reporting period.
     * @param endDate The end date of the reporting period.
     * @return A list of VehicleReportDto objects, sorted by collection date.
     * @throws InvalidInputException if the end date is before the start date.
     */
    public List<VehicleReportDto> getVehicleLogs(String vehicleId, LocalDate startDate, LocalDate endDate) {
     
        validateDateRange(startDate, endDate); 

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<WasteLog> logs = wasteLogRepository.findByVehicleIdAndCollectionStartTimeBetween(vehicleId, startDateTime, endDateTime);

        List<VehicleReportDto> reports = logs.stream()
                .filter(log -> log.getCollectionEndTime() != null) // Only include completed logs
                .map(log -> new VehicleReportDto(
                        log.getVehicleId(),
                        log.getZoneId(),
                        log.getWeightCollected(),
                        log.getCollectionStartTime().toLocalDate()
                ))
                .sorted((r1, r2) -> r1.getCollectionDate().compareTo(r2.getCollectionDate()))
                .collect(Collectors.toList());

        return reports;
    }
}