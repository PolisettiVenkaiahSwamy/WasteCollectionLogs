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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito annotations for JUnit 5
class WasteLogServiceImplTest {

    @Mock // Creates a mock instance of WasteLogRepository
    private WasteLogRepository wasteLogRepository;

    @InjectMocks // Injects the mocks into WasteLogServiceImpl
    private WasteLogServiceImpl wasteLogService;

    // Fixed time for consistent testing of LocalDateTime.now()
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2025, 6, 20, 10, 0, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    @BeforeEach
    void setUp() {
        // No explicit setup needed here as @Mock and @InjectMocks handle it.
        // If you had common mock behaviors, you'd put them here.
    }

    // --- 1. startCollection Tests ---

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
        // collectionStartTime and createdDate will be set by service

        // When
        when(wasteLogRepository.findByWorkerIdAndZoneIdAndVehicleIdAndCollectionEndTimeIsNull(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty()); // No active log exists
        when(wasteLogRepository.save(any(WasteLog.class))).thenReturn(newLog);

        // Use MockedStatic to control LocalDateTime.now()
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(FIXED_NOW);

            WasteLogResponseDTO response = wasteLogService.startCollection(request);

            // Then
            assertNotNull(response);
            assertEquals(1L, response.getLogId());
            assertEquals(WasteLogConstants.WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY, response.getMessage());

            // Verify repository calls
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
        WasteLog activeLog = new WasteLog(1L, "Z001", "RT001", "W001", LocalDateTime.now().minusHours(1), null, null, LocalDateTime.now().minusHours(1), "user", null, null);

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
        WasteLog existingLog = new WasteLog(1L, "Z001", "RT001", "W001", LocalDateTime.of(2025, 6, 20, 9, 0, 0), null, null, LocalDateTime.of(2025, 6, 20, 9, 0, 0), "user", null, null);
        WasteLog updatedLog = new WasteLog(1L, "Z001", "RT001", "W001", LocalDateTime.of(2025, 6, 20, 9, 0, 0), FIXED_NOW, 150.0, LocalDateTime.of(2025, 6, 20, 9, 0, 0), "user", FIXED_NOW, null);


        // When
        when(wasteLogRepository.findById(1L)).thenReturn(Optional.of(existingLog));
        when(wasteLogRepository.save(any(WasteLog.class))).thenReturn(updatedLog);

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(FIXED_NOW);

            WasteLogResponseDTO response = wasteLogService.endCollection(request);

            // Then
            assertNotNull(response);
            assertEquals(1L, response.getLogId());
            assertEquals(WasteLogConstants.WASTE_COLLECTION_LOG_COMPLETED_SUCCESSFULLY, response.getMessage());

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
        WasteLog completedLog = new WasteLog(1L, "Z001", "RT001", "W001", LocalDateTime.of(2025, 6, 20, 8, 0, 0), LocalDateTime.of(2025, 6, 20, 9, 0, 0), 100.0, LocalDateTime.of(2025, 6, 20, 8, 0, 0), "user", LocalDateTime.of(2025, 6, 20, 9, 0, 0), null);

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
        // Simulate a log whose start time is in the future relative to our mocked FIXED_NOW
        WasteLog existingLog = new WasteLog(1L, "Z001", "RT001", "W001", LocalDateTime.of(2025, 6, 20, 11, 0, 0), null, null, LocalDateTime.of(2025, 6, 20, 11, 0, 0), "user", null, null);

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

    // --- 3. getZoneLogs Tests ---

    @Test
    @DisplayName("shouldReturnZoneReportsForCompletedLogs: Should correctly aggregate completed logs for zone")
    void shouldReturnZoneReportsForCompletedLogs() {
        // Given
        String zoneId = "Z001";
        LocalDate startDate = LocalDate.of(2025, 6, 18);
        LocalDate endDate = LocalDate.of(2025, 6, 20);

        List<WasteLog> mockLogs = Arrays.asList(
                // Day 1: 2 completed logs
                new WasteLog(1L, zoneId, "RT001", "W001", LocalDateTime.of(2025, 6, 18, 9, 0, 0), LocalDateTime.of(2025, 6, 18, 9, 30, 0), 50.0, LocalDateTime.now(), "user", LocalDateTime.now(), null),
                new WasteLog(2L, zoneId, "RT002", "W002", LocalDateTime.of(2025, 6, 18, 10, 0, 0), LocalDateTime.of(2025, 6, 18, 10, 45, 0), 70.0, LocalDateTime.now(), "user", LocalDateTime.now(), null),
                // Day 2: 1 completed log, 1 active log (should be ignored)
                new WasteLog(3L, zoneId, "RT001", "W001", LocalDateTime.of(2025, 6, 19, 9, 0, 0), LocalDateTime.of(2025, 6, 19, 9, 50, 0), 60.0, LocalDateTime.now(), "user", LocalDateTime.now(), null),
                new WasteLog(4L, zoneId, "RT003", "W003", LocalDateTime.of(2025, 6, 19, 11, 0, 0), null, null, LocalDateTime.now(), "user", null, null), // Active log
                // Day 3: 1 completed log (different vehicle from Day 1 RT001)
                new WasteLog(5L, zoneId, "RT004", "W004", LocalDateTime.of(2025, 6, 20, 8, 0, 0), LocalDateTime.of(2025, 6, 20, 8, 45, 0), 80.0, LocalDateTime.now(), "user", LocalDateTime.now(), null)
        );

        // When
        when(wasteLogRepository.findByZoneIdAndCollectionStartTimeBetween(
                eq(zoneId), eq(startDate.atStartOfDay()), eq(endDate.atTime(LocalTime.MAX))))
                .thenReturn(mockLogs);

        List<ZoneReportDTO> reports = wasteLogService.getZoneLogs(zoneId, startDate, endDate);

        // Then
        assertNotNull(reports);
        assertEquals(3, reports.size()); // Expecting 3 days with reports

        // Verify Day 1 report
        ZoneReportDTO day1Report = reports.get(0);
        assertEquals(zoneId, day1Report.getZoneId());
        assertEquals(LocalDate.of(2025, 6, 18), day1Report.getDate());
        assertEquals(2L, day1Report.getTotalNumberOfCollections()); // RT001, RT002
        assertEquals(120.0, day1Report.getTotalWeightCollectedKg(), 0.001); // 50 + 70

        // Verify Day 2 report
        ZoneReportDTO day2Report = reports.get(1);
        assertEquals(zoneId, day2Report.getZoneId());
        assertEquals(LocalDate.of(2025, 6, 19), day2Report.getDate());
        assertEquals(1L, day2Report.getTotalNumberOfCollections()); // RT001
        assertEquals(60.0, day2Report.getTotalWeightCollectedKg(), 0.001);

        // Verify Day 3 report
        ZoneReportDTO day3Report = reports.get(2);
        assertEquals(zoneId, day3Report.getZoneId());
        assertEquals(LocalDate.of(2025, 6, 20), day3Report.getDate());
        assertEquals(1L, day3Report.getTotalNumberOfCollections()); // RT004
        assertEquals(80.0, day3Report.getTotalWeightCollectedKg(), 0.001);

        verify(wasteLogRepository, times(1))
                .findByZoneIdAndCollectionStartTimeBetween(zoneId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    }

    @Test
    @DisplayName("shouldReturnEmptyListWhenNoCompletedLogsFoundForZone: No completed logs should return empty list")
    void shouldReturnEmptyListWhenNoCompletedLogsFoundForZone() {
        // Given
        String zoneId = "Z002";
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);

        // When
        when(wasteLogRepository.findByZoneIdAndCollectionStartTimeBetween(
                eq(zoneId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList()); // No logs found

        List<ZoneReportDTO> reports = wasteLogService.getZoneLogs(zoneId, startDate, endDate);

        // Then
        assertNotNull(reports);
        assertTrue(reports.isEmpty());
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

        // When / Then
        InvalidInputException exception = assertThrows(InvalidInputException.class, () ->
                wasteLogService.getZoneLogs(zoneId, startDate, endDate)
        );

        assertEquals(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE, exception.getMessage());
        verify(wasteLogRepository, never()).findByZoneIdAndCollectionStartTimeBetween(anyString(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    // --- 4. getVehicleLogs Tests ---

    @Test
    @DisplayName("shouldReturnVehicleReportsForCompletedLogs: Should return all completed logs for vehicle")
    void shouldReturnVehicleReportsForCompletedLogs() {
        // Given
        String vehicleId = "RT001";
        LocalDate startDate = LocalDate.of(2025, 6, 18);
        LocalDate endDate = LocalDate.of(2025, 6, 20);

        List<WasteLog> mockLogs = Arrays.asList(
                new WasteLog(1L, "Z001", vehicleId, "W001", LocalDateTime.of(2025, 6, 18, 9, 0, 0), LocalDateTime.of(2025, 6, 18, 9, 30, 0), 50.0, LocalDateTime.now(), "user", LocalDateTime.now(), null),
                new WasteLog(2L, "Z002", vehicleId, "W001", LocalDateTime.of(2025, 6, 19, 10, 0, 0), LocalDateTime.of(2025, 6, 19, 10, 45, 0), 75.0, LocalDateTime.now(), "user", LocalDateTime.now(), null),
                new WasteLog(3L, "Z003", vehicleId, "W002", LocalDateTime.of(2025, 6, 20, 11, 0, 0), null, null, LocalDateTime.now(), "user", null, null), // Active log, should be ignored
                new WasteLog(4L, "Z001", vehicleId, "W001", LocalDateTime.of(2025, 6, 20, 12, 0, 0), LocalDateTime.of(2025, 6, 20, 12, 30, 0), 60.0, LocalDateTime.now(), "user", LocalDateTime.now(), null)
        );

        // When
        when(wasteLogRepository.findByVehicleIdAndCollectionStartTimeBetween(
                eq(vehicleId), eq(startDate.atStartOfDay()), eq(endDate.atTime(LocalTime.MAX))))
                .thenReturn(mockLogs);

        List<VehicleReportDTO> reports = wasteLogService.getVehicleLogs(vehicleId, startDate, endDate);

        // Then
        assertNotNull(reports);
        assertEquals(3, reports.size()); // Only completed logs

        // Verify content and order
        assertEquals(LocalDate.of(2025, 6, 18), reports.get(0).getCollectionDate());
        assertEquals(50.0, reports.get(0).getWeightCollected(), 0.001);

        assertEquals(LocalDate.of(2025, 6, 19), reports.get(1).getCollectionDate());
        assertEquals(75.0, reports.get(1).getWeightCollected(), 0.001);

        assertEquals(LocalDate.of(2025, 6, 20), reports.get(2).getCollectionDate());
        assertEquals(60.0, reports.get(2).getWeightCollected(), 0.001);

        verify(wasteLogRepository, times(1))
                .findByVehicleIdAndCollectionStartTimeBetween(vehicleId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    }

    @Test
    @DisplayName("shouldReturnEmptyListWhenNoCompletedLogsFoundForVehicle: No completed logs should return empty list")
    void shouldReturnEmptyListWhenNoCompletedLogsFoundForVehicle() {
        // Given
        String vehicleId = "PT001";
        LocalDate startDate = LocalDate.of(2025, 2, 1);
        LocalDate endDate = LocalDate.of(2025, 2, 28);

        // When
        when(wasteLogRepository.findByVehicleIdAndCollectionStartTimeBetween(
                eq(vehicleId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList()); // No logs found

        List<VehicleReportDTO> reports = wasteLogService.getVehicleLogs(vehicleId, startDate, endDate);

        // Then
        assertNotNull(reports);
        assertTrue(reports.isEmpty());
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

        // When / Then
        InvalidInputException exception = assertThrows(InvalidInputException.class, () ->
                wasteLogService.getVehicleLogs(vehicleId, startDate, endDate)
        );

        assertEquals(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE, exception.getMessage());
        verify(wasteLogRepository, never()).findByVehicleIdAndCollectionStartTimeBetween(anyString(), any(LocalDateTime.class), any(LocalDateTime.class));
    }
}