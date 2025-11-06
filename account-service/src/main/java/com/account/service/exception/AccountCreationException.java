package com.account.service.exception;

public class AccountCreationException extends  RuntimeException{
    public AccountCreationException(String message, Throwable cause) {
        super(message, cause);
    }

}
