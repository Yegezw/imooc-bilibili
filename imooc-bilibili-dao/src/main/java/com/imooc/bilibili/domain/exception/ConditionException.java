package com.imooc.bilibili.domain.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConditionException extends RuntimeException {

    private static final long serialVersionUid = 1L;

    private String code;

    public ConditionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ConditionException(String message) {
        super(message);
        code = "500";
    }
}
