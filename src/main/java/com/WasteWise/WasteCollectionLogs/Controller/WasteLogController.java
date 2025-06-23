package com.WasteWise.WasteCollectionLogs.Controller;

import com.WasteWise.WasteCollectionLogs.Constants.WasteLogConstants;
import com.WasteWise.WasteCollectionLogs.Payload.RestResponse; 
import com.WasteWise.WasteCollectionLogs.Dto.VehicleReportDTO;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogResponseDTO;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogStartRequestDTO;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogUpdateRequestDTO;
import com.WasteWise.WasteCollectionLogs.Dto.ZoneReportDTO;
import com.WasteWise.WasteCollectionLogs.Handler.InvalidInputException;
import com.WasteWise.WasteCollectionLogs.Handler.LogAlreadyCompletedException;
import com.WasteWise.WasteCollectionLogs.Handler.ResourceNotFoundException;
import com.WasteWise.WasteCollectionLogs.ServiceImpl.WasteLogServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault; 
import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


/**
 * REST Controller for managing waste collection logs.
 * This controller handles API endpoints related to starting, ending, and generating reports
 * for waste collection activities within the WasteWise application.
 * <p>
 * It leverages Spring's `@RestController` for building RESTful web services and
 * `@RequestMapping` to define the base path for all endpoints in this controller.
 * Validation for request bodies and path/request parameters is performed using
 * Jakarta Bean Validation (`@Valid`, `@Pattern`) and Spring's `@Validated`.
 * </p>
 */
@RestController
@RequestMapping("wastewise/admin/wastelogs")
@Validated 
public class WasteLogController {

    private final WasteLogServiceImpl wasteLogService;

    /**
     * Constructs a new WasteLogController with the given WasteLogServiceImpl.
     * Spring automatically injects the WasteLogServiceImpl dependency.
     *
     * @param wasteLogService The service responsible for handling waste log business logic.
     */
    public WasteLogController(WasteLogServiceImpl wasteLogService) {
        this.wasteLogService = wasteLogService;
    }

    /**
     * Initiates a new waste collection log.
     * This endpoint accepts a POST request with the details of a new waste collection.
     * The request body is validated automatically by Spring due to the `@Valid` annotation,
     * ensuring that all required fields and formats in {@link WasteLogStartRequestDTO} are met.
     *
     * @param request The {@link WasteLogStartRequestDTO} containing details such as zone ID,
     * vehicle ID, worker ID, and start time.
     * @return A {@link ResponseEntity} containing a {@link RestResponse} with the
     * details of the newly created log and an HTTP status of 201 (Created).
     * @throws InvalidInputException If any business rule validation fails (e.g., invalid ID format,
     * which should ideally be caught by @Pattern, but can also be from service).
     */
    @PostMapping("/start")
    public ResponseEntity<RestResponse<Object>> startCollection(@Valid @RequestBody WasteLogStartRequestDTO request) {
        WasteLogResponseDTO serviceResponse = wasteLogService.startCollection(request); // Service returns raw DTO
        // Build RestResponse in the controller
        RestResponse<Object> restResponse = new RestResponse<>(
            true,
            serviceResponse.getMessage(), // Use message from service DTO
            serviceResponse // Pass the service DTO as data
        );
        return new ResponseEntity<>(restResponse, HttpStatus.CREATED);
    }

    /**
     * Completes an existing waste collection log.
     * This endpoint accepts a PUT request to update an ongoing waste collection log
     * with an end time and collected weight. The request body is validated by `@Valid`.
     *
     * @param request The {@link WasteLogUpdateRequestDTO} containing the log ID to update,
     * the end time of collection, and the weight collected.
     * @return A {@link ResponseEntity} containing a {@link RestResponse} with the
     * updated log details and an HTTP status of 200 (OK).
     * @throws ResourceNotFoundException If the waste log with the given ID is not found.
     * @throws LogAlreadyCompletedException If the waste log has already been marked as completed.
     * @throws InvalidInputException If the provided end time is before the start time, or weight is invalid.
     */
    @PutMapping("/end")
    public ResponseEntity<RestResponse<Object>> endCollection(@Valid @RequestBody WasteLogUpdateRequestDTO request) {
        WasteLogResponseDTO serviceResponse = wasteLogService.endCollection(request); // Service returns raw DTO
        // Build RestResponse in the controller
        RestResponse<Object> restResponse = new RestResponse<>(
            true,
            serviceResponse.getMessage(), // Use message from service DTO
            serviceResponse // Pass the service DTO as data
        );
        return ResponseEntity.ok(restResponse);
    }
    /**
     * Retrieves a daily summary report for a specific waste collection zone within a given date range.
     * This endpoint handles GET requests to provide aggregated waste collection data for a zone.
     * The `zoneId` path variable is validated using a regular expression defined in {@link WasteLogConstants}.
     * The `startDate` and `endDate` request parameters are parsed by Spring as {@link LocalDate}
     * objects using the ISO date format (YYYY-MM-DD).
     *
     * @param zoneId The unique identifier of the zone (e.g., "Z001"). Must conform to {@link WasteLogConstants#ZONE_ID_REGEX}.
     * @param startDate The start date of the reporting period in YYYY-MM-DD format.
     * @param endDate The end date of the reporting period in YYYY-MM-DD format.
     * @param pageable Pagination information, automatically provided by Spring.
     * Defaults to sorting by `date` ascending (as per business logic).
     * @return A {@link ResponseEntity} containing a {@link RestResponse} with a Page of {@link ZoneReportDTO},
     * each representing a daily summary for the specified zone, and an HTTP status of 200 (OK).
     * An empty page is returned if no logs are found for the given criteria.
     * @throws InvalidInputException If the date range is invalid (e.g., startDate is after endDate).
     * @throws jakarta.validation.ConstraintViolationException If `zoneId` does not match the required pattern.
     * @throws org.springframework.web.method.annotation.MethodArgumentTypeMismatchException If dates are not in correct format.
     */
    @GetMapping("/reports/zone/{zoneId}")
    public ResponseEntity<RestResponse<Page<ZoneReportDTO>>> getZoneLogs( // Return type changed
            @PathVariable @Pattern(regexp = WasteLogConstants.ZONE_ID_REGEX,
                    message = "Invalid Zone ID format. Must be Z### (e.g., Z001).") String zoneId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size=1,sort = "date", direction = Sort.Direction.ASC) Pageable pageable) { // Added Pageable with fixed sort default

        Page<ZoneReportDTO> reportsPage = wasteLogService.getZoneLogs(zoneId, startDate, endDate, pageable); // Pass pageable

        String message = reportsPage.isEmpty() ?
                String.format(WasteLogConstants.NO_COMPLETED_LOGS_FOUND_ZONE, zoneId, startDate.toString(), endDate.toString()) :
                "Zone report generated successfully.";

        RestResponse<Page<ZoneReportDTO>> restResponse = new RestResponse<>(true, message, reportsPage); // Data is now a Page
        return ResponseEntity.ok(restResponse);
    }

    /**
     * Retrieves collection logs for a specific vehicle within a given date range.
     * This endpoint provides detailed waste collection log entries for a particular vehicle.
     * The `vehicleId` path variable is validated using a regular expression from {@link WasteLogConstants}.
     * The `startDate` and `endDate` request parameters are parsed as {@link LocalDate} objects
     * using the ISO date format.
     *
     * @param vehicleId The unique identifier of the vehicle (e.g., "RT001" or "PT001"). Must conform to {@link WasteLogConstants#VEHICLE_ID_REGEX}.
     * @param startDate The start date of the reporting period in YYYY-MM-DD format.
     * @param endDate The end date of the reporting period in YYYY-MM-DD format.
     * @param pageable Pagination information, automatically provided by Spring.
     * Defaults to sorting by `collectionDate` ascending.
     * @return A {@link ResponseEntity} containing a {@link RestResponse} with a Page of {@link VehicleReportDTO},
     * each representing a single collection log entry for the specified vehicle, and an HTTP status of 200 (OK).
     * An empty page is returned if no logs are found.
     * @throws InvalidInputException If the date range is invalid.
     * @throws jakarta.validation.ConstraintViolationException If `vehicleId` does not match the required pattern.
     * @throws org.springframework.web.method.annotation.MethodArgumentTypeMismatchException If dates are not in correct format.
     */
    @GetMapping("/reports/vehicle/{vehicleId}")
    public ResponseEntity<RestResponse<Page<VehicleReportDTO>>> getVehicleLogs(
            @PathVariable @Pattern(regexp = WasteLogConstants.VEHICLE_ID_REGEX,
                    message = "Invalid Vehicle ID format. Must be RT### or PT### (e.g., RT001).") String vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size=1,sort = "collectionDate", direction = Sort.Direction.ASC) Pageable pageable) { 

        Page<VehicleReportDTO> reportsPage = wasteLogService.getVehicleLogs(vehicleId, startDate, endDate, pageable); 

        String message = reportsPage.isEmpty() ?
                String.format(WasteLogConstants.NO_COMPLETED_LOGS_FOUND_VEHICLE, vehicleId, startDate.toString(), endDate.toString()) :
                WasteLogConstants.VEHICLE_REPORT_GENERATED_SUCCESSFULLY;

        RestResponse<Page<VehicleReportDTO>> restResponse = new RestResponse<>(true, message, reportsPage);
        return ResponseEntity.ok(restResponse);
    }
}