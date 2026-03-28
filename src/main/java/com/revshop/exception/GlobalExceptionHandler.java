package com.revshop.exception;

import com.revshop.dto.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource not found at path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        log.warn("Conflict at path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(
            ForbiddenOperationException ex,
            HttpServletRequest request
    ) {
        log.warn("Forbidden request at path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        log.warn("Bad request at path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ApiResponse<Void>> handleInternalServer(
            InternalServerException ex,
            HttpServletRequest request
    ) {
        log.error("Internal server error at path={} message={}", request.getRequestURI(), ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        log.warn("Validation failed at path={}", request.getRequestURI());
        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .toList();

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                errors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Constraint validation failed at path={}", request.getRequestURI());
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Constraint validation failed",
                request.getRequestURI(),
                errors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.warn("Malformed request body at path={}", request.getRequestURI(), ex);
        return buildError(HttpStatus.BAD_REQUEST, "Malformed request body", request.getRequestURI(), null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        log.warn("Max upload size exceeded at path={}", request.getRequestURI(), ex);
        return buildError(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Image is too large. Max allowed size is 10MB",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler({MultipartException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiResponse<Void>> handleMultipartError(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Multipart request error at path={}", request.getRequestURI(), ex);
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Invalid image upload request. Send multipart/form-data with a file field",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Data integrity violation at path={}", request.getRequestURI(), ex);
        String requestPath = request.getRequestURI() == null ? "" : request.getRequestURI();
        String rootMessage = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();

        String message = "Data conflict: resource already exists or violates constraints";
        if (requestPath.startsWith("/api/categories")) {
            message = "Category name already exists. Use a different name or edit existing category.";
        } else if (rootMessage.contains("ORA-00001")) {
            message = "Duplicate data detected. A record with same unique value already exists.";
        }
        return buildError(HttpStatus.CONFLICT, message, request.getRequestURI(), null);
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionSystem(
            TransactionSystemException ex,
            HttpServletRequest request
    ) {
        String message = "Transaction failed due to invalid data";
        log.warn("Transaction system exception at path={}", request.getRequestURI(), ex);
        return buildError(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception at path={}", request.getRequestURI(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request.getRequestURI(), null);
    }

    private ResponseEntity<ApiResponse<Void>> buildError(
            HttpStatus status,
            String message,
            String path,
            List<String> validationErrors
    ) {
        ApiResponse<Void> response = ApiResponse.<Void>error(
                message,
                status.value(),
                path,
                validationErrors
        );

        return ResponseEntity.status(status).body(response);
    }
}
