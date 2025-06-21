package com.WasteWise.WasteCollectionLogs.Dto;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WasteLogUpdateRequestDTO {
	    @NotNull(message = "Log ID cannot be null.")
	    @Positive(message = "Log ID must be a positive number.")
	    private Long logId;

	    @NotNull(message = "Weight Collected cannot be null.")
	    @Positive(message = "Weight Collected must be a positive value.")
	    private Double weightCollected;

}
