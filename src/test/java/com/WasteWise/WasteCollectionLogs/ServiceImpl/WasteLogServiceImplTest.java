package com.WasteWise.WasteCollectionLogs.ServiceImpl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.PageRequest; // For creating Pageable
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.data.domain.Sort; // For Pageable sorting

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito annotations for JUnit 5
@DisplayName("WasteLogServiceImpl Unit Tests")
class WasteLogServiceImplTest {

    @Mock // Creates a mock instance of WasteLogRepository
    private WasteLogRepository wasteLogRepository;

    @InjectMocks // Injects the mocks into WasteLogServiceImpl
    private WasteLogServiceImpl wasteLogService;


    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2025, 6, 20, 10, 0, 0);

    @BeforeEach
    void setUp() {
        
    }

    @Test
    @DisplayName("shouldStartCollectionSuccessfully: Valid request with no active log should succeed")
    void shouldStartCollectionSuccessfully() {
        // Given
        WasteLogStartRequestDTO request = new WasteLogStartRequestDTO("Z001", "RT001", "W001");
        WasteLog newLog = new WasteLog();
        newLog.setLogId(1L); // Simulate ID assigned by DB
        newLog.setZoneId("Z001");
        newLog.setVehicleId("RT001");
        newLog.setWorkerId("W001");

        // When
        when(wasteLogRepository.findByWorkerIdAndZoneIdAndVehicleIdAndCollectionEndTimeIsNull(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty()); // No active log exists
        when(wasteLogRepository.save(any(WasteLog.class))).thenReturn(newLog);

        // Use MockedStatic to control LocalDateTime.now() for consistent time-dependent logic
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(FIXED_NOW);

            WasteLogResponseDTO response = wasteLogService.startCollection(request);

            // Then
            assertNotNull(response);
            assertEquals(1L, response.getLogId());
            assertEquals(WasteLogConstants.WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY, response.getMessage());

            // Verify repository calls and the properties of the saved log
            verify(wasteLogRepository, times(1))
                    .findByWorkerIdAndZoneIdAndVehicleIdAndCollectionEndTimeIsNull("W001", "Z001", "RT001");
            verify(wasteLogRepository, times(1)).save(argThat(log ->
                    log.getZoneId().equals("Z001") &&
                            log.getVehicleId().equals("RT001") &&
                            log.getWorkerId().equals("W001") &&
                            log.getCollectionStartTime().equals(FIXED_NOW) &&
                            log.getCreatedDate().equals(FIXED_NOW) &&
                            log.getCollectionEndTime() == null && // Should be null on start
                            log.getWeightCollected() == null      // Should be null on start
            ));
        }
    }

    @Test
    @DisplayName("shouldThrowInvalidInputExceptionWhenActiveLogExists: Active log should prevent new collection")
    void shouldThrowInvalidInputExceptionWhenActiveLogExists() {
        // Given
        WasteLogStartRequestDTO request = new WasteLogStartRequestDTO("Z001", "RT001", "W001");
        // Simplified WasteLog constructor for an active log
        WasteLog activeLog = new WasteLog();
        activeLog.setLogId(1L);
        activeLog.setZoneId("Z001");
        activeLog.setVehicleId("RT001");
        activeLog.setWorkerId("W001");
        activeLog.setCollectionStartTime(LocalDateTime.now().minusHours(1));

        // When
        when(wasteLogRepository.findByWorkerIdAndZoneIdAndVehicleIdAndCollectionEndTimeIsNull(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(activeLog)); // Active log exists

        // Then
        InvalidInputException exception = assertThrows(InvalidInputException.class, () ->
                wasteLogService.startCollection(request)
        );

        String expectedMessage = String.format(WasteLogConstants.ACTIVE_LOG_EXISTS_MESSAGE, "W001", "Z001", "RT001");
        assertEquals(expectedMessage, exception.getMessage());

        // Verify save was NOT called
        verify(wasteLogRepository, times(1))
                .findByWorkerIdAndZoneIdAndVehicleIdAndCollectionEndTimeIsNull("W001", "Z001", "RT001");
        verify(wasteLogRepository, never()).save(any(WasteLog.class));
    }

    // --- 2. endCollection Tests ---

    @Test
    @DisplayName("shouldEndCollectionSuccessfully: Existing, uncompleted log should be updated")
    void shouldEndCollectionSuccessfully() {
        // Given
        WasteLogUpdateRequestDTO request = new WasteLogUpdateRequestDTO(1L, 150.0);
        WasteLog existingLog = new WasteLog(); // Use empty constructor, then set fields
        existingLog.setLogId(1L);
        existingLog.setZoneId("Z001");
        existingLog.setVehicleId("RT001");
        existingLog.setWorkerId("W001");
        existingLog.setCollectionStartTime(LocalDateTime.of(2025, 6, 20, 9, 0, 0));
        existingLog.setCreatedDate(LocalDateTime.of(2025, 6, 20, 9, 0, 0));
        // collectionEndTime and weightCollected are null initially

        // When
        when(wasteLogRepository.findById(1L)).thenReturn(Optional.of(existingLog));
        // Mock save to return the same object after update (common practice)
        when(wasteLogRepository.save(any(WasteLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(FIXED_NOW);

            WasteLogResponseDTO response = wasteLogService.endCollection(request);

            // Then
            assertNotNull(response);
            assertEquals(1L, response.getLogId());
            assertEquals(WasteLogConstants.WASTE_COLLECTION_LOG_COMPLETED_SUCCESSFULLY, response.getMessage());

            // Verify repository calls and the updated state of the log
            verify(wasteLogRepository, times(1)).findById(1L);
            verify(wasteLogRepository, times(1)).save(argThat(log ->
                    log.getLogId().equals(1L) &&
                            log.getCollectionEndTime().equals(FIXED_NOW) &&
                            log.getWeightCollected().equals(150.0) &&
                            log.getUpdatedDate().equals(FIXED_NOW)
            ));
        }
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenLogNotFound: Ending a non-existent log should throw exception")
    void shouldThrowResourceNotFoundExceptionWhenLogNotFound() {
        // Given
        WasteLogUpdateRequestDTO request = new WasteLogUpdateRequestDTO(999L, 100.0);

        // When
        when(wasteLogRepository.findById(999L)).thenReturn(Optional.empty());

        // Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                wasteLogService.endCollection(request)
        );

        String expectedMessage = String.format(WasteLogConstants.WASTE_LOG_NOT_FOUND_MESSAGE, 999L);
        assertEquals(expectedMessage, exception.getMessage());

        verify(wasteLogRepository, times(1)).findById(999L);
        verify(wasteLogRepository, never()).save(any(WasteLog.class));
    }

    @Test
    @DisplayName("shouldThrowLogAlreadyCompletedExceptionWhenLogAlreadyCompleted: Ending an already completed log should throw exception")
    void shouldThrowLogAlreadyCompletedExceptionWhenLogAlreadyCompleted() {
        // Given
        WasteLogUpdateRequestDTO request = new WasteLogUpdateRequestDTO(1L, 100.0);
        // Simplified WasteLog constructor for a completed log
        WasteLog completedLog = new WasteLog();
        completedLog.setLogId(1L);
        completedLog.setCollectionEndTime(LocalDateTime.of(2025, 6, 20, 9, 0, 0)); // Not null

        // When
        when(wasteLogRepository.findById(1L)).thenReturn(Optional.of(completedLog));

        // Then
        LogAlreadyCompletedException exception = assertThrows(LogAlreadyCompletedException.class, () ->
                wasteLogService.endCollection(request)
        );

        String expectedMessage = String.format(WasteLogConstants.LOG_ALREADY_COMPLETED_MESSAGE, 1L);
        assertEquals(expectedMessage, exception.getMessage());

        verify(wasteLogRepository, times(1)).findById(1L);
        verify(wasteLogRepository, never()).save(any(WasteLog.class));
    }

    @Test
    @DisplayName("shouldThrowInvalidInputExceptionWhenEndTimeIsBeforeStartTime: End time before start time should throw exception")
    void shouldThrowInvalidInputExceptionWhenEndTimeIsBeforeStartTime() {
        // Given
        WasteLogUpdateRequestDTO request = new WasteLogUpdateRequestDTO(1L, 100.0);
        // Simulate a log whose start time is in the future relative to our mocked FIXED_NOW (10:00:00)
        WasteLog existingLog = new WasteLog();
        existingLog.setLogId(1L);
        existingLog.setCollectionStartTime(LocalDateTime.of(2025, 6, 20, 11, 0, 0)); // Start time is 11:00:00

        // When
        when(wasteLogRepository.findById(1L)).thenReturn(Optional.of(existingLog));

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(FIXED_NOW); // FIXED_NOW is 10:00:00

            // Then
            InvalidInputException exception = assertThrows(InvalidInputException.class, () ->
                    wasteLogService.endCollection(request)
            );

            assertEquals(WasteLogConstants.COLLECTION_END_TIME_BEFORE_START_TIME, exception.getMessage());

            verify(wasteLogRepository, times(1)).findById(1L);
            verify(wasteLogRepository, never()).save(any(WasteLog.class));
        }
    }

    // --- 3. getZoneLogs Tests (Updated for Pageable and Page return) ---

    @Test
    @DisplayName("shouldReturnPaginatedZoneReportsForCompletedLogs: Should correctly aggregate and paginate completed logs for zone")
    void shouldReturnPaginatedZoneReportsForCompletedLogs() {
        // Given
        String zoneId = "Z001";
        LocalDate startDate = LocalDate.of(2025, 6, 18);
        LocalDate endDate = LocalDate.of(2025, 6, 20);
        Pageable pageable = PageRequest.of(0, 2, Sort.by("date").ascending()); // Request page 0, size 2

        List<WasteLog> mockLogs = Arrays.asList(
                // Day 1 (June 18): 2 completed logs, 2 unique vehicles, total 120.0kg
                createWasteLog(1L, zoneId, "RT001", "W001", LocalDate.of(2025, 6, 18), 50.0),
                createWasteLog(2L, zoneId, "RT002", "W002", LocalDate.of(2025, 6, 18), 70.0),
                // Day 2 (June 19): 1 completed log, 1 unique vehicle, total 60.0kg
                createWasteLog(3L, zoneId, "RT001", "W001", LocalDate.of(2025, 6, 19), 60.0),
                new WasteLog(4L, zoneId, "RT003", "W003", LocalDateTime.of(2025, 6, 19, 11, 0, 0), null, null, LocalDateTime.now(), "user", null, null), // Active log (should be ignored)
                // Day 3 (June 20): 1 completed log, 1 unique vehicle, total 80.0kg
                createWasteLog(5L, zoneId, "RT004", "W004", LocalDate.of(2025, 6, 20), 80.0)
        );

        // When
        when(wasteLogRepository.findByZoneIdAndCollectionStartTimeBetween(
                eq(zoneId), eq(startDate.atStartOfDay()), eq(endDate.atTime(LocalTime.MAX))))
                .thenReturn(mockLogs);

        Page<ZoneReportDTO> resultPage = wasteLogService.getZoneLogs(zoneId, startDate, endDate, pageable);

        // Then
        assertNotNull(resultPage);
        assertEquals(2, resultPage.getContent().size()); // Expect 2 elements on page 0 (because page size is 2)
        assertEquals(2, resultPage.getTotalPages()); // 3 aggregated reports (Day 1, Day 2, Day 3) / page size 2 = 1.5 -> 2 pages
        assertEquals(3, resultPage.getTotalElements()); // 3 aggregated reports in total (not 5 original logs)
        assertEquals(0, resultPage.getNumber()); // Current page is 0
        assertEquals(2, resultPage.getSize()); // Page size requested is 2
        assertTrue(resultPage.isFirst());
        assertFalse(resultPage.isLast());

        // Verify content and order of the first page (sorted by date)
        ZoneReportDTO day1Report = resultPage.getContent().get(0);
        assertEquals(zoneId, day1Report.getZoneId());
        assertEquals(LocalDate.of(2025, 6, 18), day1Report.getDate());
        assertEquals(2L, day1Report.getTotalNumberOfCollections()); // RT001, RT002
        assertEquals(120.0, day1Report.getTotalWeightCollectedKg(), 0.001); // 50 + 70

        ZoneReportDTO day2Report = resultPage.getContent().get(1);
        assertEquals(zoneId, day2Report.getZoneId());
        assertEquals(LocalDate.of(2025, 6, 19), day2Report.getDate());
        assertEquals(1L, day2Report.getTotalNumberOfCollections()); // RT001
        assertEquals(60.0, day2Report.getTotalWeightCollectedKg(), 0.001);

        verify(wasteLogRepository, times(1))
                .findByZoneIdAndCollectionStartTimeBetween(zoneId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    }

    @Test
    @DisplayName("shouldReturnEmptyPageWhenNoCompletedLogsFoundForZone: No completed logs should return empty page")
    void shouldReturnEmptyPageWhenNoCompletedLogsFoundForZone() {
        // Given
        String zoneId = "Z002";
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        when(wasteLogRepository.findByZoneIdAndCollectionStartTimeBetween(
                eq(zoneId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList()); // No logs found

        Page<ZoneReportDTO> resultPage = wasteLogService.getZoneLogs(zoneId, startDate, endDate, pageable);

        // Then
        assertNotNull(resultPage);
        assertTrue(resultPage.isEmpty());
        assertEquals(0, resultPage.getTotalElements());
        assertEquals(0, resultPage.getTotalPages());
        assertEquals(0, resultPage.getContent().size());
        verify(wasteLogRepository, times(1))
                .findByZoneIdAndCollectionStartTimeBetween(zoneId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    }

    @Test
    @DisplayName("shouldThrowInvalidInputExceptionWhenStartDateIsAfterEndDateForZoneReports: Invalid date range should throw exception")
    void shouldThrowInvalidInputExceptionWhenStartDateIsAfterEndDateForZoneReports() {
        // Given
        String zoneId = "Z001";
        LocalDate startDate = LocalDate.of(2025, 6, 20);
        LocalDate endDate = LocalDate.of(2025, 6, 18); // Start after End
        Pageable pageable = PageRequest.of(0, 10);

        // When / Then
        InvalidInputException exception = assertThrows(InvalidInputException.class, () ->
                wasteLogService.getZoneLogs(zoneId, startDate, endDate, pageable)
        );

        assertEquals(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE, exception.getMessage());
        verify(wasteLogRepository, never()).findByZoneIdAndCollectionStartTimeBetween(anyString(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    // --- 4. getVehicleLogs Tests ---

    @Test
    @DisplayName("shouldReturnPaginatedVehicleReportsForCompletedLogs: Should return all completed logs for vehicle, paginated and sorted")
    void shouldReturnPaginatedVehicleReportsForCompletedLogs() {
        // Given
        String vehicleId = "RT001";
        LocalDate startDate = LocalDate.of(2025, 6, 18);
        LocalDate endDate = LocalDate.of(2025, 6, 20);
        Pageable pageable = PageRequest.of(0, 2, Sort.by("collectionDate").ascending()); // Request page 0, size 2

        List<WasteLog> mockLogs = Arrays.asList(
                createWasteLog(1L, "Z001", vehicleId, "W001", LocalDate.of(2025, 6, 18), 50.0), // Log 1
                createWasteLog(2L, "Z002", vehicleId, "W001", LocalDate.of(2025, 6, 19), 75.0), // Log 2
                new WasteLog(3L, "Z003", vehicleId, "W002", LocalDateTime.of(2025, 6, 20, 11, 0, 0), null, null, LocalDateTime.now(), "user", null, null), // Active log, should be ignored
                createWasteLog(4L, "Z001", vehicleId, "W001", LocalDate.of(2025, 6, 20), 60.0) // Log 3
        );

        // When
        when(wasteLogRepository.findByVehicleIdAndCollectionStartTimeBetween(
                eq(vehicleId), eq(startDate.atStartOfDay()), eq(endDate.atTime(LocalTime.MAX))))
                .thenReturn(mockLogs);

        Page<VehicleReportDTO> resultPage = wasteLogService.getVehicleLogs(vehicleId, startDate, endDate, pageable);

        // Then
        assertNotNull(resultPage);
        assertEquals(2, resultPage.getContent().size()); // Expect 2 elements on page 0
        assertEquals(2, resultPage.getTotalPages()); // 3 completed logs / page size 2 = 1.5 -> 2 pages
        assertEquals(3, resultPage.getTotalElements()); // 3 completed logs in total
        assertEquals(0, resultPage.getNumber()); // Current page is 0
        assertEquals(2, resultPage.getSize()); // Page size requested is 2
        assertTrue(resultPage.isFirst());
        assertFalse(resultPage.isLast());


        // Verify content and order of the first page (sorted by collectionDate)
        assertEquals(LocalDate.of(2025, 6, 18), resultPage.getContent().get(0).getCollectionDate());
        assertEquals("Z001", resultPage.getContent().get(0).getZoneId());
        assertEquals(50.0, resultPage.getContent().get(0).getWeightCollected(), 0.001);

        assertEquals(LocalDate.of(2025, 6, 19), resultPage.getContent().get(1).getCollectionDate());
        assertEquals("Z002", resultPage.getContent().get(1).getZoneId());
        assertEquals(75.0, resultPage.getContent().get(1).getWeightCollected(), 0.001);

        verify(wasteLogRepository, times(1))
                .findByVehicleIdAndCollectionStartTimeBetween(vehicleId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    }

    @Test
    @DisplayName("shouldReturnEmptyPageWhenNoCompletedLogsFoundForVehicle: No completed logs should return empty page")
    void shouldReturnEmptyPageWhenNoCompletedLogsFoundForVehicle() {
        // Given
        String vehicleId = "PT001";
        LocalDate startDate = LocalDate.of(2025, 2, 1);
        LocalDate endDate = LocalDate.of(2025, 2, 28);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        when(wasteLogRepository.findByVehicleIdAndCollectionStartTimeBetween(
                eq(vehicleId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList()); // No logs found

        Page<VehicleReportDTO> resultPage = wasteLogService.getVehicleLogs(vehicleId, startDate, endDate, pageable);

        // Then
        assertNotNull(resultPage);
        assertTrue(resultPage.isEmpty());
        assertEquals(0, resultPage.getTotalElements());
        assertEquals(0, resultPage.getTotalPages());
        assertEquals(0, resultPage.getContent().size());
        verify(wasteLogRepository, times(1))
                .findByVehicleIdAndCollectionStartTimeBetween(vehicleId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    }

    @Test
    @DisplayName("shouldThrowInvalidInputExceptionWhenStartDateIsAfterEndDateForVehicleReports: Invalid date range should throw exception")
    void shouldThrowInvalidInputExceptionWhenStartDateIsAfterEndDateForVehicleReports() {
        // Given
        String vehicleId = "RT001";
        LocalDate startDate = LocalDate.of(2025, 6, 20);
        LocalDate endDate = LocalDate.of(2025, 6, 18); // Start after End
        Pageable pageable = PageRequest.of(0, 10);

        // When / Then
        InvalidInputException exception = assertThrows(InvalidInputException.class, () ->
                wasteLogService.getVehicleLogs(vehicleId, startDate, endDate, pageable)
        );

        assertEquals(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE, exception.getMessage());
        verify(wasteLogRepository, never()).findByVehicleIdAndCollectionStartTimeBetween(anyString(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    // Helper method to create a completed WasteLog for reporting tests
    // Assuming WasteLog has a constructor or setters for these fields
    private WasteLog createWasteLog(Long id, String zoneId, String vehicleId, String workerId, LocalDate collectionDate, double weight) {
        WasteLog log = new WasteLog();
        log.setLogId(id);
        log.setZoneId(zoneId);
        log.setVehicleId(vehicleId);
        log.setWorkerId(workerId);
        // Set start time slightly before end time for a completed log
        log.setCollectionStartTime(collectionDate.atTime(8, 0));
        log.setCollectionEndTime(collectionDate.atTime(9, 0));
        log.setWeightCollected(weight);
        log.setCreatedDate(collectionDate.atStartOfDay());
        // For simplicity, updatedDate, createdBy, updatedBy are not set in this helper
        return log;
    }
}