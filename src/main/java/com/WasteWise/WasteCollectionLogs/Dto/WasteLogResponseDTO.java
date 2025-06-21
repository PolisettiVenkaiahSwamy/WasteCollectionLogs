package com.WasteWise.WasteCollectionLogs.Dto;

import java.time.LocalDateTime;

import com.WasteWise.WasteCollectionLogs.Model.WasteLog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class WasteLogResponseDTO {
 
	 private Long logId;
	 private String zoneId;
     private String vehicleId;
	 private String workerId;
	 private LocalDateTime collectionStartTime;
	 private LocalDateTime collectionEndTime;
	 private Double weightCollected;
	 private String message;
	 private LocalDateTime createdDate; 
	    private LocalDateTime updatedDate;
	 
	 
	public WasteLogResponseDTO(Long logId, String message) {
		this.logId=logId;
		this.message=message;
	}
	public WasteLogResponseDTO(String message) {
			this.message=message;
		}
	
	public WasteLogResponseDTO(WasteLog wasteLog) {
		    this.logId = wasteLog.getLogId();
	        this.zoneId = wasteLog.getZoneId();
	        this.vehicleId = wasteLog.getVehicleId();
	        this.workerId = wasteLog.getWorkerId();
	        this.collectionStartTime = wasteLog.getCollectionStartTime();
	        this.collectionEndTime = wasteLog.getCollectionEndTime();
	        this.weightCollected = wasteLog.getWeightCollected();
	        this.message = null;
	        this.createdDate = wasteLog.getCreatedDate();
	        this.updatedDate = wasteLog.getUpdatedDate();
	}
}
