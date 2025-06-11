package com.WasteWise.WasteCollectionLogs.Dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WasteLogUpdateRequestDto {
	private Long logId;
	private Double weightCollected;

}
