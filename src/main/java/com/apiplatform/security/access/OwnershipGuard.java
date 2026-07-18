package com.apiplatform.security.access;

import com.apiplatform.exception.ForbiddenOperationException;
import com.apiplatform.model.User;
import com.apiplatform.model.Workspace;

/**
 * Enforces that the currently authenticated user owns a given workspace.
 * <p>
 * This is the single most important security fix in this codebase: previously,
 * every controller loaded any entity by its numeric ID with zero check that the
 * caller actually owned it (a textbook OWASP API-1 "Broken Object Level Authorization").
 * Any authenticated user could read, edit, or delete any other user's workspaces,
 * collections, requests, mock servers, routes, and logs simply by guessing/incrementing IDs.
 * <p>
 * Every service in this application now routes ownership checks through here so the
 * rule lives in exactly one place.
 */
public final class OwnershipGuard {

    private OwnershipGuard() {
    }

    public static void assertOwnsWorkspace(Workspace workspace, User currentUser) {
        if (workspace.getOwner() == null || !workspace.getOwner().getId().equals(currentUser.getId())) {
            throw ForbiddenOperationException.notOwner("workspace");
        }
    }
}
