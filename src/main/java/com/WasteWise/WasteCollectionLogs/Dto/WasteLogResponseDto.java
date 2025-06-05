package com.WasteWise.WasteCollectionLogs.Dto;

import java.time.LocalDateTime;

import com.WasteWise.WasteCollectionLogs.Model.WasteLog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class WasteLogResponseDto {
 
	 private String logId;
	 private String zoneId;
     private String vehicleId;
	 private String workerId;
	 private LocalDateTime collectionStartTime;
	 private LocalDateTime collectionEndTime;
	 private Double weightCollected;
	 private String message;
	 
	 
	public WasteLogResponseDto(String message, String logId) {
		this.message=message;
		this.logId=logId;
	}
	public WasteLogResponseDto(String message) {
			this.message=message;
		}
	
	public WasteLogResponseDto(WasteLog wasteLog) {
		    this.logId = wasteLog.getLogId();
	        this.zoneId = wasteLog.getZoneId();
	        this.vehicleId = wasteLog.getVehicleId();
	        this.workerId = wasteLog.getWorkerId();
	        this.collectionStartTime = wasteLog.getCollectionStartTime();
	        this.collectionEndTime = wasteLog.getCollectionEndTime();
	        this.weightCollected = wasteLog.getWeightCollected();
	        this.message = null;
	}
}
