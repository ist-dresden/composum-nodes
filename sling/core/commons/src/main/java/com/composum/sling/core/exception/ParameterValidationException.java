package com.composum.sling.core.exception;

/**
 * the exception to handle bad parameter values thrown by servlet operations
 */
public class ParameterValidationException extends Exception {

    public ParameterValidationException(String message) {
        super (message);
    }
}
