package com.withkash.kash.exceptions;

/**
 * Created by geoff on 2015-07-15.
 */
public class RelinkRequiredException extends Exception{
    public RelinkRequiredException() { super(); }
    public RelinkRequiredException(String message) { super(message); }
    public RelinkRequiredException(String message, Throwable cause) { super(message, cause); }
    public RelinkRequiredException(Throwable cause) { super(cause); }
}
