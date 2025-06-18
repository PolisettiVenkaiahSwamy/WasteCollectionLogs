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
     * It returns an HTTP 404 Not Found status with a consistent ErrorResponse.
     *
     * @param ex The {@link ResourceNotFoundException} that was thrown.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponse} with details
     * and an HTTP status of {@code NOT_FOUND} (404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Resource Not Found :{}", ex.getMessage());
        // Creating a consistent ErrorResponse
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles {@link InvalidInputException} instances.
     * It returns an HTTP 400 Bad Request status with a consistent ErrorResponse.
     *
     * @param ex The {@link InvalidInputException} that was thrown.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponse} with details
     * and an HTTP status of {@code BAD_REQUEST} (400).
     */
    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInputException(InvalidInputException ex) {
        logger.warn("Invalid Input :{}", ex.getMessage());
        // Creating a consistent ErrorResponse
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles {@link LogAlreadyCompletedException} instances.
     * It returns an HTTP 409 Conflict status with a consistent ErrorResponse.
     *
     * @param ex The {@link LogAlreadyCompletedException} that was thrown.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponse} with details
     * about the conflict and an HTTP status of {@code CONFLICT} (409).
     */
    @ExceptionHandler(LogAlreadyCompletedException.class)
    public ResponseEntity<ErrorResponse> handleLogAlreadyCompletedException(LogAlreadyCompletedException ex) {
        logger.warn("Log Already Completed :{}", ex.getMessage()); // Added logger for consistency
        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

//     You might also consider a generic exception handler for unhandled exceptions
     @ExceptionHandler(Exception.class)
     public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
         logger.error("An unexpected error occurred: {}", ex.getMessage(), ex);
         ErrorResponse error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred. Please try again later.", LocalDateTime.now());
         return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
     }
}