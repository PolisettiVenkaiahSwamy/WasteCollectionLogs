package com.WasteWise.WasteCollectionLogs.Dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WasteLogStartRequestDTO {

    @NotBlank(message = "Zone ID cannot be empty.")
    @Pattern(regexp = "^Z\\d{3}$", message = "Invalid Zone ID format. Must be like Z001.")
    private String zoneId;

    @NotBlank(message = "Vehicle ID cannot be empty.")
    @Pattern(regexp = "^(RT|PT)\\d{3}$", message = "Invalid Vehicle ID format. Must be like RT123 or PT123.")
    private String vehicleId;

    @NotBlank(message = "Worker ID cannot be empty.")
    @Pattern(regexp = "^W\\d{3}$", message = "Invalid Worker ID format. Must be like W456.")
    private String workerId;
}