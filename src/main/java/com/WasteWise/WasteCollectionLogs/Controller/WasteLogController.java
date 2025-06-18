package com.WasteWise.WasteCollectionLogs.Controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.WasteWise.WasteCollectionLogs.Constants.WasteLogConstants;
import com.WasteWise.WasteCollectionLogs.Dto.VehicleReportDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogResponseDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogStartRequestDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogUpdateRequestDto;
import com.WasteWise.WasteCollectionLogs.Dto.ZoneReportDto;
import com.WasteWise.WasteCollectionLogs.ServiceImpl.WasteLogServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST controller for managing waste collection logs.
 * Provides endpoints for starting, ending, and generating reports for waste collection activities.
 * All endpoints are prefixed with "/api/waste-logs".
 */
@RestController
@RequestMapping("wastewise/admin/wastelogs")
public class WasteLogController {

    private static final Logger logger = LoggerFactory.getLogger(WasteLogController.class);

    private final WasteLogServiceImpl wasteLogService;

    /**
     * Constructs a new WasteLogController with the specified WasteLogService.
     * Spring's dependency injection automatically provides the instance of WasteLogServiceImpl.
     *
     * @param wasteLogService The service responsible for handling waste log business logic.
     */
    public WasteLogController(WasteLogServiceImpl wasteLogService) {
        this.wasteLogService = wasteLogService;
        logger.info("WasteLogController initialized.");
    }

    /**
     * Endpoint to initiate a new waste collection log.
     *
     * @param request The request body containing details to start a collection,
     * including zoneId, vehicleId, and workerId.
     * @return A ResponseEntity containing a WasteLogResponseDto with the generated log ID
     * and a success message, along with HTTP status 201 Created.
     * @throws com.WasteWise.WasteCollectionLogs.Handler.InvalidInputException if input data is invalid.
     */
    @PostMapping("/start")
    public ResponseEntity<WasteLogResponseDto> startCollection(@Validated @RequestBody WasteLogStartRequestDto request) {
        logger.info("Received request to start collection log for zoneId: {}, vehicleId: {}, workerId: {}",
                request.getZoneId(), request.getVehicleId(), request.getWorkerId());
        WasteLogResponseDto response = wasteLogService.startCollection(request);
        logger.info("Successfully started collection log with logId: {}", response.getLogId());
        return new ResponseEntity<>(new WasteLogResponseDto(WasteLogConstants.WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY, response.getLogId()), HttpStatus.CREATED); // HttpStatus.CREATED (201) is appropriate for resource creation
    }

    /**
     * Endpoint to complete an existing waste collection log.
     * This updates the log with an end time and the collected weight.
     *
     * @param request The request body containing the log ID to be updated and the weight collected.
     * @return A ResponseEntity containing a WasteLogResponseDto with the log ID
     * and a completion success message, along with HTTP status 200 OK.
     * @throws com.WasteWise.WasteCollectionLogs.Handler.ResourceNotFoundException if the log ID is not found.
     * @throws com.WasteWise.WasteCollectionLogs.Handler.LogAlreadyCompletedException if the log is already marked as complete.
     * @throws com.WasteWise.WasteCollectionLogs.Handler.InvalidInputException if the end time is before the start time.
     */
    @PutMapping("/end")
    public ResponseEntity<WasteLogResponseDto> endCollection(@Validated @RequestBody WasteLogUpdateRequestDto request) {
        logger.info("Received request to end collection log for logId: {}, collectedWeight: {}",
                request.getLogId(), request.getWeightCollected());
        WasteLogResponseDto response = wasteLogService.endCollection(request);
        logger.info("Successfully ended collection log with logId: {}", response.getLogId());
        return new ResponseEntity<>(new WasteLogResponseDto(WasteLogConstants.WASTE_COLLECTION_LOG_COMPLETED_SUCCESSFULLY,response.getLogId()),HttpStatus.OK); // HttpStatus.OK (200) for successful update
    }

    /**
     * Endpoint to retrieve a daily summary report for a specific waste collection zone.
     * The report includes total weight collected and unique vehicles per day for the specified zone.
     *
     * @param zoneId The unique identifier of the zone.
     * @param startDate The start date for the report range (format: YYYY-MM-DD).
     * @param endDate The end date for the report range (format: YYYY-MM-DD).
     * @return A ResponseEntity containing a list of ZoneReportDto objects,
     * each representing a daily summary for the zone, along with HTTP status 200 OK.
     * @throws com.WasteWise.WasteCollectionLogs.Handler.InvalidInputException if the zoneId is invalid or dates are illogical.
     */
    @GetMapping("/reports/zone/{zoneId}")
    public ResponseEntity<List<ZoneReportDto>> getZoneReport(
            @PathVariable String zoneId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, // Explicit date format
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) { // Explicit date format

        logger.info("Received request for zone report for zoneId: {} from {} to {}", zoneId, startDate, endDate);
        List<ZoneReportDto> dailyReport = wasteLogService.getZoneLogs(zoneId, startDate, endDate);
        logger.info("Returning {} zone report entries for zoneId: {}", dailyReport.size(), zoneId);
        return new ResponseEntity<>(dailyReport, HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve collection logs for a specific vehicle within a given date range.
     *
     * @param vehicleId The unique identifier of the vehicle.
     * @param startDate The start date for the report range (format: YYYY-MM-DD).
     * @param endDate The end date for the report range (format: YYYY-MM-DD).
     * @return A ResponseEntity containing a list of VehicleReportDto objects if logs are found
     * (HTTP status 200 OK), or HTTP status 204 No Content if no logs are found for the vehicle
     * within the specified date range.
     * @throws com.WasteWise.WasteCollectionLogs.Handler.InvalidInputException if the vehicleId is invalid or dates are illogical.
     */
    @GetMapping("/reports/vehicle/{vehicleId}")
    public ResponseEntity<List<VehicleReportDto>> getVehicleReport(
            @PathVariable String vehicleId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        logger.info("Received request for vehicle report for vehicleId: {} from {} to {}", vehicleId, startDate, endDate);
        List<VehicleReportDto> vehicleLogs = wasteLogService.getVehicleLogs(vehicleId, startDate, endDate);
        logger.info("Returning {} vehicle report entries for vehicleId: {}", vehicleLogs.size(), vehicleId);
        return ResponseEntity.ok(vehicleLogs); // 200 OK with the list of logs
    }
}