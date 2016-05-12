package com.composum.sling.core.concurrent;

/**
 * Can be used by a JobExecutor to indocate an error during execution of the job.
 */
public class JobFailureException extends RuntimeException {

    public JobFailureException() {
    }

    public JobFailureException(String message) {
        super(message);
    }

    public JobFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobFailureException(Throwable cause) {
        super(cause);
    }

    public JobFailureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
