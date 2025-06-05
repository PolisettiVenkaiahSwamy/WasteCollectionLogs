package com.WasteWise.WasteCollectionLogs.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZoneReportDto { 
    private String zoneId;
    private LocalDate date; 
    private Long numberOfVehiclesUsed; 
    private Double totalWeightCollectedKg;
}