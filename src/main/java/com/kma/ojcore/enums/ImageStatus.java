package com.kma.ojcore.enums;

/**
 * Enum để quản lý trạng thái của ảnh trong hệ thống
 */
public enum ImageStatus {
    /**
     * Ảnh vừa được upload, chưa được commit vào Problem
     * Sẽ bị xóa tự động sau 24 giờ nếu không được commit
     */
    TEMPORARY,

    /**
     * Ảnh đã được gắn vào Problem và đang được sử dụng
     */
    COMMITTED,

    /**
     * Ảnh đã bị xóa (soft delete)
     */
    DELETED
}
