package com.withkash.kash.exceptions;

/**
 * Created by geoff on 2015-07-15.
 */
public class UnexpectedErrorException extends Exception {
    public UnexpectedErrorException() { super(); }
    public UnexpectedErrorException(String message) { super(message); }
    public UnexpectedErrorException(String message, Throwable cause) { super(message, cause); }
    public UnexpectedErrorException(Throwable cause) { super(cause); }
}

