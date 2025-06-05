package com.WasteWise.WasteCollectionLogs.Service;

import java.time.LocalDate;
import java.util.List;

import com.WasteWise.WasteCollectionLogs.Dto.VehicleReportDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogResponseDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogStartRequestDto;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogUpdateRequestDto;
import com.WasteWise.WasteCollectionLogs.Dto.ZoneReportDto;

public interface WasteLogService {
	
	 WasteLogResponseDto startCollection(WasteLogStartRequestDto request);
	 
	 WasteLogResponseDto endCollection(WasteLogUpdateRequestDto request);
	 
	 List<ZoneReportDto> getZoneDailySummary(String zoneId, LocalDate startDate, LocalDate endDate);
	 
	 List<VehicleReportDto> getVehicleLogs(String vehicleId, LocalDate startDate, LocalDate endDate);
	
}
