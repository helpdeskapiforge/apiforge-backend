package com.apiplatform.web.dto;

/**
 * Replaces the previous public static inner {@code EnvironmentReq} class that lived
 * inside the controller. Request/response shapes belong in their own dedicated,
 * reusable, testable types.
 */
public record EnvironmentRequest(String name, String variables, Long workspaceId) {
}
