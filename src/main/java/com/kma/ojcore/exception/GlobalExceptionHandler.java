package com.kma.ojcore.exception;


import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.kma.ojcore.dto.response.common.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.hibernate.query.SemanticException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            WebRequest request
    ) {
        String paramName = ex.getName();
        String expectedType = (ex.getRequiredType() != null) ? ex.getRequiredType().getSimpleName() : "valid type";

        String message = String.format("Parameter '%s' must be of type '%s'", paramName, expectedType);

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .error("Invalid Parameter Type")
                .message(message)
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String message;

            if (error.isBindingFailure() || "typeMismatch".equals(error.getCode())) {
                if (error.getField().contains("Date")) {
                    message = "Invalid date format";
                }
                else if (error.getField().contains("gender") || error.getField().contains("status")) {
                    message = "Invalid enum value";
                }
                else {
                    message = String.format("Invalid type for field '%s' with value '%s'", error.getField(), error.getRejectedValue());
                }
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
                .path(request.getDescription(false).replace("uri=", ""))
                .error("Invalid Payload")
                .message(errorMessage)
                .build();
    }

    /**
     * Xử lý lỗi validation cho @PathVariable và @RequestParam.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {

        String errorMessage = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .error("Invalid Parameter")
                .message(errorMessage)
                .build();
    }

    /**
     * Xử lý lỗi thiếu @RequestParam.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingRequestParam(MissingServletRequestParameterException ex, WebRequest request) {

        String errorMessage = String.format("Parameter '%s' is missing", ex.getParameterName());

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .error("Invalid Parameter")
                .message(errorMessage)
                .build();
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHandlerMethodValidation(HandlerMethodValidationException ex, WebRequest request) {
        // Lấy tất cả ConstraintViolation từ tất cả ParameterValidationResult
        String errorMessage = ex.getAllErrors().stream()
                .map(err -> {
                    if (err instanceof FieldError fieldError) {
                        String message = fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value";
                        return fieldError.getField() + ": " + message;
                    } else if (err instanceof ObjectError objectError) {
                        String message = objectError.getDefaultMessage() != null ? objectError.getDefaultMessage() : "Invalid value";
                        return objectError.getObjectName() + ": " + message;
                    } else {
                        return err.getDefaultMessage() != null ? err.getDefaultMessage() : "Invalid value";
                    }
                })
                .collect(Collectors.joining("; "));

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .error("Invalid Payload")
                .message(errorMessage)
                .build();
    }


    // Xử lý ResourceNotFoundException, khi không tìm thấy tài nguyên
    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFoundException(Exception e, WebRequest request) {

        String message = e.getMessage() != null ? e.getMessage() : "Resource not found";

        return ErrorResponse.builder()
                .timestamp(new Date())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(message)
                .status(NOT_FOUND.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
    }

    // Xử lý ResourceAlreadyExistsException, khi tài nguyên đã tồn tại
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleResourceAlreadyExists(ResourceAlreadyExistsException e, WebRequest request) {

        String message = e.getMessage() != null ? e.getMessage() : "Resource already exists";

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(CONFLICT.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .error(CONFLICT.getReasonPhrase())
                .message(message)
                .build();
    }

    // Xử lý InvalidDataException, khi dữ liệu không hợp lệ
    @ExceptionHandler(InvalidDataException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateKeyException(InvalidDataException e, WebRequest request) {

        String message = e.getMessage() != null ? e.getMessage() : "Invalid data";

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(CONFLICT.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .error(CONFLICT.getReasonPhrase())
                .message(message)
                .build();
    }

    @ExceptionHandler(SemanticException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleDatabaseExceptions(Exception e, WebRequest request) {

        String message = e.getMessage() != null ? e.getMessage() : "Database error occurred";

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(message)
                .build();
    }

    @ExceptionHandler(StorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleStorageException(StorageException e, WebRequest request) {

        String message = e.getMessage() != null ? e.getMessage() : "Storage error occurred";

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(message)
                .build();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            WebRequest request
    ) {
        Throwable cause = ex.getCause();
        String message = "Invalid JSON format";

        // Check xem có phải lỗi do sai định dạng dữ liệu (Date, Enum, int...) không
        if (cause instanceof InvalidFormatException invalidEx) {
            // Nếu kiểu dữ liệu bị sai là LocalDate -> Trả lỗi ngày tháng
            if (invalidEx.getTargetType().equals(java.time.LocalDate.class)) {
                message = "Invalid date format";
            }
            // Nếu kiểu dữ liệu bị sai là Enum -> Trả lỗi Enum
            else if (invalidEx.getTargetType().isEnum()) {
                message = "Invalid enum value";
            }
            else {
                message = "Invalid data type";
            }
        }
        // Check lỗi thiếu trường bắt buộc khi deserialize (nếu dùng @JsonRequired)
        else if (cause instanceof MismatchedInputException) {
            message = "Missing required field or mismatched data type";
        }

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .error("Invalid Payload Format")
                .message(message)
                .build();
    }

    // Xử lý tất cả các ngoại lệ khác
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAll(Exception e, WebRequest request) {

        String message = e.getMessage() != null ? e.getMessage() : "Internal server error";

        return ErrorResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(message)
                .build();
    }
}
