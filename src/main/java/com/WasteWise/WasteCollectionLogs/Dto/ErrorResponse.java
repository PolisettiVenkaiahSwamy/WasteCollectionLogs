package com.WasteWise.WasteCollectionLogs.Dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ErrorResponse {
	private int status;
    private String message;
    private LocalDateTime timestamp;

}
