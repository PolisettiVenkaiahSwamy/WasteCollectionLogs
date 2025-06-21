package com.WasteWise.WasteCollectionLogs.Service;

import java.time.LocalDate;
import java.util.List;

import com.WasteWise.WasteCollectionLogs.Dto.VehicleReportDTO;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogResponseDTO;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogStartRequestDTO;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogUpdateRequestDTO;
import com.WasteWise.WasteCollectionLogs.Dto.ZoneReportDTO;

public interface WasteLogService {
	
	 WasteLogResponseDTO startCollection(WasteLogStartRequestDTO request);
	 
	 WasteLogResponseDTO endCollection(WasteLogUpdateRequestDTO request);
	 
	 List<ZoneReportDTO> getZoneLogs(String zoneId, LocalDate startDate, LocalDate endDate);
	 
	 List<VehicleReportDTO> getVehicleLogs(String vehicleId, LocalDate startDate, LocalDate endDate);
	
}
