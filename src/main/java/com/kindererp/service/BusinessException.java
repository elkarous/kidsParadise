package com.kindererp.service;

import lombok.Getter;

/**
 * A rule violation the user can fix (duplicate payment, missing field, ...). Carries a translation
 * key and arguments so the UI can show it in the user's language.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String messageKey;
    private final transient Object[] args;

    public BusinessException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }
}
