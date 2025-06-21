package com.WasteWise.WasteCollectionLogs.Handler;

import java.time.LocalDateTime;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.WasteWise.WasteCollectionLogs.Dto.ErrorResponseDTO; // Assuming ErrorResponse DTO exists and matches structure

import jakarta.validation.ConstraintViolationException;

/**
 * Global exception handler for the WasteCollectionLogs application.
 * This class leverages Spring's {@code @ControllerAdvice} to provide centralized
 * exception handling across all {@code @Controller} classes, ensuring consistent
 * error responses for different types of exceptions.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
	/**
     * Handles {@link ResourceNotFoundException} instances.
     * It returns an HTTP 404 Not Found status with a consistent ErrorResponse.
     *
     * @param ex The {@link ResourceNotFoundException} that was thrown.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponseDTO} with details
     * and an HTTP status of {@code NOT_FOUND} (404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Resource Not Found :{}", ex.getMessage());
        ErrorResponseDTO error = new ErrorResponseDTO(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles {@link InvalidInputException} instances.
     * It returns an HTTP 400 Bad Request status with a consistent ErrorResponse.
     *
     * @param ex The {@link InvalidInputException} that was thrown.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponseDTO} with details
     * and an HTTP status of {@code BAD_REQUEST} (400).
     */
    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidInputException(InvalidInputException ex) {
        logger.warn("Invalid Input :{}", ex.getMessage());
        ErrorResponseDTO error = new ErrorResponseDTO(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles {@link LogAlreadyCompletedException} instances.
     * It returns an HTTP 409 Conflict status with a consistent ErrorResponse.
     *
     * @param ex The {@link LogAlreadyCompletedException} that was thrown.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponseDTO} with details
     * about the conflict and an HTTP status of {@code CONFLICT} (409).
     */
    @ExceptionHandler(LogAlreadyCompletedException.class)
    public ResponseEntity<ErrorResponseDTO> handleLogAlreadyCompletedException(LogAlreadyCompletedException ex) {
        logger.warn("Log Already Completed :{}", ex.getMessage());
        ErrorResponseDTO error = new ErrorResponseDTO(HttpStatus.CONFLICT.value(), ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Handles {@link MethodArgumentNotValidException} instances, which occur when
     * {@code @RequestBody} DTO validation fails. It extracts field errors and
     * returns an HTTP 400 Bad Request status with detailed validation messages.
     *
     * @param ex The {@link MethodArgumentNotValidException} that was thrown.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponseDTO} with details
     * and an HTTP status of {@code BAD_REQUEST} (400).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // It's good practice to explicitly state the status here too
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        if (errorMessage.isEmpty()) {
            errorMessage = "Validation failed for unknown reasons.";
        } else {
            errorMessage = "Validation failed: " + errorMessage;
        }

        logger.warn("Method Argument Not Valid Exception: {}", errorMessage); // More specific log message

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
            HttpStatus.BAD_REQUEST.value(),
            errorMessage,
            LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
    }
    /**
     * NEW: Handles {@link MethodArgumentTypeMismatchException} for malformed @RequestParam/@PathVariable values.
     * This will catch errors like invalid date formats or non-numeric input for numeric fields.
     *
     * @param ex The {@link MethodArgumentTypeMismatchException} that was thrown.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponseDTO} with details
     * and an HTTP status of {@code BAD_REQUEST} (400).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String parameterName = ex.getName();
        String invalidValue = (ex.getValue() != null) ? ex.getValue().toString() : "null";
        String requiredType = (ex.getRequiredType() != null) ? ex.getRequiredType().getSimpleName() : "unknown";
        String errorMessage = String.format("Parameter '%s' has invalid value '%s'. Expected type is '%s'.",
                                            parameterName, invalidValue, requiredType);
        
        logger.warn("Method Argument Type Mismatch Exception: {}", errorMessage);

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
            HttpStatus.BAD_REQUEST.value(),
            errorMessage,
            LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles {@link ConstraintViolationException} instances, which occur when
     * {@code @PathVariable} or {@code @RequestParam} validation fails
     * (when {@code @Validated} is on the controller class). It returns an HTTP 400 Bad Request
     * status with detailed validation errors.
     *
     * @param ex The {@link ConstraintViolationException} that was thrown.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponseDTO} with details
     * and an HTTP status of {@code BAD_REQUEST} (400).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolationException(ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        if (errorMessage.isEmpty()) {
            errorMessage = "Validation failed for path variables or request parameters.";
        } else {
            errorMessage = "Validation failed: " + errorMessage;
        }

        logger.warn("Constraint Violation Exception: {}", errorMessage); // More specific log message

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
            HttpStatus.BAD_REQUEST.value(),
            errorMessage,
            LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
    }
    /**
     * NEW: Handles {@link MissingServletRequestParameterException} for missing required @RequestParam.
     *
     * @param ex The {@link MissingServletRequestParameterException} that was thrown.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponseDTO} with details
     * and an HTTP status of {@code BAD_REQUEST} (400).
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponseDTO> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        String errorMessage = String.format("Required request parameter '%s' is not present.", ex.getParameterName());
        
        logger.warn("Missing Servlet Request Parameter Exception: {}", errorMessage);

        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
            HttpStatus.BAD_REQUEST.value(),
            errorMessage,
            LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
    }


    /**
     * Generic exception handler for any other unhandled exceptions.
     * It returns an HTTP 500 Internal Server Error status with a general message.
     *
     * @param ex The unexpected {@link Exception} that was thrown.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponseDTO} with a generic
     * error message and an HTTP status of {@code INTERNAL_SERVER_ERROR} (500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
        logger.error("An unexpected error occurred: {}", ex.getMessage(), ex); // Log full stack trace
        ErrorResponseDTO error = new ErrorResponseDTO(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred. Please try again later.", LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}