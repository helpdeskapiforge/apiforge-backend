package com.apiplatform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the Phase 0 work: /api/v1 versioning still resolves the same as the legacy
 * /api prefix, list endpoints return DTOs (no raw entity/JPA-proxy fields leak through),
 * and the paginated endpoints honor page/size and cap at the server-side maximum.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkspaceRequestFlowIT {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String token;
    private Long workspaceId;
    private Long collectionId;

    @BeforeEach
    void signUpAndSignIn() throws Exception {
        String email = "flow-" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Flow User\",\"email\":\"" + email + "\",\"password\":\"correct-horse-battery-staple\"}"))
                .andExpect(status().isCreated());

        MvcResult signin = mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"correct-horse-battery-staple\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(signin.getResponse().getContentAsString());
        token = body.get("token").asText();

        MvcResult wsResult = mockMvc.perform(post("/api/v1/workspaces/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Flow Workspace\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        workspaceId = objectMapper.readTree(wsResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult colResult = mockMvc.perform(post("/api/v1/collections/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Flow Collection\",\"workspaceId\":" + workspaceId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        collectionId = objectMapper.readTree(colResult.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void workspaceResponseHasNoOwnerObjectLeak() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/my-workspaces")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Flow Workspace"))
                .andExpect(jsonPath("$[0].owner").doesNotExist()) // DTO, not the JPA entity
                .andExpect(jsonPath("$[0].ownerId").exists());
    }

    @Test
    void legacyUnversionedPathStillWorks() throws Exception {
        mockMvc.perform(get("/api/workspaces/my-workspaces")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void paginatedRequestsEndpointHonorsPageSizeAndCapsAtMax() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/requests/create")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Req " + i + "\",\"method\":\"GET\",\"url\":\"\",\"collectionId\":"
                                    + collectionId + ",\"workspaceId\":" + workspaceId + "}"))
                    .andExpect(status().isCreated());
        }

        MvcResult result = mockMvc.perform(get("/api/v1/requests/collection/" + collectionId)
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andReturn();

        JsonNode page = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(2, page.get("data").size());
        assertTrue(page.get("data").get(0).has("method"));
        assertTrue(page.get("data").get(0).get("collection").isMissingNode()); // no entity leakage
    }

    @Test
    void requestingLargerThanMaxPageSizeGetsClamped() throws Exception {
        mockMvc.perform(get("/api/v1/requests/collection/" + collectionId)
                        .header("Authorization", "Bearer " + token)
                        .param("size", "10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(200)); // MAX_PAGE_SIZE in RequestItemService
    }
}
