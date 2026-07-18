package com.apiplatform.exception;

/**
 * Thrown when an authenticated user attempts to access or modify a resource
 * they do not own (Broken Object Level Authorization guard).
 * Mapped to HTTP 403 by {@link GlobalExceptionHandler}.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }

    public static ForbiddenOperationException notOwner(String entity) {
        return new ForbiddenOperationException("You do not have access to this " + entity + ".");
    }
}
