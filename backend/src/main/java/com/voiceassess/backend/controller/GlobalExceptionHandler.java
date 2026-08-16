package com.voiceassess.backend.controller;

import com.voiceassess.backend.model.SystemErrorLog;
import com.voiceassess.backend.repository.SystemErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;

/**
 * One place for error responses so controllers don't each reinvent them.
 * 500s get written to the error log table (visible on the admin logs page)
 * with a sanitized message — no stack traces, no secrets, no internals.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final SystemErrorLogRepository errorLogRepo;

    public GlobalExceptionHandler(SystemErrorLogRepository errorLogRepo) {
        this.errorLogRepo = errorLogRepo;
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<?> handleMultipartException(MultipartException e) {
        log.warn("Multipart upload failed: {}", e.getMessage());
        return ResponseEntity.status(400).body(Map.of(
            "error", "Invalid file upload. The file may be too large or the wrong type."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.status(400).body(Map.of("error", "Missing parameter: " + e.getParameterName()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAll(Exception e) {
        log.error("Unhandled exception", e);

        // persist for the admin logs page — message only, the stack trace stays server-side
        try {
            var entry = new SystemErrorLog();
            entry.setErrorType(e.getClass().getSimpleName());
            entry.setErrorMessage(String.valueOf(e.getMessage()));
            entry.setRelatedEntity(null);
            errorLogRepo.save(entry);
        } catch (Exception logErr) {
            // never let logging break the actual response
            log.error("Failed to persist error log entry", logErr);
        }

        return ResponseEntity.status(500).body(Map.of(
            "error", "Something went wrong on the server. Try again."));
    }
}
