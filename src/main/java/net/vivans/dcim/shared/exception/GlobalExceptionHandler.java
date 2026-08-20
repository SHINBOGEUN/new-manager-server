package net.vivans.dcim.shared.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.influxdb.exceptions.UnauthorizedException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> httpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("HttpMessageNotReadableException: {}", e.getMessage(), e);

        String message = "Invalid JSON format in request body";
        if (e.getCause() instanceof JsonParseException) {
            message = "JSON syntax error in request body";
        } else if (e.getCause() instanceof JsonMappingException) {
            message = "JSON mapping error - invalid field values";
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, message, "Please check your request format"));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> unauthorizedExceptionHandle(UnauthorizedException e) {
        log.error("InfluxDB UnauthorizedException: {}", e, e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(401, "Please Check InfluxDB Server", e.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Object>> invalidTokenExceptionHandle(InvalidTokenException e) {
        log.error("InvalidTokenException: {}", e, e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(401, "Please Check your token", e.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> badCredentialsExceptionHandle(BadCredentialsException e) {
        log.error("BadCredentialsException: {}", e.getMessage(), e);
        return ResponseEntity.status(401).body(ApiResponse.error(401, "Please check your credentials", "Invalid username or password"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException: {}", e.getMessage(), e);

        FieldError fieldError = (FieldError) e.getBindingResult().getAllErrors().get(0);
        String message = String.format("Invalid value for parameter '%s'", fieldError.getField());
        return ResponseEntity.badRequest().body(ApiResponse.error(400, message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> missingServletRequestParameterExceptionHandler(MissingServletRequestParameterException e) {
        log.error("MissingServletRequestParameterException: {}", e, e);
        String message = String.format("Required parameter '%s' (%s) is missing", e.getParameterName(), e.getParameterType());
        return ResponseEntity.badRequest().body(ApiResponse.error(400, message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException e) {
        log.error("MethodArgumentTypeMismatchException: {}", e, e);
        String message = String.format("Invalid value for parameter '%s'", e.getName());
        return ResponseEntity.badRequest().body(ApiResponse.error(400, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> illegalArgumentExceptionHandler(IllegalArgumentException e) {
        log.error("IllegalArgumentException: {}", e, e);
        return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Object>> conflictExceptionHandler(ConflictException e) {
        log.warn("ConflictException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, e.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> entityNotFoundExceptionHandler(EntityNotFoundException e) {
        log.warn("EntityNotFoundException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> dataIntegrityViolationExceptionHandler(DataIntegrityViolationException e) {
        log.warn("DataIntegrityViolationException: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "duplicate or constraint violation"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> noResourceFoundExceptionHandler(NoResourceFoundException e) {
        log.warn("NoResourceFoundException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, "Resource not found", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> exceptionHandler(Exception e) {
        log.error("Exception: {}", e, e);
        return ResponseEntity.internalServerError().body(ApiResponse.error(500, e.getMessage()));
    }
}
