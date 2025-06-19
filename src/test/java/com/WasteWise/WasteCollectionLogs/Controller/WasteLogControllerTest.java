package com.WasteWise.WasteCollectionLogs.Controller;

import com.WasteWise.WasteCollectionLogs.Dto.VehicleReportDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogResponseDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogStartRequestDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogUpdateRequestDto; // Correct import for your DTO
import com.WasteWise.WasteCollectionLogs.Dto.ZoneReportDto;
import com.WasteWise.WasteCollectionLogs.Handler.InvalidInputException;
import com.WasteWise.WasteCollectionLogs.Handler.LogAlreadyCompletedException;
import com.WasteWise.WasteCollectionLogs.Handler.ResourceNotFoundException;
import com.WasteWise.WasteCollectionLogs.ServiceImpl.WasteLogServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;


//@WebMvcTest(WasteLogController.class)
@SpringBootTest // ADD THIS
@AutoConfigureMockMvc
@DisplayName("WasteLogController Tests")
class WasteLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WasteLogServiceImpl wasteLogService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // --- POST /wastewise/admin/wastelogs/start ---

    @Test
    @DisplayName("startCollection - Success (201 Created)")
    void startCollection_Success() throws Exception {
        WasteLogStartRequestDto requestDto = new WasteLogStartRequestDto("Z001", "RT001", "W001");
        
        WasteLogResponseDto expectedResponse = new WasteLogResponseDto(
                1L, "Z001", "RT001", "W001", 
                LocalDateTime.now().minusMinutes(5),
                null,
                null,
                "Collection started successfully",
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().minusMinutes(5)
        );

        when(wasteLogService.startCollection(any(WasteLogStartRequestDto.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logId", is(expectedResponse.getLogId().intValue())))
                .andExpect(jsonPath("$.zoneId", is("Z001")))
                .andExpect(jsonPath("$.message", is("Collection started successfully")));

        verify(wasteLogService, times(1)).startCollection(any(WasteLogStartRequestDto.class));
    }

    @Test
    @DisplayName("startCollection - Validation Failure: Invalid Zone ID (400 Bad Request)")
    void startCollection_InvalidZoneId_BadRequest() throws Exception {
        WasteLogStartRequestDto requestDto = new WasteLogStartRequestDto("Z1", "RT001", "W001");

        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(wasteLogService, never()).startCollection(any(WasteLogStartRequestDto.class));
    }

    @Test
    @DisplayName("startCollection - Validation Failure: Blank Vehicle ID (400 Bad Request)")
    void startCollection_BlankVehicleId_BadRequest() throws Exception {
        WasteLogStartRequestDto requestDto = new WasteLogStartRequestDto("Z001", "", "W001");

        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(wasteLogService, never()).startCollection(any(WasteLogStartRequestDto.class));
    }

    @Test
    @DisplayName("startCollection - Validation Failure: Null Worker ID (400 Bad Request)")
    void startCollection_NullWorkerId_BadRequest() throws Exception {
        WasteLogStartRequestDto requestDto = new WasteLogStartRequestDto("Z001", "RT001", null);

        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(wasteLogService, never()).startCollection(any(WasteLogStartRequestDto.class));
    }

//   PUT /wastewise/admin/wastelogs/end` Tests

    @Test
    @DisplayName("endCollection - Success (200 OK)")
    void endCollection_Success() throws Exception {
        // Using your actual WasteLogUpdateRequestDto
        WasteLogUpdateRequestDto requestDto = new WasteLogUpdateRequestDto(1L, 150.5);

        WasteLogResponseDto expectedResponse = new WasteLogResponseDto(
                1L, "Z001", "RT001", "W001",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                150.5,
                "Collection ended successfully",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now()
        );

        when(wasteLogService.endCollection(any(WasteLogUpdateRequestDto.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logId", is(expectedResponse.getLogId().intValue())))
                .andExpect(jsonPath("$.weightCollected", is(expectedResponse.getWeightCollected())))
                .andExpect(jsonPath("$.message", is("Collection ended successfully")));

        verify(wasteLogService, times(1)).endCollection(any(WasteLogUpdateRequestDto.class));
    }

    @Test
    @DisplayName("endCollection - Validation Failure: Null Log ID (400 Bad Request)")
    void endCollection_NullLogId_BadRequest() throws Exception {
        // Using your actual WasteLogUpdateRequestDto. This tests the @NotNull on logId.
        WasteLogUpdateRequestDto requestDto = new WasteLogUpdateRequestDto(null, 100.0);

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists()); // Assert that a message is present

        verify(wasteLogService, never()).endCollection(any(WasteLogUpdateRequestDto.class));
    }

    @Test
    @DisplayName("endCollection - Validation Failure: Non-Positive Weight (400 Bad Request)")
    void endCollection_NonPositiveWeight_BadRequest() throws Exception {
        // Using your actual WasteLogUpdateRequestDto. This tests the @Positive on weightCollected.
        WasteLogUpdateRequestDto requestDto = new WasteLogUpdateRequestDto(1L, 0.0); // 0.0 is not positive

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists()); // Assert that a message is present

        verify(wasteLogService, never()).endCollection(any(WasteLogUpdateRequestDto.class));
    }

    @Test
    @DisplayName("endCollection - Service Throws ResourceNotFoundException (404 Not Found)")
    void endCollection_ResourceNotFound_NotFound() throws Exception {
        WasteLogUpdateRequestDto requestDto = new WasteLogUpdateRequestDto(999L, 200.0);

        when(wasteLogService.endCollection(any(WasteLogUpdateRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Log not found with ID: 999"));

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Log not found with ID: 999")));

        verify(wasteLogService, times(1)).endCollection(any(WasteLogUpdateRequestDto.class));
    }

    @Test
    @DisplayName("endCollection - Service Throws LogAlreadyCompletedException (409 Conflict)")
    void endCollection_LogAlreadyCompleted_Conflict() throws Exception {
        WasteLogUpdateRequestDto requestDto = new WasteLogUpdateRequestDto(1L, 200.0);

        when(wasteLogService.endCollection(any(WasteLogUpdateRequestDto.class)))
                .thenThrow(new LogAlreadyCompletedException("Log 1 is already completed."));

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Log 1 is already completed.")));

        verify(wasteLogService, times(1)).endCollection(any(WasteLogUpdateRequestDto.class));
    }

    @Test
    @DisplayName("endCollection - Service Throws InvalidInputException (400 Bad Request)")
    void endCollection_InvalidInput_BadRequest() throws Exception {
        WasteLogUpdateRequestDto requestDto = new WasteLogUpdateRequestDto(1L, 200.0);

        when(wasteLogService.endCollection(any(WasteLogUpdateRequestDto.class)))
                .thenThrow(new InvalidInputException("End time cannot be before start time."));

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("End time cannot be before start time.")));

        verify(wasteLogService, times(1)).endCollection(any(WasteLogUpdateRequestDto.class));
    }


//   `GET /wastewise/admin/wastelogs/reports/zone/{zoneId}` Tests

    @Test
    @DisplayName("getZoneLogs - Success (200 OK)")
    void getZoneLogs_Success() throws Exception {
        String zoneId = "Z001";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        List<ZoneReportDto> expectedReports = Arrays.asList(
                new ZoneReportDto("Z001", LocalDate.of(2024, 1, 15), 3L, 500.0),
                new ZoneReportDto("Z001", LocalDate.of(2024, 1, 16), 4L, 600.0)
        );

        when(wasteLogService.getZoneLogs(eq(zoneId), eq(startDate), eq(endDate)))
                .thenReturn(expectedReports);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", zoneId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].zoneId", is("Z001")))
                .andExpect(jsonPath("$[0].totalWeightCollectedKg", is(500.0)));

        verify(wasteLogService, times(1)).getZoneLogs(eq(zoneId), eq(startDate), eq(endDate));
    }

    @Test
    @DisplayName("getZoneLogs - Validation Failure: Invalid Zone ID Path Variable (400 Bad Request)")
    void getZoneLogs_InvalidZoneIdPathVariable_BadRequest() throws Exception {
        String invalidZoneId = "Z1";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", invalidZoneId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(wasteLogService, never()).getZoneLogs(anyString(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("getZoneLogs - Missing Start Date (400 Bad Request)")
    void getZoneLogs_MissingStartDate_BadRequest() throws Exception {
        String zoneId = "Z001";
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", zoneId)
                        .param("endDate", endDate.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(wasteLogService, never()).getZoneLogs(anyString(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("getZoneLogs - Malformed Date Format (400 Bad Request)")
    void getZoneLogs_MalformedDateFormat_BadRequest() throws Exception {
        String zoneId = "Z001";
        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", zoneId)
                        .param("startDate", "2024/01/01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(wasteLogService, never()).getZoneLogs(anyString(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("getZoneLogs - Service Throws InvalidInputException (400 Bad Request)")
    void getZoneLogs_InvalidInput_BadRequest() throws Exception {
        String zoneId = "Z001";
        LocalDate startDate = LocalDate.of(2024, 1, 31);
        LocalDate endDate = LocalDate.of(2024, 1, 1);

        when(wasteLogService.getZoneLogs(eq(zoneId), eq(startDate), eq(endDate)))
                .thenThrow(new InvalidInputException("Start date cannot be after end date."));

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", zoneId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Start date cannot be after end date.")));

        verify(wasteLogService, times(1)).getZoneLogs(eq(zoneId), eq(startDate), eq(endDate));
    }


//    GET /wastewise/admin/wastelogs/reports/vehicle/{vehicleId}` Tests

    @Test
    @DisplayName("getVehicleLogs - Success (200 OK)")
    void getVehicleLogs_Success() throws Exception {
        String vehicleId = "RT001";
        LocalDate startDate = LocalDate.of(2024, 2, 1);
        LocalDate endDate = LocalDate.of(2024, 2, 29);

        List<VehicleReportDto> expectedReports = Arrays.asList(
                new VehicleReportDto("RT001", "Z001", 150.0, LocalDate.of(2024, 2, 10)),
                new VehicleReportDto("RT001", "Z002", 100.0, LocalDate.of(2024, 2, 10))
        );

        when(wasteLogService.getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate)))
                .thenReturn(expectedReports);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle/{vehicleId}", vehicleId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].vehicleId", is("RT001")))
                .andExpect(jsonPath("$[0].zoneId", is("Z001")))
                .andExpect(jsonPath("$[0].weightCollected", is(150.0)));

        verify(wasteLogService, times(1)).getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate));
    }

    @Test
    @DisplayName("getVehicleLogs - Validation Failure: Invalid Vehicle ID Path Variable (400 Bad Request)")
    void getVehicleLogs_InvalidVehicleIdPathVariable_BadRequest() throws Exception {
        String invalidVehicleId = "R1";
        LocalDate startDate = LocalDate.of(2024, 2, 1);
        LocalDate endDate = LocalDate.of(2024, 2, 29);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle/{vehicleId}", invalidVehicleId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(wasteLogService, never()).getVehicleLogs(anyString(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("getVehicleLogs - Missing End Date (400 Bad Request)")
    void getVehicleLogs_MissingEndDate_BadRequest() throws Exception {
        String vehicleId = "PT002";
        LocalDate startDate = LocalDate.of(2024, 2, 1);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle/{vehicleId}", vehicleId)
                        .param("startDate", startDate.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(wasteLogService, never()).getVehicleLogs(anyString(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("getVehicleLogs - Malformed Date Format (400 Bad Request)")
    void getVehicleLogs_MalformedDateFormat_BadRequest() throws Exception {
        String vehicleId = "PT002";
        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle/{vehicleId}", vehicleId)
                        .param("startDate", "01-02-2024")
                        .param("endDate", "2024-02-29"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(wasteLogService, never()).getVehicleLogs(anyString(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("getVehicleLogs - Service Throws InvalidInputException (400 Bad Request)")
    void getVehicleLogs_InvalidInput_BadRequest() throws Exception {
        String vehicleId = "RT001";
        LocalDate startDate = LocalDate.of(2024, 2, 29);
        LocalDate endDate = LocalDate.of(2024, 2, 1);

        when(wasteLogService.getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate)))
                .thenThrow(new InvalidInputException("Start date cannot be after end date for vehicle report."));

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle/{vehicleId}", vehicleId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Start date cannot be after end date for vehicle report.")));

        verify(wasteLogService, times(1)).getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate));
    }
}