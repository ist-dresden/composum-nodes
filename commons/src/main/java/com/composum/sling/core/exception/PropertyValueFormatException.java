package com.composum.sling.core.exception;

/**
 * the exception to handle bad property values values thrown by servlet operations
 */
public class PropertyValueFormatException extends Exception {

    public PropertyValueFormatException(String message) {
        super (message);
    }
}
