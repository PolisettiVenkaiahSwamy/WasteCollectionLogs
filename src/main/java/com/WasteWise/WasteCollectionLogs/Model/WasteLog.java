package com.WasteWise.WasteCollectionLogs.Model;

import java.time.LocalDateTime;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;	
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
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "log_id")
	private String logId;
	@Column(name="zone_id")
	private String zoneId;
	@Column(name="vehicle_id")
	private String vehicleId;
	@Column(name="worker_id")
	private String workerId;
	@Column(name="collection_start_time")
	private LocalDateTime collectionStartTime;
	@Column(name="collection_end_time")
	private LocalDateTime collectionEndTime;
	@Column(name="weight_collected")
	private Double weightCollected;
	
	
	@Column(name="created_date")
	 private LocalDateTime createdDate;
	
	@Column(name="created_by")
	    private String createdBy;
	
	@Column(name="updated_date")
	    private LocalDateTime updatedDate;
	
	@Column(name="updated_by")
	    private String updatedBy;
	
}
