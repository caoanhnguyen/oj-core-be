package com.kma.ojcore.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.kma.ojcore.dto.response.common.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // =========================================================================
    // 1. XỬ LÝ LỖI NGHIỆP VỤ CHÍNH (BUSINESS EXCEPTION)
    // =========================================================================
    @ExceptionHandler(BusinessException.class)
    public ErrorResponse handleBusinessException(BusinessException e, WebRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(errorCode.getStatusCode().value())
                .errorCode(errorCode.getCode())
                .error(errorCode.getStatusCode().getReasonPhrase())
                .message(e.getMessage() != null ? e.getMessage() : errorCode.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
    }

    // =========================================================================
    // 2. XỬ LÝ LỖI SECURITY BỊ LỌT VÀO CONTROLLER
    // =========================================================================
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.FORBIDDEN.value())
                .errorCode(ErrorCode.UNAUTHORIZED.getCode())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(ErrorCode.UNAUTHORIZED.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.UNAUTHORIZED.value())
                .errorCode(ErrorCode.UNAUTHENTICATED.getCode())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(ErrorCode.UNAUTHENTICATED.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
    }

    // =========================================================================
    // 3. XỬ LÝ LỖI VALIDATION & PAYLOAD
    // =========================================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String message;
            if (error.isBindingFailure() || "typeMismatch".equals(error.getCode())) {
                message = "Invalid data format or type";
            } else {
                message = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value";
            }
            errors.put(error.getField(), message);
        });

        String errorMessage = errors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("; "));

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .path(request.getDescription(false).replace("uri=", ""))
                .error("Invalid Payload")
                .message(errorMessage)
                .build();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .path(request.getDescription(false).replace("uri=", ""))
                .error("Invalid Parameter")
                .message(errorMessage)
                .build();
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHandlerMethodValidation(HandlerMethodValidationException ex, WebRequest request) {
        String errorMessage = ex.getAllErrors().stream()
                .map(err -> {
                    if (err instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    } else if (err instanceof ObjectError objectError) {
                        return objectError.getObjectName() + ": " + objectError.getDefaultMessage();
                    }
                    return err.getDefaultMessage();
                })
                .collect(Collectors.joining("; "));

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .path(request.getDescription(false).replace("uri=", ""))
                .error("Invalid Payload")
                .message(errorMessage)
                .build();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        Throwable cause = ex.getCause();
        String message = "Invalid JSON format";

        if (cause instanceof InvalidFormatException invalidEx) {
            if (invalidEx.getTargetType().equals(java.time.LocalDate.class)) {
                message = "Invalid date format";
            } else if (invalidEx.getTargetType().isEnum()) {
                message = "Invalid enum value";
            } else {
                message = "Invalid data type";
            }
        } else if (cause instanceof MismatchedInputException) {
            message = "Missing required field or mismatched data type";
        }

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .path(request.getDescription(false).replace("uri=", ""))
                .error("Invalid Payload Format")
                .message(message)
                .build();
    }

    // =========================================================================
    // 4. XỬ LÝ LỖI THAM SỐ REQUEST (PARAMS/PATH VARIABLES)
    // =========================================================================
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String expectedType = (ex.getRequiredType() != null) ? ex.getRequiredType().getSimpleName() : "valid type";
        String message = String.format("Parameter '%s' must be of type '%s'", ex.getName(), expectedType);

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH.getCode())
                .path(request.getDescription(false).replace("uri=", ""))
                .error("Invalid Parameter Type")
                .message(message)
                .build();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingRequestParam(MissingServletRequestParameterException ex, WebRequest request) {
        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.MISSING_REQUEST_PARAMETER.getCode())
                .path(request.getDescription(false).replace("uri=", ""))
                .error("Missing Parameter")
                .message(String.format("Parameter '%s' is missing", ex.getParameterName()))
                .build();
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoResourceFound(NoResourceFoundException e, WebRequest request) {
        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.NOT_FOUND.value())
                .errorCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                .error("Resource Not Found")
                .message("The requested URL was not found on this server.")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
    }

    // =========================================================================
    // 5. CHỐT CHẶN CUỐI CÙNG (CÁC LỖI UNEXPECTED)
    // =========================================================================
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAll(Exception e, WebRequest request) {
        log.error("Unhandled exception occurred:", e);
        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                .path(request.getDescription(false).replace("uri=", ""))
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage()) // Ẩn lỗi thực tế
                .build();
    }
}