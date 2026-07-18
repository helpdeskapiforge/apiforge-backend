package com.apiplatform.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Consistent error envelope returned by every failed API call.
 *
 * Example:
 * {
 *   "timestamp": "2026-07-06T10:15:30Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Workspace not found with id: 42",
 *   "path": "/api/workspaces/42",
 *   "fieldErrors": null
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError ofValidation(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
    }
}
