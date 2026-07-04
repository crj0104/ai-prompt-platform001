package com.course.promptplatform.common;

/**
 * 业务异常用于表示参数合法但业务规则不允许继续执行的场景。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
