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

/**
 * Global exception handler for the WasteCollectionLogs application.
 * This class leverages Spring's {@code @ControllerAdvice} to provide centralized
 * exception handling across all {@code @Controller} classes, ensuring consistent
 * error responses for different types of exceptions.
 */
@ControllerAdvice // This annotation makes this class capable of handling exceptions thrown by any controller in the application.
public class GlobalExceptionHandler {
	
	// A logger instance to log messages and warnings related to exceptions.
	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
	/**
	 * Handles {@link ResourceNotFoundException} instances.
	 * This method is invoked when a requested resource (e.g., a specific waste log entry)
	 * cannot be located in the system. It returns an HTTP 404 Not Found status.
	 *
	 * @param ex The {@link ResourceNotFoundException} that was thrown.
	 * @return A {@link ResponseEntity} containing a {@link WasteLogResponseDto} with the
	 * error message and an HTTP status of {@code NOT_FOUND} (404).
	 */
	@ExceptionHandler(ResourceNotFoundException.class) // Specifies that this method handles ResourceNotFoundException.
	public ResponseEntity<WasteLogResponseDto>handleResourceNotFoundException(ResourceNotFoundException ex){
		// Logs a warning message indicating that a resource was not found, including the exception's message.
		logger.warn("Resource Not Found :{}",ex.getMessage());
		// Returns a response entity with the error message encapsulated in a DTO and an HTTP 404 status.
		return new ResponseEntity<>(new WasteLogResponseDto(ex.getMessage()),HttpStatus.NOT_FOUND);
	}

	/**
	 * Handles {@link InvalidInputException} instances.
	 * This method is invoked when the application receives input that is malformed,
	 * incomplete, or does not meet expected validation rules. It returns an HTTP 400 Bad Request status.
	 *
	 * @param ex The {@link InvalidInputException} that was thrown.
	 * @return A {@link ResponseEntity} containing a {@link WasteLogResponseDto} with the
	 * error message and an HTTP status of {@code BAD_REQUEST} (400).
	 */
	@ExceptionHandler(InvalidInputException.class) // Specifies that this method handles InvalidInputException.
	public ResponseEntity<WasteLogResponseDto>handleInvalidInputException(InvalidInputException ex){
		// Logs a warning message indicating that invalid input was received, including the exception's message.
		logger.warn("Invalid Input :{}",ex.getMessage());
		// Returns a response entity with the error message encapsulated in a DTO and an HTTP 400 status.
		return new ResponseEntity<>(new WasteLogResponseDto(ex.getMessage()),HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handles {@link LogAlreadyCompletedException} instances.
	 * This method is invoked when an operation is attempted on a waste log that has already
	 * been marked as completed, indicating a conflict in the resource's state. It returns an HTTP 409 Conflict status.
	 *
	 * @param ex The {@link LogAlreadyCompletedException} that was thrown.
	 * @return A {@link ResponseEntity} containing an {@link ErrorResponse} with details
	 * about the conflict and an HTTP status of {@code CONFLICT} (409).
	 */
	 @ExceptionHandler(LogAlreadyCompletedException.class) // Specifies that this method handles LogAlreadyCompletedException.
	    public ResponseEntity<ErrorResponse> handleLogAlreadyCompletedException(LogAlreadyCompletedException ex) {
	        // Creates a structured error response including the HTTP status code, message, and current timestamp.
	        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage(), LocalDateTime.now());
	        // Returns a response entity with the structured error response and an HTTP 409 status.
	        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
	    }
}