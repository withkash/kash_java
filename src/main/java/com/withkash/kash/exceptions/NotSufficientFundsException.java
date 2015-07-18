package com.withkash.kash.exceptions;


public class NotSufficientFundsException extends Exception {
    public NotSufficientFundsException() { super(); }
    public NotSufficientFundsException(String message) { super(message); }
    public NotSufficientFundsException(String message, Throwable cause) { super(message, cause); }
    public NotSufficientFundsException(Throwable cause) { super(cause); }
}
