package com.WasteWise.WasteCollectionLogs.Dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehicleReportDTO {
	    private String vehicleId;
	    private String zoneId;
	    private Double weightCollected;
	    private LocalDate collectionDate; 
}
