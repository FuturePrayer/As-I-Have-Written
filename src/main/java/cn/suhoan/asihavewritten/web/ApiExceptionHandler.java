package cn.suhoan.asihavewritten.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cn.suhoan.asihavewritten.log.LogEntryNotFoundException;

@RestControllerAdvice(basePackages = "cn.suhoan.asihavewritten.web")
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidApiKeyException.class)
    ResponseEntity<Map<String, String>> invalidApiKey() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid api key"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalidRequest(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "invalid request"));
    }

    @ExceptionHandler(LogEntryNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(LogEntryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
