package com.sora.backend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Collection Controller Integration Tests")
class CollectionControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should get all collections ordered by sort order")
    void shouldGetAllCollections() throws Exception {
        mockMvc.perform(get("/api/collections")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].code").value("GENERAL"))
                .andExpect(jsonPath("$[0].nameKey").value("collection.general"))
                .andExpect(jsonPath("$[0].iconName").value("camera"))
                .andExpect(jsonPath("$[0].sortOrder").value(1))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[1].code").value("CULINARY"))
                .andExpect(jsonPath("$[1].nameKey").value("collection.culinary"))
                .andExpect(jsonPath("$[1].iconName").value("restaurant"))
                .andExpect(jsonPath("$[1].sortOrder").value(2))
                .andExpect(jsonPath("$[1].isDefault").value(false))
                .andExpect(jsonPath("$[2].code").value("EVENTS"))
                .andExpect(jsonPath("$[2].nameKey").value("collection.events"))
                .andExpect(jsonPath("$[2].iconName").value("celebration"))
                .andExpect(jsonPath("$[2].sortOrder").value(3))
                .andExpect(jsonPath("$[2].isDefault").value(false))
                .andExpect(jsonPath("$[3].code").value("OTHERS"))
                .andExpect(jsonPath("$[3].nameKey").value("collection.others"))
                .andExpect(jsonPath("$[3].iconName").value("more_horiz"))
                .andExpect(jsonPath("$[3].sortOrder").value(4))
                .andExpect(jsonPath("$[3].isDefault").value(false));
    }

    @Test
    @DisplayName("Should get collection by valid ID")
    void shouldGetCollectionByValidId() throws Exception {
        var generalCollection = collectionRepository.findByCode("GENERAL").orElseThrow();
        Long actualId = generalCollection.getId();
        
        mockMvc.perform(get("/api/collections/" + actualId)
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(actualId))
                .andExpect(jsonPath("$.code").value("GENERAL"))
                .andExpect(jsonPath("$.nameKey").value("collection.general"))
                .andExpect(jsonPath("$.iconName").value("camera"))
                .andExpect(jsonPath("$.sortOrder").value(1))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    @DisplayName("Should return 404 for invalid collection ID")
    void shouldReturn404ForInvalidId() throws Exception {
        mockMvc.perform(get("/api/collections/999")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get collection by valid code")
    void shouldGetCollectionByValidCode() throws Exception {
        var culinaryCollection = collectionRepository.findByCode("CULINARY").orElseThrow();
        Long actualId = culinaryCollection.getId();
        
        mockMvc.perform(get("/api/collections/by-code/CULINARY")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(actualId))
                .andExpect(jsonPath("$.code").value("CULINARY"))
                .andExpect(jsonPath("$.nameKey").value("collection.culinary"))
                .andExpect(jsonPath("$.iconName").value("restaurant"))
                .andExpect(jsonPath("$.sortOrder").value(2))
                .andExpect(jsonPath("$.isDefault").value(false));
    }

    @Test
    @DisplayName("Should handle case insensitive collection code lookup")
    void shouldHandleCaseInsensitiveCodeLookup() throws Exception {
        mockMvc.perform(get("/api/collections/by-code/culinary")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("CULINARY"));
    }

    @Test
    @DisplayName("Should return 404 for invalid collection code")
    void shouldReturn404ForInvalidCode() throws Exception {
        mockMvc.perform(get("/api/collections/by-code/INVALID")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get default collection")
    void shouldGetDefaultCollection() throws Exception {
        mockMvc.perform(get("/api/collections/default")
                        .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("GENERAL"))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    @DisplayName("Should require authentication for all endpoints")
    void shouldRequireAuthenticationForAllEndpoints() throws Exception {
        mockMvc.perform(get("/api/collections"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/collections/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/collections/by-code/GENERAL"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/collections/default"))
                .andExpect(status().isUnauthorized());
    }
}