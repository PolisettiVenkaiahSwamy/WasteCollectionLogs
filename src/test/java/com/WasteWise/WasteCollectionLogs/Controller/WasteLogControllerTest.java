package com.WasteWise.WasteCollectionLogs.Controller;

import com.WasteWise.WasteCollectionLogs.Constants.WasteLogConstants;
import com.WasteWise.WasteCollectionLogs.Dto.VehicleReportDTO;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogResponseDTO;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogStartRequestDTO;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogUpdateRequestDTO;
import com.WasteWise.WasteCollectionLogs.Dto.ZoneReportDTO;
import com.WasteWise.WasteCollectionLogs.Handler.GlobalExceptionHandler;
import com.WasteWise.WasteCollectionLogs.Handler.InvalidInputException;
import com.WasteWise.WasteCollectionLogs.Handler.LogAlreadyCompletedException;
import com.WasteWise.WasteCollectionLogs.Handler.ResourceNotFoundException;
import com.WasteWise.WasteCollectionLogs.ServiceImpl.WasteLogServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // For LocalDateTime serialization/deserialization

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyString; 
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

// @WebMvcTest annotation focuses on Spring MVC components and wires up MockMvc
// It will auto-configure MockMvc and automatically scan for @Controller, @ControllerAdvice, etc.
@WebMvcTest(WasteLogController.class)
@ContextConfiguration(classes = {WasteLogController.class, GlobalExceptionHandler.class})
@Import(GlobalExceptionHandler.class)
class WasteLogControllerTest {

    @Autowired
    private MockMvc mockMvc; 

    @MockitoBean 
    private WasteLogServiceImpl wasteLogService;

    private ObjectMapper objectMapper; // To convert objects to JSON and vice-versa

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Register module for Java 8 Date/Time types
    }

    // --- 1. POST /wastewise/admin/wastelogs/start ---

    @Test
    @DisplayName("shouldReturnCreatedStatusAndSuccessResponseForStartCollection")
    void shouldReturnCreatedStatusAndSuccessResponseForStartCollection() throws Exception {
        // Given
        WasteLogStartRequestDTO requestDto = new WasteLogStartRequestDTO("Z001", "RT001", "W001");
        WasteLogResponseDTO serviceResponseDto = new WasteLogResponseDTO(1L, WasteLogConstants.WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY);

        when(wasteLogService.startCollection(any(WasteLogStartRequestDTO.class))).thenReturn(serviceResponseDto);

        // When & Then
        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated()) // HTTP 201
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is(WasteLogConstants.WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY)))
                .andExpect(jsonPath("$.data.logId", is(1)))
                .andExpect(jsonPath("$.data.message", is(WasteLogConstants.WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService).startCollection(any(WasteLogStartRequestDTO.class));
    }

    @Test
    @DisplayName("shouldReturnBadRequestWhenStartCollectionRequestIsInvalid_MissingZoneId")
    void shouldReturnBadRequestWhenStartCollectionRequestIsInvalid_MissingZoneId() throws Exception {
        // Given
        // Invalid request: zoneId is blank
        WasteLogStartRequestDTO requestDto = new WasteLogStartRequestDTO("", "RT001", "W001");

        // When & Then
        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest()) // HTTP 400 from MethodArgumentNotValidException
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", containsString("Zone ID cannot be empty.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService, never()).startCollection(any(WasteLogStartRequestDTO.class));
    }

    @Test
    @DisplayName("shouldReturnBadRequestWhenStartCollectionRequestIsInvalid_InvalidWorkerIdFormat")
    void shouldReturnBadRequestWhenStartCollectionRequestIsInvalid_InvalidWorkerIdFormat() throws Exception {
        // Given
        // Invalid request: workerId format is wrong
        WasteLogStartRequestDTO requestDto = new WasteLogStartRequestDTO("Z001", "RT001", "Invalid");

        // When & Then
        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest()) // HTTP 400 from MethodArgumentNotValidException
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", containsString("workerId: Invalid Worker ID format. Must be like W456.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService, never()).startCollection(any(WasteLogStartRequestDTO.class));
    }

    @Test
    @DisplayName("shouldReturnBadRequestWhenActiveLogExistsOnStartCollection")
    void shouldReturnBadRequestWhenActiveLogExistsOnStartCollection() throws Exception {
        // Given
        WasteLogStartRequestDTO requestDto = new WasteLogStartRequestDTO("Z001", "RT001", "W001");
        String expectedErrorMessage = String.format(WasteLogConstants.ACTIVE_LOG_EXISTS_MESSAGE, "W001", "Z001", "RT001");
        when(wasteLogService.startCollection(any(WasteLogStartRequestDTO.class)))
                .thenThrow(new InvalidInputException(expectedErrorMessage));

        // When & Then
        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest()) // HTTP 400 due to InvalidInputException
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", is(expectedErrorMessage)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService).startCollection(any(WasteLogStartRequestDTO.class));
    }


    // --- 2. PUT /wastewise/admin/wastelogs/end ---

    @Test
    @DisplayName("shouldReturnOkStatusAndSuccessResponseForEndCollection")
    void shouldReturnOkStatusAndSuccessResponseForEndCollection() throws Exception {
        // Given
        WasteLogUpdateRequestDTO requestDto = new WasteLogUpdateRequestDTO(1L, 150.0);
        WasteLogResponseDTO serviceResponseDto = new WasteLogResponseDTO(1L, WasteLogConstants.WASTE_COLLECTION_LOG_COMPLETED_SUCCESSFULLY);

        when(wasteLogService.endCollection(any(WasteLogUpdateRequestDTO.class))).thenReturn(serviceResponseDto);

        // When & Then
        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is(WasteLogConstants.WASTE_COLLECTION_LOG_COMPLETED_SUCCESSFULLY)))
                .andExpect(jsonPath("$.data.logId", is(1)))
                .andExpect(jsonPath("$.data.message", is(WasteLogConstants.WASTE_COLLECTION_LOG_COMPLETED_SUCCESSFULLY)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService).endCollection(any(WasteLogUpdateRequestDTO.class));
    }

    @Test
    @DisplayName("shouldReturnBadRequestWhenEndCollectionRequestIsInvalid_NegativeWeight")
    void shouldReturnBadRequestWhenEndCollectionRequestIsInvalid_NegativeWeight() throws Exception {
        // Given
        // Invalid request: weightCollected is negative
        WasteLogUpdateRequestDTO requestDto = new WasteLogUpdateRequestDTO(1L, -10.0);

        // When & Then
        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest()) // HTTP 400 from MethodArgumentNotValidException
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", containsString("weightCollected: Weight Collected must be a positive value.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService, never()).endCollection(any(WasteLogUpdateRequestDTO.class));
    }

    @Test
    @DisplayName("shouldReturnNotFoundForEndCollectionWhenLogNotFound")
    void shouldReturnNotFoundForEndCollectionWhenLogNotFound() throws Exception {
        // Given
        WasteLogUpdateRequestDTO requestDto = new WasteLogUpdateRequestDTO(999L, 100.0);
        String expectedErrorMessage = String.format(WasteLogConstants.WASTE_LOG_NOT_FOUND_MESSAGE, 999L);
        doThrow(new ResourceNotFoundException(expectedErrorMessage))
                .when(wasteLogService).endCollection(any(WasteLogUpdateRequestDTO.class));

        // When & Then
        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound()) // HTTP 404 due to ResourceNotFoundException
                .andExpect(jsonPath("$.status", is(HttpStatus.NOT_FOUND.value())))
                .andExpect(jsonPath("$.message", is(expectedErrorMessage)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService).endCollection(any(WasteLogUpdateRequestDTO.class));
    }

    @Test
    @DisplayName("shouldReturnConflictForEndCollectionWhenLogAlreadyCompleted")
    void shouldReturnConflictForEndCollectionWhenLogAlreadyCompleted() throws Exception {
        // Given
        WasteLogUpdateRequestDTO requestDto = new WasteLogUpdateRequestDTO(1L, 100.0);
        String expectedErrorMessage = String.format(WasteLogConstants.LOG_ALREADY_COMPLETED_MESSAGE, 1L);
        doThrow(new LogAlreadyCompletedException(expectedErrorMessage))
                .when(wasteLogService).endCollection(any(WasteLogUpdateRequestDTO.class));

        // When & Then
        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict()) // HTTP 409 due to LogAlreadyCompletedException
                .andExpect(jsonPath("$.status", is(HttpStatus.CONFLICT.value())))
                .andExpect(jsonPath("$.message", is(expectedErrorMessage)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService).endCollection(any(WasteLogUpdateRequestDTO.class));
    }

    @Test
    @DisplayName("shouldReturnBadRequestForEndCollectionWhenInvalidInputFromService")
    void shouldReturnBadRequestForEndCollectionWhenInvalidInputFromService() throws Exception {
        // Given
        WasteLogUpdateRequestDTO requestDto = new WasteLogUpdateRequestDTO(1L, 100.0);
        String expectedErrorMessage = WasteLogConstants.COLLECTION_END_TIME_BEFORE_START_TIME;
        doThrow(new InvalidInputException(expectedErrorMessage))
                .when(wasteLogService).endCollection(any(WasteLogUpdateRequestDTO.class));

        // When & Then
        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest()) // HTTP 400 due to InvalidInputException
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", is(expectedErrorMessage)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService).endCollection(any(WasteLogUpdateRequestDTO.class));
    }


    // --- 3. GET /wastewise/admin/wastelogs/reports/zone/{zoneId} ---

    @Test
    @DisplayName("shouldReturnOkStatusAndZoneReports")
    void shouldReturnOkStatusAndZoneReports() throws Exception {
        // Given
        String zoneId = "Z001";
        LocalDate startDate = LocalDate.of(2025, 6, 1);
        LocalDate endDate = LocalDate.of(2025, 6, 3);
        List<ZoneReportDTO> mockReports = Arrays.asList(
                new ZoneReportDTO("Z001", LocalDate.of(2025, 6, 1), 2L, 150.0),
                new ZoneReportDTO("Z001", LocalDate.of(2025, 6, 2), 1L, 100.0)
        );

        when(wasteLogService.getZoneLogs(eq(zoneId), eq(startDate), eq(endDate)))
                .thenReturn(mockReports);

        // When & Then
        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", zoneId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Zone report generated successfully."))) // Message set in controller based on list content
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].zoneId", is("Z001")))
                .andExpect(jsonPath("$.data[0].date", is("2025-06-01")))
                .andExpect(jsonPath("$.data[0].totalNumberOfCollections", is(2)))
                .andExpect(jsonPath("$.data[0].totalWeightCollectedKg", is(150.0)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService).getZoneLogs(eq(zoneId), eq(startDate), eq(endDate));
    }

    @Test
    @DisplayName("shouldReturnOkStatusAndEmptyZoneReportsWhenNoneFound")
    void shouldReturnOkStatusAndEmptyZoneReportsWhenNoneFound() throws Exception {
        // Given
        String zoneId = "Z002";
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        List<ZoneReportDTO> emptyReports = Collections.emptyList();

        when(wasteLogService.getZoneLogs(eq(zoneId), eq(startDate), eq(endDate)))
                .thenReturn(emptyReports);

        String expectedMessage = String.format(WasteLogConstants.NO_COMPLETED_LOGS_FOUND_ZONE, zoneId, startDate.toString(), endDate.toString());

        // When & Then
        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", zoneId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is(expectedMessage)))
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService).getZoneLogs(eq(zoneId), eq(startDate), eq(endDate));
    }

    @Test
    @DisplayName("shouldReturnBadRequestWhenInvalidZoneIdFormat")
    void shouldReturnBadRequestWhenInvalidZoneIdFormat() throws Exception {
        // Given
        String invalidZoneId = "Z-1"; // Invalid format
        LocalDate startDate = LocalDate.of(2025, 6, 1);
        LocalDate endDate = LocalDate.of(2025, 6, 3);

        // When & Then
        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", invalidZoneId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()) // HTTP 400 from ConstraintViolationException
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", containsString("Invalid Zone ID format. Must be Z### (e.g., Z001).")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService, never()).getZoneLogs(anyString(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("shouldReturnBadRequestWhenZoneReportDatesAreInvalid_StartDateAfterEndDate")
    void shouldReturnBadRequestWhenZoneReportDatesAreInvalid_StartDateAfterEndDate() throws Exception {
        // Given
        String zoneId = "Z001";
        LocalDate startDate = LocalDate.of(2025, 6, 20);
        LocalDate endDate = LocalDate.of(2025, 6, 18); // Invalid date range

        // Mock the service to throw InvalidInputException for this specific case
        when(wasteLogService.getZoneLogs(eq(zoneId), eq(startDate), eq(endDate)))
                .thenThrow(new InvalidInputException(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE));

        // When & Then
        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", zoneId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()) // HTTP 400 due to InvalidInputException
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", is(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService).getZoneLogs(eq(zoneId), eq(startDate), eq(endDate));
    }

    @Test
    @DisplayName("shouldReturnBadRequestWhenZoneReportDatesAreInvalid_MalformedDate")
    void shouldReturnBadRequestWhenZoneReportDatesAreInvalid_MalformedDate() throws Exception {
        // Given
        String zoneId = "Z001";
        String malformedDate = "2025-06/01"; // Invalid format
        LocalDate endDate = LocalDate.of(2025, 6, 3);

        // When & Then
        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", zoneId)
                        .param("startDate", malformedDate)
                        .param("endDate", endDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()) // HTTP 400 from MethodArgumentTypeMismatchException
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", containsString("Parameter 'startDate' has invalid value '2025-06/01'. Expected type is 'LocalDate'.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService, never()).getZoneLogs(anyString(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("shouldReturnBadRequestWhenZoneReportDatesAreMissing")
    void shouldReturnBadRequestWhenZoneReportDatesAreMissing() throws Exception {
        // Given
        String zoneId = "Z001";
        // Missing startDate and endDate parameters

        // When & Then
        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", zoneId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()) // HTTP 400 from MissingServletRequestParameterException
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", containsString("Required request parameter 'startDate' is not present.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService, never()).getZoneLogs(anyString(), any(LocalDate.class), any(LocalDate.class));
    }


    // --- 4. GET /wastewise/admin/wastelogs/reports/vehicle/{vehicleId} ---

    @Test
    @DisplayName("shouldReturnOkStatusAndVehicleReports")
    void shouldReturnOkStatusAndVehicleReports() throws Exception {
        // Given
        String vehicleId = "RT001";
        LocalDate startDate = LocalDate.of(2025, 6, 1);
        LocalDate endDate = LocalDate.of(2025, 6, 3);
        List<VehicleReportDTO> mockReports = Arrays.asList(
                new VehicleReportDTO("RT001", "Z001", 100.0, LocalDate.of(2025, 6, 1)),
                new VehicleReportDTO("RT001", "Z002", 120.0, LocalDate.of(2025, 6, 2))
        );

        when(wasteLogService.getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate)))
                .thenReturn(mockReports);

        // When & Then
        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle/{vehicleId}", vehicleId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is(WasteLogConstants.VEHICLE_REPORT_GENERATED_SUCCESSFULLY)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].vehicleId", is("RT001")))
                .andExpect(jsonPath("$.data[0].collectionDate", is("2025-06-01")))
                .andExpect(jsonPath("$.data[0].weightCollected", is(100.0)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService).getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate));
    }

    @Test
    @DisplayName("shouldReturnOkStatusAndEmptyVehicleReportsWhenNoneFound")
    void shouldReturnOkStatusAndEmptyVehicleReportsWhenNoneFound() throws Exception {
        // Given
        String vehicleId = "RT005";
        LocalDate startDate = LocalDate.of(2025, 3, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        List<VehicleReportDTO> emptyReports = Collections.emptyList();

        when(wasteLogService.getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate)))
                .thenReturn(emptyReports);

        String expectedMessage = String.format(WasteLogConstants.NO_COMPLETED_LOGS_FOUND_VEHICLE, vehicleId, startDate.toString(), endDate.toString());

        // When & Then
        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle/{vehicleId}", vehicleId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is(expectedMessage)))
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService).getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate));
    }

    @Test
    @DisplayName("shouldReturnBadRequestWhenInvalidVehicleIdFormat")
    void shouldReturnBadRequestWhenInvalidVehicleIdFormat() throws Exception {
        // Given
        String invalidVehicleId = "ABC-123"; // Invalid format
        LocalDate startDate = LocalDate.of(2025, 6, 1);
        LocalDate endDate = LocalDate.of(2025, 6, 3);

        // When & Then
        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle/{vehicleId}", invalidVehicleId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()) // HTTP 400 from ConstraintViolationException
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", containsString("Invalid Vehicle ID format. Must be RT### or PT### (e.g., RT001).")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService, never()).getVehicleLogs(anyString(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("shouldReturnBadRequestWhenVehicleReportDatesAreInvalid_StartDateAfterEndDate")
    void shouldReturnBadRequestWhenVehicleReportDatesAreInvalid_StartDateAfterEndDate() throws Exception {
        // Given
        String vehicleId = "RT001";
        LocalDate startDate = LocalDate.of(2025, 6, 20);
        LocalDate endDate = LocalDate.of(2025, 6, 18); // Invalid date range

        // Mock the service to throw InvalidInputException for this specific case
        when(wasteLogService.getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate)))
                .thenThrow(new InvalidInputException(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE));

        // When & Then
        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle/{vehicleId}", vehicleId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()) // HTTP 400 due to InvalidInputException
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", is(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(wasteLogService).getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate));
    }
}