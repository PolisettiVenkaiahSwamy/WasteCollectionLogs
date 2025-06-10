package com.WasteWise.WasteCollectionLogs.Handler;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NoReportDataFoundException extends RuntimeException{

	 public NoReportDataFoundException(String message) {
	        super(message);
	    }
	 
	 public NoReportDataFoundException(String message, Throwable cause) {
	        super(message, cause);
	    }
}
