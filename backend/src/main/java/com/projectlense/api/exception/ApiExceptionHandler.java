package com.projectlense.api.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiProblem.class)
    ResponseEntity<?> apiProblem(ApiProblem e) {
        return ResponseEntity.status(e.status).body(Map.of("detail", e.getMessage()));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class,
            MethodArgumentNotValidException.class, ConstraintViolationException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<?> validation(Exception e) {
        List<Map<String, Object>> details = new ArrayList<>();
        if (e instanceof MethodArgumentNotValidException invalid) {
            for (FieldError field : invalid.getBindingResult().getFieldErrors()) {
                details.add(Map.of("type", "missing", "loc", List.of("body", field.getField()),
                        "msg", "Field required", "input", ""));
            }
        } else if (e instanceof MissingServletRequestPartException missing) {
            details.add(Map.of("type", "missing", "loc", List.of("body", missing.getRequestPartName()), "msg", "Field required"));
        } else if (e instanceof MissingServletRequestParameterException missing) {
            details.add(Map.of("type", "missing", "loc", List.of("query", missing.getParameterName()), "msg", "Field required"));
        } else {
            details.add(Map.of("type", "missing", "loc", List.of("body"), "msg", "Field required"));
        }
        return ResponseEntity.status(422).body(Map.of("detail", details));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> tooLarge(Exception e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("detail", "Uploaded file is too large"));
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<?> notFound(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> badRequest(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("detail", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<?> unavailable(Exception e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("detail", e.getMessage()));
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<?> badMultipart(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("detail", "Invalid multipart request"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e) {
        return ResponseEntity.status(500).body(Map.of("detail", e.getMessage() == null ? "Internal Server Error" : e.getMessage()));
    }

    public static class ApiProblem extends RuntimeException {
        final HttpStatus status;
        public ApiProblem(HttpStatus status, String message) { super(message); this.status = status; }
    }
}
