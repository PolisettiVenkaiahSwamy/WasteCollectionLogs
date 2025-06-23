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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = {WasteLogController.class})
@Import(GlobalExceptionHandler.class)
class WasteLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WasteLogServiceImpl wasteLogService;

    @Autowired
    private ObjectMapper objectMapper;

    private WasteLogStartRequestDTO startRequestDTO;
    private WasteLogUpdateRequestDTO updateRequestDTO;
    private WasteLogResponseDTO successResponseDTO;
    private Pageable pageableForZone;
    private Pageable pageableForVehicle;

    @BeforeEach
    void setUp() {
        startRequestDTO = new WasteLogStartRequestDTO("Z001", "RT001", "W001");
        updateRequestDTO = new WasteLogUpdateRequestDTO(123L, 150.0);
        successResponseDTO = new WasteLogResponseDTO(123L, WasteLogConstants.WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY);
        
        pageableForZone = PageRequest.of(0, 1, org.springframework.data.domain.Sort.by("date").ascending());
        pageableForVehicle = PageRequest.of(0, 1, org.springframework.data.domain.Sort.by("collectionDate").ascending());
    }

    // --- startCollection Tests ---

    @Test
    void startCollection_Success() throws Exception {
        when(wasteLogService.startCollection(any(WasteLogStartRequestDTO.class))).thenReturn(successResponseDTO);

        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(startRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(WasteLogConstants.WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY))
                .andExpect(jsonPath("$.data.logId").value(123));
    }

    @Test
    void startCollection_InvalidInput_MissingFields() throws Exception {
        WasteLogStartRequestDTO invalidRequest = new WasteLogStartRequestDTO(null, "RT001", "W001"); // Missing zoneId

        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                // Updated expected message to match handler's output for @RequestBody validation
                .andExpect(jsonPath("$.message").value("Validation failed: zoneId: Zone ID cannot be empty.")) 
                .andExpect(jsonPath("$.timestamp").exists());
    }
    
    @Test
    void startCollection_InvalidInput_InvalidFormat() throws Exception {
        WasteLogStartRequestDTO invalidRequest = new WasteLogStartRequestDTO("Z01", "RT001", "W001"); // Invalid zoneId format

        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                // Updated expected message to match handler's output for @RequestBody validation
                .andExpect(jsonPath("$.message").value("Validation failed: zoneId: Invalid Zone ID format. Must be like Z001."))
                .andExpect(jsonPath("$.timestamp").exists());
    }


    @Test
    void startCollection_BusinessLogicError_ActiveLogExists() throws Exception {
        when(wasteLogService.startCollection(any(WasteLogStartRequestDTO.class)))
                .thenThrow(new InvalidInputException(String.format(WasteLogConstants.ACTIVE_LOG_EXISTS_MESSAGE, "W001", "Z001", "RT001")));

        mockMvc.perform(post("/wastewise/admin/wastelogs/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(startRequestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(String.format(WasteLogConstants.ACTIVE_LOG_EXISTS_MESSAGE, "W001", "Z001", "RT001")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // --- endCollection Tests ---

    @Test
    void endCollection_Success() throws Exception {
        WasteLogResponseDTO endSuccessResponse = new WasteLogResponseDTO(123L, WasteLogConstants.WASTE_COLLECTION_LOG_COMPLETED_SUCCESSFULLY);
        when(wasteLogService.endCollection(any(WasteLogUpdateRequestDTO.class))).thenReturn(endSuccessResponse);

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(WasteLogConstants.WASTE_COLLECTION_LOG_COMPLETED_SUCCESSFULLY))
                .andExpect(jsonPath("$.data.logId").value(123));
    }

    @Test
    void endCollection_InvalidInput_NegativeWeight() throws Exception {
        WasteLogUpdateRequestDTO invalidRequest = new WasteLogUpdateRequestDTO(123L, -10.0); // Negative weight

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                // Updated expected message to match handler's output for @RequestBody validation
                .andExpect(jsonPath("$.message").value("Validation failed: weightCollected: Weight Collected must be a positive value.")) 
                .andExpect(jsonPath("$.timestamp").exists());
    }
    
    @Test
    void endCollection_InvalidInput_NullLogId() throws Exception {
        WasteLogUpdateRequestDTO invalidRequest = new WasteLogUpdateRequestDTO(null, 100.0); 

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                // Updated expected message to match handler's output for @RequestBody validation
                .andExpect(jsonPath("$.message").value("Validation failed: logId: Log ID cannot be null.")) 
                .andExpect(jsonPath("$.timestamp").exists());
    }


    @Test
    void endCollection_ResourceNotFound() throws Exception {
        when(wasteLogService.endCollection(any(WasteLogUpdateRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException(String.format(WasteLogConstants.WASTE_LOG_NOT_FOUND_MESSAGE, 999L)));

        WasteLogUpdateRequestDTO notFoundRequest = new WasteLogUpdateRequestDTO(999L, 100.0);

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(notFoundRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(String.format(WasteLogConstants.WASTE_LOG_NOT_FOUND_MESSAGE, 999L)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void endCollection_LogAlreadyCompleted() throws Exception {
        when(wasteLogService.endCollection(any(WasteLogUpdateRequestDTO.class)))
                .thenThrow(new LogAlreadyCompletedException(String.format(WasteLogConstants.LOG_ALREADY_COMPLETED_MESSAGE, 123L)));

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequestDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(String.format(WasteLogConstants.LOG_ALREADY_COMPLETED_MESSAGE, 123L)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void endCollection_InvalidInput_EndTimeBeforeStartTime() throws Exception {
        when(wasteLogService.endCollection(any(WasteLogUpdateRequestDTO.class)))
                .thenThrow(new InvalidInputException(WasteLogConstants.COLLECTION_END_TIME_BEFORE_START_TIME));

        mockMvc.perform(put("/wastewise/admin/wastelogs/end")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(WasteLogConstants.COLLECTION_END_TIME_BEFORE_START_TIME))
                .andExpect(jsonPath("$.timestamp").exists());
    }


    // --- getZoneLogs Tests ---

    @Test
    void getZoneLogs_Success_WithData() throws Exception {
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 31);
        ZoneReportDTO report1 = new ZoneReportDTO("Z001", LocalDate.of(2023, 1, 10), 2L, 200.0);
        ZoneReportDTO report2 = new ZoneReportDTO("Z001", LocalDate.of(2023, 1, 15), 1L, 150.0);
        List<ZoneReportDTO> reportsList = List.of(report1, report2);
        Page<ZoneReportDTO> reportsPage = new PageImpl<>(reportsList, pageableForZone, reportsList.size());

        when(wasteLogService.getZoneLogs(any(String.class), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(reportsPage);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", "Z001")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("page", "0")
                .param("size", "1")
                .param("sort", "date,asc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Zone report generated successfully."))
                .andExpect(jsonPath("$.data.content[0].zoneId").value("Z001"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void getZoneLogs_Success_NoData() throws Exception {
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 31);
        Page<ZoneReportDTO> emptyPage = new PageImpl<>(Collections.emptyList(), pageableForZone, 0);

        when(wasteLogService.getZoneLogs(any(String.class), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", "Z001")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("page", "0")
                .param("size", "1")
                .param("sort", "date,asc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(String.format(WasteLogConstants.NO_COMPLETED_LOGS_FOUND_ZONE, "Z001", startDate.toString(), endDate.toString())))
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void getZoneLogs_InvalidZoneIdFormat() throws Exception {
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 31);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", "INVALID_ZONE")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("page", "0")
                .param("size", "1")
                .param("sort", "date,asc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                // Updated expected message to match handler's output for @PathVariable validation
                .andExpect(jsonPath("$.message").value("Validation failed: getZoneLogs.zoneId: Invalid Zone ID format. Must be Z### (e.g., Z001)."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getZoneLogs_InvalidDateRange() throws Exception {
        LocalDate startDate = LocalDate.of(2023, 1, 31);
        LocalDate endDate = LocalDate.of(2023, 1, 1); // endDate before startDate

        when(wasteLogService.getZoneLogs(any(String.class), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenThrow(new InvalidInputException(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE));

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", "Z001")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("page", "0")
                .param("size", "1")
                .param("sort", "date,asc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getZoneLogs_InvalidDateFormat() throws Exception {
        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/zone/{zoneId}", "Z001")
                .param("startDate", "2023/01/01") // Invalid format
                .param("endDate", "2023-01-31")
                .param("page", "0")
                .param("size", "1")
                .param("sort", "date,asc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                // Updated expected message to match handler's output for MethodArgumentTypeMismatchException
                .andExpect(jsonPath("$.message").value("Parameter 'startDate' has invalid value '2023/01/01'. Expected type is 'LocalDate'."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // --- getVehicleLogs Tests ---

    @Test
    void getVehicleLogs_Success_WithData() throws Exception {
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 31);
        VehicleReportDTO report1 = new VehicleReportDTO("RT001", "Z001", 100.0, LocalDate.of(2023, 1, 5));
        VehicleReportDTO report2 = new VehicleReportDTO("RT001", "Z002", 120.0, LocalDate.of(2023, 1, 12));
        List<VehicleReportDTO> reportsList = List.of(report1, report2);
        Page<VehicleReportDTO> reportsPage = new PageImpl<>(reportsList, pageableForVehicle, reportsList.size());

        when(wasteLogService.getVehicleLogs(any(String.class), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(reportsPage);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle/{vehicleId}", "RT001")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("page", "0")
                .param("size", "1")
                .param("sort", "collectionDate,asc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(WasteLogConstants.VEHICLE_REPORT_GENERATED_SUCCESSFULLY))
                .andExpect(jsonPath("$.data.content[0].vehicleId").value("RT001"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void getVehicleLogs_Success_NoData() throws Exception {
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 31);
        Page<VehicleReportDTO> emptyPage = new PageImpl<>(Collections.emptyList(), pageableForVehicle, 0);

        when(wasteLogService.getVehicleLogs(any(String.class), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle/{vehicleId}", "RT001")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("page", "0")
                .param("size", "1")
                .param("sort", "collectionDate,asc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(String.format(WasteLogConstants.NO_COMPLETED_LOGS_FOUND_VEHICLE, "RT001", startDate.toString(), endDate.toString())))
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void getVehicleLogs_InvalidVehicleIdFormat() throws Exception {
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 31);

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle/{vehicleId}", "INVALID_VEHICLE")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("page", "0")
                .param("size", "1")
                .param("sort", "collectionDate,asc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed: getVehicleLogs.vehicleId: Invalid Vehicle ID format. Must be RT### or PT### (e.g., RT001)."))
                .andExpect(jsonPath("$.timestamp").exists());
    }


    @Test
    void getVehicleLogs_InvalidDateRange() throws Exception {
        LocalDate startDate = LocalDate.of(2023, 1, 31);
        LocalDate endDate = LocalDate.of(2023, 1, 1); // endDate before startDate

        when(wasteLogService.getVehicleLogs(any(String.class), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenThrow(new InvalidInputException(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE));

        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle/{vehicleId}", "RT001")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("page", "0")
                .param("size", "1")
                .param("sort", "collectionDate,asc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getVehicleLogs_InvalidDateFormat() throws Exception {
        mockMvc.perform(get("/wastewise/admin/wastelogs/reports/vehicle/{vehicleId}", "RT001")
                .param("startDate", "01-01-2023") // Invalid format
                .param("endDate", "2023-01-31")
                .param("page", "0")
                .param("size", "1")
                .param("sort", "collectionDate,asc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                // Updated expected message to match handler's output for MethodArgumentTypeMismatchException
                .andExpect(jsonPath("$.message").value("Parameter 'startDate' has invalid value '01-01-2023'. Expected type is 'LocalDate'."))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}