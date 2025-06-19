package com.WasteWise.WasteCollectionLogs.Model;

import java.time.LocalDateTime;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="waste_log")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WasteLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "log_id")
	private Long logId;
	
	@Column(name="zone_id", nullable = false)
	private String zoneId;
	
	@Column(name="vehicle_id", nullable = false)
	private String vehicleId;
	
	@Column(name="worker_id", nullable = false)
	private String workerId;
	
	@Column(name="collection_start_time", nullable = false)
	private LocalDateTime collectionStartTime;
	
	@Column(name="collection_end_time")
	private LocalDateTime collectionEndTime;
	
	@Column(name="weight_collected")
	private Double weightCollected;
	
	
	@Column(name="created_date", nullable = false)
	 private LocalDateTime createdDate;
	
	@Column(name="created_by")
	    private String createdBy;
	
	@Column(name="updated_date")
	    private LocalDateTime updatedDate;
	
	@Column(name="updated_by")
	    private String updatedBy;
	
}
