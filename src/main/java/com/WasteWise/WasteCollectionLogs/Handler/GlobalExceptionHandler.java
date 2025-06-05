package com.WasteWise.WasteCollectionLogs.Handler;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.WasteWise.WasteCollectionLogs.Dto.ErrorResponse;
import com.WasteWise.WasteCollectionLogs.Dto.WasteLogResponseDto;

@ControllerAdvice
public class GlobalExceptionHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<WasteLogResponseDto>handleResourceNotFoundException(ResourceNotFoundException ex){
		logger.warn("Resource Not Found :{}",ex.getMessage());
		return new ResponseEntity<>(new WasteLogResponseDto(ex.getMessage()),HttpStatus.NOT_FOUND);
	}
	@ExceptionHandler(InvalidInputException.class)
	public ResponseEntity<WasteLogResponseDto>handleInvalidInputException(InvalidInputException ex){
		logger.warn("Invalid Input :{}",ex.getMessage());
		return new ResponseEntity<>(new WasteLogResponseDto(ex.getMessage()),HttpStatus.BAD_REQUEST);
	}
	 @ExceptionHandler(LogAlreadyCompletedException.class)
	    public ResponseEntity<ErrorResponse> handleLogAlreadyCompletedException(LogAlreadyCompletedException ex) {
	        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage(), LocalDateTime.now());
	        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
	    }

}	
