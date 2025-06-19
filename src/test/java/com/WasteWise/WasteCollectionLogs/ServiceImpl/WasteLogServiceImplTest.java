package com.WasteWise.WasteCollectionLogs.ServiceImpl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.ArgumentMatchers.eq; // Make sure this is imported
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WasteLogServiceImpl - Unit Tests (Assuming DTO Validation)")
class WasteLogServiceImplTest {

    @Mock
    private WasteLogRepository wasteLogRepository;

    @InjectMocks
    private WasteLogServiceImpl wasteLogService;

    private WasteLogStartRequestDto validStartRequestDto;
    private WasteLogUpdateRequestDto validUpdateRequestDto;
    private WasteLog wasteLogInProgress; // A log that is not yet ended
    private WasteLog wasteLogEnded;      // A log that has already been ended

    @BeforeEach
    void setUp() {
        // DTOs will be assumed to be valid from the controller's perspective
        validStartRequestDto = new WasteLogStartRequestDto("Z001", "RT123", "W456");
        validUpdateRequestDto = new WasteLogUpdateRequestDto(1L, 150.50);

        wasteLogInProgress = new WasteLog();
        wasteLogInProgress.setLogId(1L);
        wasteLogInProgress.setZoneId("Z001");
        wasteLogInProgress.setVehicleId("RT123");
        wasteLogInProgress.setWorkerId("W456");
        wasteLogInProgress.setCollectionStartTime(LocalDateTime.now().minusHours(2));
        wasteLogInProgress.setCreatedDate(LocalDateTime.now().minusHours(2));
        // collectionEndTime and weightCollected are null for in-progress

        wasteLogEnded = new WasteLog();
        wasteLogEnded.setLogId(2L);
        wasteLogEnded.setZoneId("Z002");
        wasteLogEnded.setVehicleId("PT005");
        wasteLogEnded.setWorkerId("W002");
        wasteLogEnded.setCollectionStartTime(LocalDateTime.now().minusHours(5));
        wasteLogEnded.setCollectionEndTime(LocalDateTime.now().minusHours(4));
        wasteLogEnded.setWeightCollected(200.0);
        wasteLogEnded.setCreatedDate(LocalDateTime.now().minusHours(5));
        wasteLogEnded.setUpdatedDate(LocalDateTime.now().minusHours(4));
    }

    // --- startCollection Tests ---

    @Test
    @DisplayName("startCollection: Should successfully start a collection log with valid input")
    void testStartCollection_Success() {

        WasteLog newLog = new WasteLog();
        newLog.setLogId(1L); // Simulating DB assigned ID after save

        when(wasteLogRepository.save(any(WasteLog.class))).thenReturn(newLog);

        // When
        WasteLogResponseDto response = wasteLogService.startCollection(validStartRequestDto);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getLogId());
        assertEquals(WasteLogConstants.WASTE_COLLECTION_LOG_RECORDED_SUCCESSFULLY, response.getMessage());

        // Verify that the save method was called with a WasteLog entity correctly populated
        verify(wasteLogRepository, times(1)).save(argThat(log ->
            log.getZoneId().equals(validStartRequestDto.getZoneId()) &&
            log.getVehicleId().equals(validStartRequestDto.getVehicleId()) &&
            log.getWorkerId().equals(validStartRequestDto.getWorkerId()) &&
            log.getCollectionStartTime() != null &&
            log.getCreatedDate() != null &&
            log.getCollectionEndTime() == null && // Must be null for new log
            log.getWeightCollected() == null      // Must be null for new log
        ));
    }


    // --- endCollection Tests ---

    @Test
    @DisplayName("endCollection: Should successfully end an existing collection log")
    void testEndCollection_Success() {

        when(wasteLogRepository.findById(validUpdateRequestDto.getLogId())).thenReturn(Optional.of(wasteLogInProgress));
        when(wasteLogRepository.save(any(WasteLog.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return the saved entity

        // When
        WasteLogResponseDto response = wasteLogService.endCollection(validUpdateRequestDto);

        // Then
        assertNotNull(response);
        assertEquals(validUpdateRequestDto.getLogId(), response.getLogId());
        assertEquals(WasteLogConstants.WASTE_COLLECTION_LOG_COMPLETED_SUCCESSFULLY, response.getMessage());

        // Verify the log was updated correctly
        verify(wasteLogRepository, times(1)).findById(validUpdateRequestDto.getLogId());
        verify(wasteLogRepository, times(1)).save(argThat(log ->
                log.getLogId().equals(validUpdateRequestDto.getLogId()) &&
                log.getWeightCollected().equals(validUpdateRequestDto.getWeightCollected()) &&
                log.getCollectionEndTime() != null &&
                log.getUpdatedDate() != null &&
                log.getCollectionEndTime().isAfter(wasteLogInProgress.getCollectionStartTime()) // End time is after start time
        ));
    }

    @Test
    @DisplayName("endCollection: Should throw ResourceNotFoundException when ending a non-existent log")
    void testEndCollection_LogNotFound() {
        // Given
        Long nonExistentLogId = 99L;
        WasteLogUpdateRequestDto requestDto = new WasteLogUpdateRequestDto(nonExistentLogId, 100.0);

        when(wasteLogRepository.findById(nonExistentLogId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            wasteLogService.endCollection(requestDto);
        });

        assertEquals(String.format(WasteLogConstants.WASTE_LOG_NOT_FOUND_MESSAGE, nonExistentLogId), exception.getMessage());
        verify(wasteLogRepository, never()).save(any(WasteLog.class)); // Ensure save is not called
    }

    @Test
    @DisplayName("endCollection: Should throw LogAlreadyCompletedException when log is already completed")
    void testEndCollection_LogAlreadyCompleted() {
        // Given
        WasteLogUpdateRequestDto requestDto = new WasteLogUpdateRequestDto(wasteLogEnded.getLogId(), 100.0);

        when(wasteLogRepository.findById(requestDto.getLogId())).thenReturn(Optional.of(wasteLogEnded));

        // When & Then
        LogAlreadyCompletedException exception = assertThrows(LogAlreadyCompletedException.class, () -> {
            wasteLogService.endCollection(requestDto);
        });

        assertEquals(String.format(WasteLogConstants.LOG_ALREADY_COMPLETED_MESSAGE, requestDto.getLogId()), exception.getMessage());
        verify(wasteLogRepository, never()).save(any(WasteLog.class)); // Ensure save is not called
    }

    @Test
    @DisplayName("endCollection: Should throw InvalidInputException when collection end time is before start time (business rule)")
    void testEndCollection_EndTimeBeforeStartTime() {
        // Given
        // Simulate a scenario where current time (used by service for end time) is before the log's start time
        wasteLogInProgress.setCollectionStartTime(LocalDateTime.now().plusDays(1)); // Future start time for current mock

        WasteLogUpdateRequestDto requestDto = new WasteLogUpdateRequestDto(wasteLogInProgress.getLogId(), 100.0);

        when(wasteLogRepository.findById(requestDto.getLogId())).thenReturn(Optional.of(wasteLogInProgress));

        // When & Then
        InvalidInputException exception = assertThrows(InvalidInputException.class, () -> {
            wasteLogService.endCollection(requestDto);
        });

        assertEquals(WasteLogConstants.COLLECTION_END_TIME_BEFORE_START_TIME, exception.getMessage());
        verify(wasteLogRepository, never()).save(any(WasteLog.class)); // Ensure save is not called
    }

    // No tests here for null/zero/negative weight collected or null logId as DTO validation handles them.

    // --- getZoneLogs Tests ---

    @Test
    @DisplayName("getZoneLogs: Should retrieve correct report with multiple completed logs across different days")
    void testGetZoneLogs_Success_MultipleDays() {
        // Given
        String zoneId = "Z001";
        LocalDate startDate = LocalDate.of(2024, 6, 1);
        LocalDate endDate = LocalDate.of(2024, 6, 3);

        WasteLog log1 = new WasteLog();
        log1.setLogId(1L); log1.setZoneId("Z001"); log1.setVehicleId("RT101"); log1.setWorkerId("W001");
        log1.setCollectionStartTime(LocalDateTime.of(2024, 6, 1, 9, 0)); log1.setCollectionEndTime(LocalDateTime.of(2024, 6, 1, 10, 0));
        log1.setWeightCollected(100.0); log1.setCreatedDate(LocalDateTime.now());

        WasteLog log2 = new WasteLog();
        log2.setLogId(2L); log2.setZoneId("Z001"); log2.setVehicleId("RT102"); log2.setWorkerId("W002");
        log2.setCollectionStartTime(LocalDateTime.of(2024, 6, 2, 10, 0)); log2.setCollectionEndTime(LocalDateTime.of(2024, 6, 2, 11, 0));
        log2.setWeightCollected(150.0); log2.setCreatedDate(LocalDateTime.now());

        WasteLog log3 = new WasteLog();
        log3.setLogId(3L); log3.setZoneId("Z001"); log3.setVehicleId("RT101"); log3.setWorkerId("W003"); // Same vehicle RT101 on a different day
        log3.setCollectionStartTime(LocalDateTime.of(2024, 6, 3, 11, 0)); log3.setCollectionEndTime(LocalDateTime.of(2024, 6, 3, 12, 0));
        log3.setWeightCollected(200.0); log3.setCreatedDate(LocalDateTime.now());

        // An uncompleted log (collectionEndTime is null), should not be included in the report calculation
        WasteLog logUncompleted = new WasteLog();
        logUncompleted.setLogId(4L); logUncompleted.setZoneId("Z001"); logUncompleted.setVehicleId("RT103"); logUncompleted.setWorkerId("W004");
        logUncompleted.setCollectionStartTime(LocalDateTime.of(2024, 6, 2, 14, 0)); logUncompleted.setCollectionEndTime(null);
        logUncompleted.setWeightCollected(0.0); logUncompleted.setCreatedDate(LocalDateTime.now());

        when(wasteLogRepository.findByZoneIdAndCollectionStartTimeBetween(
            eq(zoneId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(log1, log2, log3, logUncompleted));

        // When
        List<ZoneReportDto> reports = wasteLogService.getZoneLogs(zoneId, startDate, endDate);

        // Then
        assertNotNull(reports);
        assertEquals(3, reports.size()); // Expect 3 daily reports for completed logs

        // Verify reports are sorted by date
        assertEquals(LocalDate.of(2024, 6, 1), reports.get(0).getDate());
        assertEquals(LocalDate.of(2024, 6, 2), reports.get(1).getDate());
        assertEquals(LocalDate.of(2024, 6, 3), reports.get(2).getDate());

        // Verify report for June 1
        ZoneReportDto reportDay1 = reports.get(0);
        assertEquals(zoneId, reportDay1.getZoneId());
        assertEquals(100.0, reportDay1.getTotalWeightCollectedKg()); // Corrected method name
        assertEquals(1L, reportDay1.getVehiclesUsed()); // Corrected method name

        // Verify report for June 2
        ZoneReportDto reportDay2 = reports.get(1);
        assertEquals(zoneId, reportDay2.getZoneId());
        assertEquals(150.0, reportDay2.getTotalWeightCollectedKg()); // Corrected method name
        assertEquals(1L, reportDay2.getVehiclesUsed()); // Corrected method name

        // Verify report for June 3
        ZoneReportDto reportDay3 = reports.get(2);
        assertEquals(zoneId, reportDay3.getZoneId());
        assertEquals(200.0, reportDay3.getTotalWeightCollectedKg()); // Corrected method name
        assertEquals(1L, reportDay3.getVehiclesUsed()); // Corrected method name

        // *** FIX APPLIED HERE ***
        verify(wasteLogRepository, times(1)).findByZoneIdAndCollectionStartTimeBetween(
            eq(zoneId), eq(startDate.atStartOfDay()), eq(endDate.atTime(LocalTime.MAX)) // All arguments are now matchers
        );
    }

    @Test
    @DisplayName("getZoneLogs: Should return an empty list when no completed logs exist for the period")
    void testGetZoneLogs_NoCompletedLogs() {
        // Given
        String zoneId = "Z003";
        LocalDate startDate = LocalDate.of(2024, 8, 1);
        LocalDate endDate = LocalDate.of(2024, 8, 5);

        when(wasteLogRepository.findByZoneIdAndCollectionStartTimeBetween(
            eq(zoneId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        List<ZoneReportDto> reports = wasteLogService.getZoneLogs(zoneId, startDate, endDate);

        // Then
        assertNotNull(reports);
        assertTrue(reports.isEmpty());
        // *** FIX APPLIED HERE ***
        verify(wasteLogRepository, times(1)).findByZoneIdAndCollectionStartTimeBetween(
            eq(zoneId), eq(startDate.atStartOfDay()), eq(endDate.atTime(LocalTime.MAX)) // All arguments are now matchers
        );
    }

    @Test
    @DisplayName("getZoneLogs: Should throw InvalidInputException when start date is after end date (business rule)")
    void testGetZoneLogs_InvalidDateRange() {
        // Given
        String zoneId = "Z001";
        LocalDate startDate = LocalDate.of(2024, 1, 5);
        LocalDate endDate = LocalDate.of(2024, 1, 1); // Invalid range

        // When & Then
        InvalidInputException exception = assertThrows(InvalidInputException.class, () -> {
            wasteLogService.getZoneLogs(zoneId, startDate, endDate);
        });

        assertEquals(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE, exception.getMessage());
        // No change needed here as `never()` implicitly doesn't care about argument matchers for non-interaction
        verify(wasteLogRepository, never()).findByZoneIdAndCollectionStartTimeBetween(any(), any(), any());
    }

    // No tests here for malformed Zone ID, as the controller with @Pattern handles it.

    // --- getVehicleLogs Tests ---

    @Test
    @DisplayName("getVehicleLogs: Should retrieve report for a vehicle with multiple completed logs")
    void testGetVehicleLogs_Success_MultipleLogs() {
        // Given
        String vehicleId = "PT001";
        LocalDate startDate = LocalDate.of(2025, 6, 1);
        LocalDate endDate = LocalDate.of(2025, 6, 5);

        WasteLog log1 = new WasteLog();
        log1.setLogId(10L); log1.setZoneId("Z001"); log1.setVehicleId("PT001"); log1.setWorkerId("W010");
        log1.setCollectionStartTime(LocalDateTime.of(2025, 6, 1, 9, 0)); log1.setCollectionEndTime(LocalDateTime.of(2025, 6, 1, 10, 0));
        log1.setWeightCollected(200.0); log1.setCreatedDate(LocalDateTime.now());

        WasteLog log2 = new WasteLog();
        log2.setLogId(11L); log2.setZoneId("Z002"); log2.setVehicleId("PT001"); log2.setWorkerId("W011");
        log2.setCollectionStartTime(LocalDateTime.of(2025, 6, 3, 10, 0)); log2.setCollectionEndTime(LocalDateTime.of(2025, 6, 3, 11, 0));
        log2.setWeightCollected(120.0); log2.setCreatedDate(LocalDateTime.now());

        WasteLog log3 = new WasteLog();
        log3.setLogId(12L); log3.setZoneId("Z001"); log3.setVehicleId("PT001"); log3.setWorkerId("W012");
        log3.setCollectionStartTime(LocalDateTime.of(2025, 6, 5, 11, 0)); log3.setCollectionEndTime(LocalDateTime.of(2025, 6, 5, 12, 0));
        log3.setWeightCollected(180.0); log3.setCreatedDate(LocalDateTime.now());

        // An uncompleted log for the vehicle (collectionEndTime is null), should not be included
        WasteLog logUncompleted = new WasteLog();
        logUncompleted.setLogId(13L); logUncompleted.setZoneId("Z001"); logUncompleted.setVehicleId("PT001"); logUncompleted.setWorkerId("W013");
        logUncompleted.setCollectionStartTime(LocalDateTime.of(2025, 6, 4, 14, 0)); logUncompleted.setCollectionEndTime(null);
        logUncompleted.setWeightCollected(0.0); logUncompleted.setCreatedDate(LocalDateTime.now());

        when(wasteLogRepository.findByVehicleIdAndCollectionStartTimeBetween(
            eq(vehicleId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(log1, log2, log3, logUncompleted));

        // When
        List<VehicleReportDto> reports = wasteLogService.getVehicleLogs(vehicleId, startDate, endDate);

        // Then
        assertNotNull(reports);
        assertEquals(3, reports.size()); // Expect 3 completed logs

        // Verify reports are sorted by date
        assertEquals(LocalDate.of(2025, 6, 1), reports.get(0).getCollectionDate());
        assertEquals(LocalDate.of(2025, 6, 3), reports.get(1).getCollectionDate());
        assertEquals(LocalDate.of(2025, 6, 5), reports.get(2).getCollectionDate());

        assertEquals(vehicleId, reports.get(0).getVehicleId());
        assertEquals("Z001", reports.get(0).getZoneId());
        assertEquals(200.0, reports.get(0).getWeightCollected());

        assertEquals(vehicleId, reports.get(1).getVehicleId());
        assertEquals("Z002", reports.get(1).getZoneId());
        assertEquals(120.0, reports.get(1).getWeightCollected());

        assertEquals(vehicleId, reports.get(2).getVehicleId());
        assertEquals("Z001", reports.get(2).getZoneId());
        assertEquals(180.0, reports.get(2).getWeightCollected());

        // *** FIX APPLIED HERE ***
        verify(wasteLogRepository, times(1)).findByVehicleIdAndCollectionStartTimeBetween(
            eq(vehicleId), eq(startDate.atStartOfDay()), eq(endDate.atTime(LocalTime.MAX)) // All arguments are now matchers
        );
    }

    @Test
    @DisplayName("getVehicleLogs: Should return an empty list when no completed logs exist for the vehicle")
    void testGetVehicleLogs_NoCompletedLogs() {
        // Given
        String vehicleId = "RT999";
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);

        when(wasteLogRepository.findByVehicleIdAndCollectionStartTimeBetween(
            eq(vehicleId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        List<VehicleReportDto> reports = wasteLogService.getVehicleLogs(vehicleId, startDate, endDate);

        // Then
        assertNotNull(reports);
        assertTrue(reports.isEmpty());

        // *** FIX APPLIED HERE ***
        verify(wasteLogRepository, times(1)).findByVehicleIdAndCollectionStartTimeBetween(
            eq(vehicleId), eq(startDate.atStartOfDay()), eq(endDate.atTime(LocalTime.MAX)) // All arguments are now matchers
        );
    }

    @Test
    @DisplayName("getVehicleLogs: Should throw InvalidInputException when start date is after end date (business rule)")
    void testGetVehicleLogs_InvalidDateRange() {
        // Given
        String vehicleId = "RT001";
        LocalDate startDate = LocalDate.of(2025, 2, 10);
        LocalDate endDate = LocalDate.of(2025, 2, 5); // Invalid range

        // When & Then
        InvalidInputException exception = assertThrows(InvalidInputException.class, () -> {
            wasteLogService.getVehicleLogs(vehicleId, startDate, endDate);
        });

        assertEquals(WasteLogConstants.END_DATE_CANNOT_BE_BEFORE_START_DATE, exception.getMessage());
        // No change needed here for `never()`
        verify(wasteLogRepository, never()).findByVehicleIdAndCollectionStartTimeBetween(any(), any(), any());
    }
}