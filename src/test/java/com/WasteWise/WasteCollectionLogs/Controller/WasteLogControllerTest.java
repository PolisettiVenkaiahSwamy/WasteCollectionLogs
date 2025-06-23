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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.containsString;


@WebMvcTest
@ContextConfiguration(classes = {WasteLogController.class})
@Import(GlobalExceptionHandler.class)
class WasteLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WasteLogServiceImpl wasteLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void startCollection_ShouldReturnCreated_WhenSuccessful() throws Exception {
        WasteLogStartRequestDTO request = new WasteLogStartRequestDTO("Z001", "RT001", "W001");
        WasteLogResponseDTO serviceResponse = new WasteLogResponseDTO(1L, WasteLogConstants.WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY);

        when(wasteLogService.startCollection(any(WasteLogStartRequestDTO.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is(WasteLogConstants.WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY)))
                .andExpect(jsonPath("$.data.logId", is(1)));

        verify(wasteLogService, times(1)).startCollection(any(WasteLogStartRequestDTO.class));
    }

    @Test
    void startCollection_ShouldReturnBadRequest_WhenInvalidInput() throws Exception {
        WasteLogStartRequestDTO request = new WasteLogStartRequestDTO(null, "RT001", "W001");

        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message").exists());

        verify(wasteLogService, never()).startCollection(any(WasteLogStartRequestDTO.class));
    }

    @Test
    void startCollection_ShouldReturnBadRequest_WhenActiveLogExists() throws Exception {
        WasteLogStartRequestDTO request = new WasteLogStartRequestDTO("Z001", "RT001", "W001");

        when(wasteLogService.startCollection(any(WasteLogStartRequestDTO.class)))
                .thenThrow(new InvalidInputException(String.format(WasteLogConstants.ACTIVE_LOG_EXISTS_MESSAGE, "W001", "Z001", "RT001")));

        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is(String.format(WasteLogConstants.ACTIVE_LOG_EXISTS_MESSAGE, "W001", "Z001", "RT001"))));

        verify(wasteLogService, times(1)).startCollection(any(WasteLogStartRequestDTO.class));
    }

    @Test
    void endCollection_ShouldReturnOk_WhenSuccessful() throws Exception {
        WasteLogUpdateRequestDTO request = new WasteLogUpdateRequestDTO(1L, 500.50);
        WasteLogResponseDTO serviceResponse = new WasteLogResponseDTO(1L, WasteLogConstants.WASTE_COLLECTION_LOG_COMPLETED_SUCCESSFULLY);

        when(wasteLogService.endCollection(any(WasteLogUpdateRequestDTO.class))).thenReturn(serviceResponse);

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is(WasteLogConstants.WASTE_COLLECTION_LOG_COMPLETED_SUCCESSFULLY)))
                .andExpect(jsonPath("$.data.logId", is(1)));

        verify(wasteLogService, times(1)).endCollection(any(WasteLogUpdateRequestDTO.class));
    }

    @Test
    void endCollection_ShouldReturnBadRequest_WhenInvalidWeight() throws Exception {
        WasteLogUpdateRequestDTO request = new WasteLogUpdateRequestDTO(1L, -100.0);

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message").exists());

        verify(wasteLogService, never()).endCollection(any(WasteLogUpdateRequestDTO.class));
    }

    @Test
    void endCollection_ShouldReturnNotFound_WhenLogNotFound() throws Exception {
        WasteLogUpdateRequestDTO request = new WasteLogUpdateRequestDTO(999L, 500.0);

        when(wasteLogService.endCollection(any(WasteLogUpdateRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException(String.format(WasteLogConstants.WASTE_LOG_NOT_FOUND_MESSAGE, 999L)));

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is(String.format(WasteLogConstants.WASTE_LOG_NOT_FOUND_MESSAGE, 999L))));

        verify(wasteLogService, times(1)).endCollection(any(WasteLogUpdateRequestDTO.class));
    }

    @Test
    void endCollection_ShouldReturnConflict_WhenLogAlreadyCompleted() throws Exception {
        WasteLogUpdateRequestDTO request = new WasteLogUpdateRequestDTO(1L, 500.0);

        when(wasteLogService.endCollection(any(WasteLogUpdateRequestDTO.class)))
                .thenThrow(new LogAlreadyCompletedException(String.format(WasteLogConstants.LOG_ALREADY_COMPLETED_MESSAGE, 1L)));

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is(String.format(WasteLogConstants.LOG_ALREADY_COMPLETED_MESSAGE, 1L))));

        verify(wasteLogService, times(1)).endCollection(any(WasteLogUpdateRequestDTO.class));
    }

    @Test
    void endCollection_ShouldReturnBadRequest_WhenEndTimeBeforeStartTime() throws Exception {
        WasteLogUpdateRequestDTO request = new WasteLogUpdateRequestDTO(1L, 500.0);

        when(wasteLogService.endCollection(any(WasteLogUpdateRequestDTO.class)))
                .thenThrow(new InvalidInputException(WasteLogConstants.COLLECTION_END_TIME_BEFORE_START_TIME));

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is(WasteLogConstants.COLLECTION_END_TIME_BEFORE_START_TIME)));

        verify(wasteLogService, times(1)).endCollection(any(WasteLogUpdateRequestDTO.class));
    }

    @Test
    void getZoneLogs_ShouldReturnOk_WhenReportsExist() throws Exception {
        String zoneId = "Z001";
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 3);
        Pageable pageable = PageRequest.of(0, 1, Sort.by("date").ascending());

        List<ZoneReportDTO> reportList = List.of(
                new ZoneReportDTO("Z001", LocalDate.of(2023, 1, 1), 2L, 1000.0),
                new ZoneReportDTO("Z001", LocalDate.of(2023, 1, 2), 3L, 1500.0)
        );
        PageImpl<ZoneReportDTO> reportsPage = new PageImpl<>(reportList, pageable, reportList.size());

        when(wasteLogService.getZoneLogs(eq(zoneId), eq(startDate), eq(endDate), any(Pageable.class)))
                .thenReturn(reportsPage);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone")
                .param("zoneId", zoneId)
                .param("startDate", "2023-01-01")
                .param("endDate", "2023-01-03")
                .param("page", "0")
                .param("size", "1")
                .param("sort", "date,ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Zone report generated successfully.")))
                .andExpect(jsonPath("$.data.content[0].zoneId", is("Z001")))
                .andExpect(jsonPath("$.data.content[0].date", is("2023-01-01")))
                .andExpect(jsonPath("$.data.content[1].totalWeightCollectedKg", is(1500.0)))
                .andExpect(jsonPath("$.data.totalElements", is(2)));

        verify(wasteLogService, times(1)).getZoneLogs(eq(zoneId), eq(startDate), eq(endDate), any(Pageable.class));
    }

    @Test
    void getZoneLogs_ShouldReturnOk_WhenNoReportsExist() throws Exception {
        String zoneId = "Z999";
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 3);
        Pageable pageable = PageRequest.of(0, 1, Sort.by("date").ascending());

        PageImpl<ZoneReportDTO> reportsPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(wasteLogService.getZoneLogs(eq(zoneId), eq(startDate), eq(endDate), any(Pageable.class)))
                .thenReturn(reportsPage);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone")
                .param("zoneId", zoneId)
                .param("startDate", "2023-01-01")
                .param("endDate", "2023-01-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is(String.format(WasteLogConstants.NO_COMPLETED_LOGS_FOUND_ZONE, zoneId, startDate, endDate))))
                .andExpect(jsonPath("$.data.content", is(empty())))
                .andExpect(jsonPath("$.data.totalElements", is(0)));

        verify(wasteLogService, times(1)).getZoneLogs(eq(zoneId), eq(startDate), eq(endDate), any(Pageable.class));
    }

    @Test
    void getZoneLogs_ShouldReturnBadRequest_WhenInvalidZoneIdFormat() throws Exception {
        String zoneId = "Z00";
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 3);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone")
                .param("zoneId", zoneId)
                .param("startDate", "2023-01-01")
                .param("endDate", "2023-01-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is(containsString("Invalid Zone ID format."))));

        verify(wasteLogService, never()).getZoneLogs(anyString(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void getZoneLogs_ShouldReturnBadRequest_WhenMissingStartDate() throws Exception {
        String zoneId = "Z001";
        LocalDate endDate = LocalDate.of(2023, 1, 3);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone")
                .param("zoneId", zoneId)
                .param("endDate", "2023-01-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Required request parameter 'startDate' is not present.")));

        verify(wasteLogService, never()).getZoneLogs(anyString(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void getZoneLogs_ShouldReturnBadRequest_WhenInvalidDateFormat() throws Exception {
        String zoneId = "Z001";
        String invalidStartDate = "2023/01/01";
        LocalDate endDate = LocalDate.of(2023, 1, 3);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone")
                .param("zoneId", zoneId)
                .param("startDate", invalidStartDate)
                .param("endDate", "2023-01-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is(containsString("Parameter 'startDate' has invalid value"))));

        verify(wasteLogService, never()).getZoneLogs(anyString(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void getZoneLogs_ShouldReturnBadRequest_WhenInvalidDateRangeFromService() throws Exception {
        String zoneId = "Z001";
        LocalDate startDate = LocalDate.of(2023, 1, 5);
        LocalDate endDate = LocalDate.of(2023, 1, 1);

        when(wasteLogService.getZoneLogs(eq(zoneId), eq(startDate), eq(endDate), any(Pageable.class)))
                .thenThrow(new InvalidInputException(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE));

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone")
                .param("zoneId", zoneId)
                .param("startDate", "2023-01-05")
                .param("endDate", "2023-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE)));

        verify(wasteLogService, times(1)).getZoneLogs(eq(zoneId), eq(startDate), eq(endDate), any(Pageable.class));
    }

    @Test
    void getVehicleLogs_ShouldReturnOk_WhenReportsExist() throws Exception {
        String vehicleId = "RT001";
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 3);
        Pageable pageable = PageRequest.of(0, 1, Sort.by("collectionDate").ascending());

        List<VehicleReportDTO> reportList = List.of(
                new VehicleReportDTO("RT001", "Z001", 300.0, LocalDate.of(2023, 1, 1)),
                new VehicleReportDTO("RT001", "Z002", 450.0, LocalDate.of(2023, 1, 2))
        );
        PageImpl<VehicleReportDTO> reportsPage = new PageImpl<>(reportList, pageable, reportList.size());

        when(wasteLogService.getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate), any(Pageable.class)))
                .thenReturn(reportsPage);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle")
                .param("vehicleId", vehicleId)
                .param("startDate", "2023-01-01")
                .param("endDate", "2023-01-03")
                .param("page", "0")
                .param("size", "1")
                .param("sort", "collectionDate,ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is(WasteLogConstants.VEHICLE_REPORT_GENERATED_SUCCESSFULLY)))
                .andExpect(jsonPath("$.data.content[0].vehicleId", is("RT001")))
                .andExpect(jsonPath("$.data.content[0].collectionDate", is("2023-01-01")))
                .andExpect(jsonPath("$.data.content[1].weightCollected", is(450.0)))
                .andExpect(jsonPath("$.data.totalElements", is(2)));

        verify(wasteLogService, times(1)).getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate), any(Pageable.class));
    }

    @Test
    void getVehicleLogs_ShouldReturnOk_WhenNoReportsExist() throws Exception {
        String vehicleId = "RT999";
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 3);
        Pageable pageable = PageRequest.of(0, 1, Sort.by("collectionDate").ascending());

        PageImpl<VehicleReportDTO> reportsPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(wasteLogService.getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate), any(Pageable.class)))
                .thenReturn(reportsPage);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle")
                .param("vehicleId", vehicleId)
                .param("startDate", "2023-01-01")
                .param("endDate", "2023-01-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is(String.format(WasteLogConstants.NO_COMPLETED_LOGS_FOUND_VEHICLE, vehicleId, startDate, endDate))))
                .andExpect(jsonPath("$.data.content", is(empty())))
                .andExpect(jsonPath("$.data.totalElements", is(0)));

        verify(wasteLogService, times(1)).getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate), any(Pageable.class));
    }

    @Test
    void getVehicleLogs_ShouldReturnBadRequest_WhenInvalidVehicleIdFormat() throws Exception {
        String vehicleId = "R001";
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 3);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle")
                .param("vehicleId", vehicleId)
                .param("startDate", "2023-01-01")
                .param("endDate", "2023-01-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is(containsString("Invalid Vehicle ID format."))));

        verify(wasteLogService, never()).getVehicleLogs(anyString(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void getVehicleLogs_ShouldReturnBadRequest_WhenMissingEndDate() throws Exception {
        String vehicleId = "RT001";
        LocalDate startDate = LocalDate.of(2023, 1, 1);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle")
                .param("vehicleId", vehicleId)
                .param("startDate", "2023-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Required request parameter 'endDate' is not present.")));

        verify(wasteLogService, never()).getVehicleLogs(anyString(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void getVehicleLogs_ShouldReturnBadRequest_WhenInvalidDateFormat() throws Exception {
        String vehicleId = "RT001";
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        String invalidEndDate = "01/01/2023";

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle")
                .param("vehicleId", vehicleId)
                .param("startDate", "2023-01-01")
                .param("endDate", invalidEndDate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is(containsString("Parameter 'endDate' has invalid value"))));

        verify(wasteLogService, never()).getVehicleLogs(anyString(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
    }

    @Test
    void getVehicleLogs_ShouldReturnBadRequest_WhenInvalidDateRangeFromService() throws Exception {
        String vehicleId = "RT001";
        LocalDate startDate = LocalDate.of(2023, 1, 5);
        LocalDate endDate = LocalDate.of(2023, 1, 1);

        when(wasteLogService.getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate), any(Pageable.class)))
                .thenThrow(new InvalidInputException(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE));

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle")
                .param("vehicleId", vehicleId)
                .param("startDate", "2023-01-05")
                .param("endDate", "2023-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE)));

        verify(wasteLogService, times(1)).getVehicleLogs(eq(vehicleId), eq(startDate), eq(endDate), any(Pageable.class));
    }
}