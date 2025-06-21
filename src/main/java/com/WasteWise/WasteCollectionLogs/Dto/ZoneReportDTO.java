package com.WasteWise.WasteCollectionLogs.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZoneReportDTO { 
    private String zoneId;
    private LocalDate date; 
    private Long totalNumberOfCollections; 
    private Double totalWeightCollectedKg;
}