package com.WasteWise.WasteCollectionLogs.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class WasteLogStartRequestDto {

	private String zoneId;
	private String vehicleId;
	private String workerId;
}