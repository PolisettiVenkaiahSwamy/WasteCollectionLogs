package com.WasteWise.WasteCollectionLogs.Handler;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class LogAlreadyCompletedException extends RuntimeException{

	
	 
	public LogAlreadyCompletedException(String message) {
        super(message);
    }
    public LogAlreadyCompletedException(String message, Throwable cause) {
        super(message, cause);
    }
}
