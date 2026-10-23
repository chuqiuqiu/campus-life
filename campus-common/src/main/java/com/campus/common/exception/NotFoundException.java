package com.campus.common.exception;

/**
 * 资源不存在（404）
 */
public class NotFoundException extends CommonException {

    public NotFoundException(String message) {
        super(message, 404);
    }
}
