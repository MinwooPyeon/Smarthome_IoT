package com.eeum.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public interface ControllerHelper {
	default ResponseEntity<?> handleSuccess(Object data){
		return handleSuccess(data, HttpStatus.OK);
	}
	
	default ResponseEntity<?> handleFail(Exception e){
		return handleFail(e, HttpStatus.FORBIDDEN);
	}
	
	default ResponseEntity<?> handleSuccess(Object data, HttpStatus status){
		Map<String, Object> map = Map.of("status","SUCCESS","data", data);
		return ResponseEntity.status(status).body(map);
	}
	
	default ResponseEntity<?> handleFail(Exception e, HttpStatus status){
		Map<String, Object> map = Map.of("status","FAIL","error", e.getMessage());
		return ResponseEntity.status(status).body(map);
	}
}
