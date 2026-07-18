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

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CollectionFolderNestingIT {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String token;
    private Long workspaceId;

    @BeforeEach
    void setUp() throws Exception {
        String email = "folders-" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Folder User\",\"email\":\"" + email + "\",\"password\":\"correct-horse-battery-staple\"}"))
                .andExpect(status().isCreated());

        MvcResult signin = mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"correct-horse-battery-staple\"}"))
                .andExpect(status().isOk())
                .andReturn();
        token = objectMapper.readTree(signin.getResponse().getContentAsString()).get("token").asText();

        MvcResult ws = mockMvc.perform(post("/api/v1/workspaces/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Folder Workspace\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        workspaceId = objectMapper.readTree(ws.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createCollection(String name, Long parentId) throws Exception {
        String parentField = parentId != null ? ",\"parentId\":" + parentId : "";
        MvcResult result = mockMvc.perform(post("/api/v1/collections/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"workspaceId\":" + workspaceId + parentField + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void createsNestedFolder() throws Exception {
        long parent = createCollection("Parent Folder", null);
        long child = createCollection("Child Folder", parent);

        MvcResult result = mockMvc.perform(get("/api/v1/collections/workspace/" + workspaceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tree = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode childNode = null;
        for (JsonNode node : tree) {
            if (node.get("id").asLong() == child) {
                childNode = node;
            }
        }
        org.junit.jupiter.api.Assertions.assertNotNull(childNode, "child collection should be present in the workspace list");
        org.junit.jupiter.api.Assertions.assertEquals(parent, childNode.get("parentId").asLong());
    }

    @Test
    void movingAFolderIntoItsOwnDescendantIsRejected() throws Exception {
        long parent = createCollection("A", null);
        long child = createCollection("B", parent);
        long grandchild = createCollection("C", child);

        // Try to move "A" (the root) to be a child of "C" (its own grandchild) -- a cycle.
        mockMvc.perform(put("/api/v1/collections/" + parent)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":" + grandchild + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot move a folder into one of its own descendants."));
    }

    @Test
    void renamingWithoutNameFieldStillWorksForAMoveOnlyUpdate() throws Exception {
        long folder = createCollection("Movable", null);
        long target = createCollection("Target", null);

        // Move-only update: no "name" field sent at all -- this is exactly the request
        // shape that used to fail @NotBlank validation before the DTO fix.
        mockMvc.perform(put("/api/v1/collections/" + folder)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":" + target + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(target))
                .andExpect(jsonPath("$.name").value("Movable")); // unchanged
    }

    @Test
    void clearingParentMovesFolderBackToRoot() throws Exception {
        long parent = createCollection("Root", null);
        long child = createCollection("Nested", parent);

        mockMvc.perform(put("/api/v1/collections/" + child)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clearParent\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(nullValue()));
    }

    @Test
    void creatingWithoutNameIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/collections/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workspaceId\":" + workspaceId + "}"))
                .andExpect(status().isBadRequest());
    }
}
