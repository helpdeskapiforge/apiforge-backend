package com.apiplatform.web.mapper;

import com.apiplatform.model.*;
import com.apiplatform.web.dto.response.*;

/**
 * Every entity->response mapping lives here. Before this existed, controllers
 * returned JPA entities directly, coupling the wire format to the persistence model
 * (and relying on scattered @JsonIgnore annotations to avoid leaking internal fields
 * or recursing through relationships). Centralizing the mapping means the API
 * contract is explicit and reviewable in one file, and entity refactors don't
 * silently change what a client receives.
 */
public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static WorkspaceResponse toResponse(Workspace w) {
        return new WorkspaceResponse(w.getId(), w.getName(), w.getDescription(),
                w.getOwner() != null ? w.getOwner().getId() : null);
    }

    public static CollectionResponse toResponse(Collection c) {
        return new CollectionResponse(c.getId(), c.getName(), c.getDescription(),
                c.getWorkspace() != null ? c.getWorkspace().getId() : null,
                c.getParentId(), c.getSortOrder());
    }

    public static RequestItemResponse toResponse(RequestItem r) {
        return new RequestItemResponse(
                r.getId(), r.getName(), r.getMethod(), r.getUrl(), r.getHeaders(), r.getBody(), r.getAuthConfig(),
                r.getCollection() != null ? r.getCollection().getId() : null,
                r.getWorkspace() != null ? r.getWorkspace().getId() : null);
    }

    public static EnvironmentResponse toResponse(Environment e) {
        return new EnvironmentResponse(e.getId(), e.getName(), e.getVariables(),
                e.getWorkspace() != null ? e.getWorkspace().getId() : null);
    }

    public static MockServerResponse toResponse(MockServer s) {
        return new MockServerResponse(s.getId(), s.getName(), s.getPort(), s.getPathPrefix(),
                s.getWorkspace() != null ? s.getWorkspace().getId() : null);
    }

    public static MockRouteResponse toResponse(MockRoute r) {
        return new MockRouteResponse(
                r.getId(), r.getMethod(), r.getPath(), r.getStatusCode(), r.getContentType(),
                r.getResponseBody(), r.getResponseHeaders(), r.getDelayMs(), r.isEnabled(),
                r.isChaosEnabled(), r.getFailureRate(), r.getDescription(),
                r.getMockServer() != null ? r.getMockServer().getId() : null);
    }

    public static MockLogResponse toResponse(MockLog l) {
        return new MockLogResponse(
                l.getId(), l.getMethod(), l.getPath(), l.getStatusCode(), l.getDurationMs(), l.getTimestamp(),
                l.getRequestBody(), l.getResponseBody(), l.isChaosTriggered(),
                l.getMockServer() != null ? l.getMockServer().getId() : null);
    }

    public static RequestHistoryResponse toResponse(RequestHistory h) {
        return new RequestHistoryResponse(h.getId(), h.getMethod(), h.getUrl(), h.getStatus(),
                h.getDurationMs(), h.getTimestamp());
    }
}
