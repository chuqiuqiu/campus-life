package com.campus.common.exception;

/**
 * 无权限（403）
 */
public class ForbiddenException extends CommonException {

    public ForbiddenException(String message) {
        super(message, 403);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause, 403);
    }

    public ForbiddenException(Throwable cause) {
        super(cause, 403);
    }
}
