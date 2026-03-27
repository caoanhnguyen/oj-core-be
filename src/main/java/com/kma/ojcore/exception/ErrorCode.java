package com.kma.ojcore.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ==========================================
    // 1. SYS - SYSTEM
    // ==========================================
    UNCATEGORIZED_EXCEPTION("SYS_001", "Uncategorized system error.", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_FAILED("SYS_002", "Invalid input data.", HttpStatus.BAD_REQUEST),
    METHOD_ARGUMENT_TYPE_MISMATCH("SYS_003", "Invalid argument type.", HttpStatus.BAD_REQUEST),
    MISSING_REQUEST_PARAMETER("SYS_004", "Missing required parameter.", HttpStatus.BAD_REQUEST),

    // ==========================================
    // 2. AUTH - AUTHENTICATION/AUTHORIZATION
    // ==========================================
    UNAUTHENTICATED("AUTH_001", "Unauthenticated. Please log in.", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("AUTH_002", "Unauthorized access.", HttpStatus.FORBIDDEN),
    WRONG_CREDENTIALS("AUTH_003", "Invalid username or password.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("AUTH_004", "Token expired. Please log in again.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("AUTH_005", "Invalid or revoked token.", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED("AUTH_006", "Account locked. Please contact administrator.", HttpStatus.FORBIDDEN),
    EMAIL_NOT_VERIFIED("AUTH_007", "Email not verified.", HttpStatus.FORBIDDEN),

    // ==========================================
    // 3. USR - USER
    // ==========================================
    USER_NOT_FOUND("USR_001", "User not found.", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USR_002", "Username already exists.", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS("USR_003", "Email already in use.", HttpStatus.CONFLICT),
    CANNOT_MODIFY_SELF("USR_004", "You cannot modify your own account status or roles.", HttpStatus.FORBIDDEN),

    // ==========================================
    // 4. PRB - PROBLEM & TOPIC
    // ==========================================
    PROBLEM_NOT_FOUND("PRB_001", "Problem not found.", HttpStatus.NOT_FOUND),
    PROBLEM_ALREADY_EXISTS("PRB_002", "Problem slug already exists.", HttpStatus.CONFLICT),
    TOPIC_NOT_FOUND("TOP_001", "Topic not found.", HttpStatus.NOT_FOUND),
    TOPIC_ALREADY_EXISTS("TOP_002", "Topic name or slug already exists.", HttpStatus.CONFLICT),

    // ==========================================
    // 5. SUB - SUBMISSION
    // ==========================================
    SUBMISSION_NOT_FOUND("SUB_001", "Submission not found.", HttpStatus.NOT_FOUND),
    LANGUAGE_NOT_SUPPORTED("SUB_002", "Programming language not supported for this problem.", HttpStatus.BAD_REQUEST),
    SUBMISSION_LIMIT_EXCEEDED("SUB_003", "Submission rate limit exceeded.", HttpStatus.TOO_MANY_REQUESTS),
    RUN_CODE_IN_PROGRESS("SUB_004", "Run code result is still processing or has expired.", HttpStatus.NOT_FOUND),

    // ==========================================
    // 7. CON - CONTEST
    // ==========================================
    CONTEST_NOT_FOUND("CON_001", "Contest not found.", HttpStatus.NOT_FOUND),
    CONTEST_NOT_STARTED("CON_002", "The contest has not started yet.", HttpStatus.FORBIDDEN),
    ALREADY_REGISTERED("CON_003", "You have already registered for this contest.", HttpStatus.BAD_REQUEST),
    INCORRECT_PASSWORD("CON_004", "Incorrect contest password.", HttpStatus.FORBIDDEN),
    NOT_REGISTERED("CON_005", "You must register to view these problems.", HttpStatus.FORBIDDEN),

    // ==========================================
    // 6. FIL - FILE/STORAGE
    // ==========================================
    FILE_UPLOAD_FAILED("FIL_001", "File upload failed.", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_TOO_LARGE("FIL_002", "File size exceeds the allowed limit.", HttpStatus.PAYLOAD_TOO_LARGE),
    INVALID_FILE_FORMAT("FIL_003", "Unsupported file format.", HttpStatus.BAD_REQUEST),
    TESTCASE_NOT_FOUND("FIL_004", "Testcase file not found.", HttpStatus.NOT_FOUND),
    FILE_EMPTY("FIL_005", "Uploaded file is empty.", HttpStatus.BAD_REQUEST),
    FILE_NOT_FOUND("FIL_006", "File not found in storage.", HttpStatus.NOT_FOUND),
    INVALID_TESTCASE_ARCHIVE("FIL_007", "ZIP file does not contain valid testcases (.in/.out).", HttpStatus.BAD_REQUEST),
    MISSING_OUTPUT_FILE("FIL_008", "Missing corresponding output file.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(String code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}