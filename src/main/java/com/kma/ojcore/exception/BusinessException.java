package com.kma.ojcore.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    // Cách 1: Chỉ truyền mã lỗi, lấy message mặc định từ Enum
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // Cách 2: Truyền mã lỗi và tự ghi đè message (dùng khi cần in log chi tiết biến động)
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}