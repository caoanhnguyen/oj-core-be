package com.kma.ojcore.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ==========================================
    // 1. SYS - SYSTEM
    // ==========================================
    UNCATEGORIZED_EXCEPTION("SYS_001", "Lỗi hệ thống không xác định.", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_FAILED("SYS_002", "Dữ liệu đầu vào không hợp lệ.", HttpStatus.BAD_REQUEST),
    METHOD_ARGUMENT_TYPE_MISMATCH("SYS_003", "Kiểu đối số không hợp lệ.", HttpStatus.BAD_REQUEST),
    MISSING_REQUEST_PARAMETER("SYS_004", "Thiếu tham số bắt buộc.", HttpStatus.BAD_REQUEST),

    // ==========================================
    // 2. AUTH - AUTHENTICATION/AUTHORIZATION
    // ==========================================
    UNAUTHENTICATED("AUTH_001", "Chưa xác thực. Vui lòng đăng nhập.", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("AUTH_002", "Không có quyền truy cập.", HttpStatus.FORBIDDEN),
    WRONG_CREDENTIALS("AUTH_003", "Tên đăng nhập hoặc mật khẩu không chính xác.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("AUTH_004", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("AUTH_005", "Token không hợp lệ hoặc đã bị thu hồi.", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED("AUTH_006", "Tài khoản đang bị khóa. Vui lòng liên hệ quản trị viên.", HttpStatus.FORBIDDEN),
    EMAIL_NOT_VERIFIED("AUTH_007", "Email chưa được xác thực.", HttpStatus.FORBIDDEN),

    // ==========================================
    // 3. USR - USER
    // ==========================================
    USER_NOT_FOUND("USR_001", "Không tìm thấy người dùng.", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USR_002", "Tên đăng nhập đã tồn tại.", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS("USR_003", "Email này đã được sử dụng.", HttpStatus.CONFLICT),
    CANNOT_MODIFY_SELF("USR_004", "Bạn không thể tự thay đổi trạng thái hoặc quyền của chính mình.", HttpStatus.FORBIDDEN),

    // ==========================================
    // 4. PRB - PROBLEM & TOPIC
    // ==========================================
    PROBLEM_NOT_FOUND("PRB_001", "Không tìm thấy Problem.", HttpStatus.NOT_FOUND),
    PROBLEM_ALREADY_EXISTS("PRB_002", "Slug của Problem đã tồn tại.", HttpStatus.CONFLICT),
    PROBLEM_IN_USE("PRB_003", "Problem đang được sử dụng trong Contest và không thể bị xóa.", HttpStatus.BAD_REQUEST),
    TOPIC_NOT_FOUND("TOP_001", "Không tìm thấy Topic.", HttpStatus.NOT_FOUND),
    TOPIC_ALREADY_EXISTS("TOP_002", "Tên hoặc Slug của Topic đã tồn tại.", HttpStatus.CONFLICT),

    // ==========================================
    // 5. SUB - SUBMISSION
    // ==========================================
    SUBMISSION_NOT_FOUND("SUB_001", "Không tìm thấy Submission.", HttpStatus.NOT_FOUND),
    LANGUAGE_NOT_SUPPORTED("SUB_002", "Ngôn ngữ lập trình không được hỗ trợ cho Problem này.", HttpStatus.BAD_REQUEST),
    SUBMISSION_LIMIT_EXCEEDED("SUB_003", "Vượt quá giới hạn tần suất nộp bài.", HttpStatus.TOO_MANY_REQUESTS),
    RUN_CODE_IN_PROGRESS("SUB_004", "Kết quả Run code đang được xử lý hoặc đã hết hạn.", HttpStatus.NOT_FOUND),

    // ==========================================
    // 7. CON - CONTEST
    // ==========================================
    CONTEST_NOT_FOUND("CON_001", "Không tìm thấy Contest.", HttpStatus.NOT_FOUND),
    CONTEST_NOT_STARTED("CON_002", "Contest chưa bắt đầu.", HttpStatus.FORBIDDEN),
    ALREADY_REGISTERED("CON_003", "Bạn đã đăng ký tham gia Contest này rồi.", HttpStatus.BAD_REQUEST),
    INCORRECT_PASSWORD("CON_004", "Mật khẩu Contest không chính xác.", HttpStatus.FORBIDDEN),
    NOT_REGISTERED("CON_005", "Bạn phải đăng ký để có thể xem các Problem này.", HttpStatus.FORBIDDEN),
    BANNED_FROM_CONTEST("CON_006", "Bạn đã bị cấm tham gia Contest này.", HttpStatus.FORBIDDEN),
    SCOREBOARD_HIDDEN("CON_007", "Scoreboard của Contest hiện đang bị ẩn.", HttpStatus.FORBIDDEN),
    RESOURCE_ACCESS_DENIED("CON_008", "Tài nguyên của Contest đang bị khóa theo cấu hình hiển thị hiện tại.", HttpStatus.FORBIDDEN),

    // ==========================================
    // 6. FIL - FILE/STORAGE
    // ==========================================
    FILE_UPLOAD_FAILED("FIL_001", "Tải tệp lên thất bại.", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_TOO_LARGE("FIL_002", "Kích thước tệp vượt quá giới hạn cho phép.", HttpStatus.PAYLOAD_TOO_LARGE),
    INVALID_FILE_FORMAT("FIL_003", "Định dạng tệp không được hỗ trợ.", HttpStatus.BAD_REQUEST),
    TESTCASE_NOT_FOUND("FIL_004", "Không tìm thấy tệp Testcase.", HttpStatus.NOT_FOUND),
    FILE_EMPTY("FIL_005", "Tệp tải lên bị trống.", HttpStatus.BAD_REQUEST),
    FILE_NOT_FOUND("FIL_006", "Không tìm thấy tệp trong bộ lưu trữ.", HttpStatus.NOT_FOUND),
    INVALID_TESTCASE_ARCHIVE("FIL_007", "Tệp nén không chứa các Testcase (.in/.out) hợp lệ.", HttpStatus.BAD_REQUEST),
    MISSING_OUTPUT_FILE("FIL_008", "Thiếu tệp output tương ứng.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(String code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}