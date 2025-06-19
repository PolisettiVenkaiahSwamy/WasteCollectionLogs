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

@Service
public class WasteLogServiceImpl {

    private final WasteLogRepository wasteLogRepository;

    public WasteLogServiceImpl(WasteLogRepository wasteLogRepository) {
        this.wasteLogRepository = wasteLogRepository;
    }

    // --- Helper Validation Methods (Private) ---

    // This method is now empty for basic input format/presence checks,
    // as those are handled by @NotBlank and @Pattern on WasteLogStartRequestDto
    // and @Valid in the controller.
    // Keep it only if you have complex business logic validations (e.g., DB existence checks
    // not covered by simple annotations). If no such checks, the call to this method can also be removed.
    
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new InvalidInputException(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE);
        }
    }

    // --- Public Service Methods ---

    public WasteLogResponseDto startCollection(WasteLogStartRequestDto request) {
        // The DTO validation ensures the request is valid before it reaches here.
        // The call below `validateWasteLogStartRequest` will execute an empty method for now.
        // If no complex business logic validation is added to `validateWasteLogStartRequest`,
    	
        WasteLog wasteLog = new WasteLog();
        wasteLog.setZoneId(request.getZoneId());
        wasteLog.setVehicleId(request.getVehicleId());
        wasteLog.setWorkerId(request.getWorkerId());
        wasteLog.setCollectionStartTime(LocalDateTime.now());
        wasteLog.setCreatedDate(LocalDateTime.now());

        wasteLog = wasteLogRepository.save(wasteLog);

        return new WasteLogResponseDto(wasteLog.getLogId(), WasteLogConstants.WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY);
    }

    public WasteLogResponseDto endCollection(WasteLogUpdateRequestDto request) {
        // --- FIX HERE: Remove the call to validateWasteLogUpdateRequest(request); ---
        // This validation is now handled by @NotNull and @Positive on WasteLogUpdateRequestDto
        // and @Valid in the controller.

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

    public List<ZoneReportDto> getZoneLogs(String zoneId, LocalDate startDate, LocalDate endDate) {
        // --- FIX HERE: No longer need to validate zoneId here. Handled by controller. ---
        // The controller's @Pattern on @PathVariable zoneId will handle the format validation.
        // Your code correctly removed the if-block, which is good.

        validateDateRange(startDate, endDate); // This is a business logic validation, keep it.

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
    
    public List<VehicleReportDto> getVehicleLogs(String vehicleId, LocalDate startDate, LocalDate endDate) {
        // --- FIX HERE: No longer need to validate vehicleId here. Handled by controller. ---
        // The controller's @Pattern on @PathVariable vehicleId will handle the format validation.
        // Your code correctly removed the if-block, which is good.

        validateDateRange(startDate, endDate); // This is a business logic validation, keep it.

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<WasteLog> logs = wasteLogRepository.findByVehicleIdAndCollectionStartTimeBetween(vehicleId, startDateTime, endDateTime);

        List<VehicleReportDto> reports = logs.stream()
                .filter(log -> log.getCollectionEndTime() != null)
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