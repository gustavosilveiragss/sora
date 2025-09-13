package com.sora.backend.integration;

import com.sora.backend.dto.TravelPermissionRequestDto;
import com.sora.backend.model.*;
import com.sora.backend.model.TravelPermissionStatus;
import com.sora.backend.model.PostVisibilityType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TravelPermissionControllerIntegrationTest extends BaseIntegrationTest {

    private Country testCountry;
    private TravelPermission testPermission;

    @BeforeEach
    @Transactional
    @Override
    void setUp() {
        super.setUp();
        createTravelPermissionTestData();
    }
    
    private void createTravelPermissionTestData() {
        createTestCountry();
        createRequiredPosts();
        createTestPermission();
    }
    
    private void createRequiredPosts() {
        createDefaultTravelPermissions();
        createTestPosts();
    }
    
    protected void createTestPosts() {
        Country japan = countryRepository.findByCode("JP").orElse(null);
        if (japan == null) {
            japan = new Country();
            japan.setCode("JP");
            japan.setNameKey("country.japan");
            japan.setLatitude(36.2048);
            japan.setLongitude(138.2529);
            japan.setTimezone("Asia/Tokyo");
            japan = countryRepository.save(japan);
        }
        
        Country us = countryRepository.findByCode("US").orElse(null);
        if (us == null) {
            us = new Country();
            us.setCode("US");
            us.setNameKey("country.usa");
            us.setLatitude(39.8283);
            us.setLongitude(-98.5795);
            us.setTimezone("America/New_York");
            us = countryRepository.save(us);
        }
        
        var generalCollection = collectionRepository.findByCode("GENERAL").orElseThrow();

        Post jpPost = new Post();
        jpPost.setAuthor(testUser1);
        jpPost.setProfileOwner(testUser1);
        jpPost.setCountry(japan);
        jpPost.setCollection(generalCollection);
        jpPost.setCityName("Tokyo");
        jpPost.setCityLatitude(35.6762);
        jpPost.setCityLongitude(139.6503);
        jpPost.setCaption("Beautiful Japan!");
        jpPost.setVisibilityType(PostVisibilityType.PERSONAL);
        jpPost.setCreatedAt(LocalDateTime.now());
        jpPost.setUpdatedAt(LocalDateTime.now());
        postRepository.save(jpPost);

        Post usPost = new Post();
        usPost.setAuthor(testUser1);
        usPost.setProfileOwner(testUser1);
        usPost.setCountry(us);
        usPost.setCollection(generalCollection);
        usPost.setCityName("New York");
        usPost.setCityLatitude(40.7128);
        usPost.setCityLongitude(-74.0060);
        usPost.setCaption("Amazing NYC!");
        usPost.setVisibilityType(PostVisibilityType.PERSONAL);
        usPost.setCreatedAt(LocalDateTime.now());
        usPost.setUpdatedAt(LocalDateTime.now());
        postRepository.save(usPost);

        Post brPost = new Post();
        brPost.setAuthor(testUser1);
        brPost.setProfileOwner(testUser1);
        brPost.setCountry(testCountry);
        brPost.setCollection(generalCollection);
        brPost.setCityName("São Paulo");
        brPost.setCityLatitude(-23.5505);
        brPost.setCityLongitude(-46.6333);
        brPost.setCaption("Brazil visit!");
        brPost.setVisibilityType(PostVisibilityType.PERSONAL);
        brPost.setCreatedAt(LocalDateTime.now());
        brPost.setUpdatedAt(LocalDateTime.now());
        postRepository.save(brPost);
    }

    private void createTestCountry() {
        testCountry = countryRepository.findByCode("BR").orElse(null);
        if (testCountry == null) {
            testCountry = new Country();
            testCountry.setCode("BR");
            testCountry.setNameKey("country.brazil");
            testCountry.setLatitude(-14.235);
            testCountry.setLongitude(-51.9253);
            testCountry.setTimezone("America/Sao_Paulo");
            testCountry = countryRepository.save(testCountry);
        }
    }

    private void createTestPermission() {
        var existingPermission = travelPermissionRepository
            .findByGrantorIdAndGranteeIdAndCountryId(testUser1.getId(), testUser2.getId(), testCountry.getId())
            .orElse(null);
        
        if (existingPermission == null) {
            testPermission = new TravelPermission();
            testPermission.setGrantor(testUser1);
            testPermission.setGrantee(testUser2);
            testPermission.setCountry(testCountry);
            testPermission.setStatus(TravelPermissionStatus.PENDING);
            testPermission.setInvitationMessage("Let's explore Brazil together!");
            testPermission = travelPermissionRepository.save(testPermission);
        } else {
            testPermission = existingPermission;
            testPermission.setStatus(TravelPermissionStatus.PENDING);
            testPermission.setRespondedAt(null);
            testPermission = travelPermissionRepository.save(testPermission);
        }
    }

    @Test
    void grantTravelPermission_Success() throws Exception {
        TravelPermissionRequestDto request = new TravelPermissionRequestDto(
            "testuser2",
            "JP",
            "Let's document our trip to Japan together! 🇯🇵"
        );

        Country japan = countryRepository.findByCode("JP").orElse(null);
        if (japan == null) {
            japan = new Country();
            japan.setCode("JP");
            japan.setNameKey("country.japan");
            japan.setLatitude(36.2048);
            japan.setLongitude(138.2529);
            japan.setTimezone("Asia/Tokyo");
            countryRepository.save(japan);
        }

        mockMvc.perform(post("/api/travel-permissions")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grantor.username").value("testuser1"))
                .andExpect(jsonPath("$.grantee.username").value("testuser2"))
                .andExpect(jsonPath("$.country.code").value("JP"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.invitationMessage").value("Let's document our trip to Japan together! 🇯🇵"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void grantTravelPermission_InvalidCountry() throws Exception {
        TravelPermissionRequestDto request = new TravelPermissionRequestDto(
            "testuser2",
            "INVALID_COUNTRY",
            "Invalid country test"
        );

        mockMvc.perform(post("/api/travel-permissions")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void grantTravelPermission_UserNotFound() throws Exception {
        TravelPermissionRequestDto request = new TravelPermissionRequestDto(
            "nonexistentuser",
            "BR",
            "Permission for non-existent user"
        );

        mockMvc.perform(post("/api/travel-permissions")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void grantTravelPermission_ToSelf() throws Exception {
        TravelPermissionRequestDto request = new TravelPermissionRequestDto(
            "testuser1", // Same as grantor
            "BR",
            "Cannot grant permission to self"
        );

        mockMvc.perform(post("/api/travel-permissions")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void grantTravelPermission_AlreadyExists() throws Exception {
        TravelPermissionRequestDto request = new TravelPermissionRequestDto(
            "testuser2",
            "BR", // Already has permission for BR
            "Duplicate permission test"
        );

        mockMvc.perform(post("/api/travel-permissions")
                .header("Authorization", "Bearer " + testUser1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptTravelPermission_Success() throws Exception {
        mockMvc.perform(post("/api/travel-permissions/" + testPermission.getId() + "/accept")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.respondedAt").exists())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/travel-permissions/received")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    void acceptTravelPermission_NotGrantee() throws Exception {
        mockMvc.perform(post("/api/travel-permissions/" + testPermission.getId() + "/accept")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptTravelPermission_AlreadyResponded() throws Exception {
        mockMvc.perform(post("/api/travel-permissions/" + testPermission.getId() + "/accept")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/travel-permissions/" + testPermission.getId() + "/accept")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptTravelPermission_NotFound() throws Exception {
        mockMvc.perform(post("/api/travel-permissions/999999/accept")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void declineTravelPermission_Success() throws Exception {
        mockMvc.perform(post("/api/travel-permissions/" + testPermission.getId() + "/decline")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.respondedAt").exists())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/travel-permissions/received")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("DECLINED"));
    }

    @Test
    void declineTravelPermission_NotGrantee() throws Exception {
        mockMvc.perform(post("/api/travel-permissions/" + testPermission.getId() + "/decline")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokeTravelPermission_Success() throws Exception {
        testPermission.setStatus(TravelPermissionStatus.ACTIVE);
        travelPermissionRepository.save(testPermission);

        mockMvc.perform(delete("/api/travel-permissions/" + testPermission.getId())
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/travel-permissions/granted")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("REVOKED"));
    }

    @Test
    void revokeTravelPermission_NotGrantor() throws Exception {
        mockMvc.perform(delete("/api/travel-permissions/" + testPermission.getId())
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokeTravelPermission_NotActive() throws Exception {
        mockMvc.perform(delete("/api/travel-permissions/" + testPermission.getId())
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGrantedPermissions_Success() throws Exception {
        mockMvc.perform(get("/api/travel-permissions/granted")
                .param("status", "PENDING")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].grantor.username").value("testuser1"))
                .andExpect(jsonPath("$.content[0].grantee.username").value("testuser2"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void getGrantedPermissions_FilterByStatus() throws Exception {
        // Clean existing ACTIVE permissions for testUser1 to ensure predictable count
        var existingActivePermissions = travelPermissionRepository.findByGrantorIdAndStatus(
            testUser1.getId(), TravelPermissionStatus.ACTIVE);
        travelPermissionRepository.deleteAll(existingActivePermissions);
        
        Country usCountry = countryRepository.findByCode("US").orElse(null);
        if (usCountry == null) {
            usCountry = new Country();
            usCountry.setCode("US");
            usCountry.setNameKey("country.usa");
            usCountry.setLatitude(39.8283);
            usCountry.setLongitude(-98.5795);
            usCountry.setTimezone("America/New_York");
            usCountry = countryRepository.save(usCountry);
        }
        
        TravelPermission activePermission = new TravelPermission();
        activePermission.setGrantor(testUser1);
        activePermission.setGrantee(testUser2);
        activePermission.setCountry(usCountry);
        activePermission.setStatus(TravelPermissionStatus.ACTIVE);
        activePermission.setInvitationMessage("Active permission");
        travelPermissionRepository.save(activePermission);

        mockMvc.perform(get("/api/travel-permissions/granted")
                .param("status", "ACTIVE")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    void getReceivedPermissions_Success() throws Exception {
        mockMvc.perform(get("/api/travel-permissions/received")
                .param("status", "PENDING")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].grantor.username").value("testuser1"))
                .andExpect(jsonPath("$.content[0].grantee.username").value("testuser2"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void getReceivedPermissions_EmptyResult() throws Exception {
        // Clean existing ACTIVE permissions for testUser2 as grantee to ensure empty result
        var existingActivePermissions = travelPermissionRepository.findByGranteeIdAndStatus(
            testUser2.getId(), TravelPermissionStatus.ACTIVE);
        travelPermissionRepository.deleteAll(existingActivePermissions);
        
        mockMvc.perform(get("/api/travel-permissions/received")
                .param("status", "ACTIVE")
                .param("page", "0")
                .param("size", "20")
                .header("Authorization", "Bearer " + testUser2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getTravelPermissions_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/travel-permissions/granted"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void grantTravelPermission_Unauthorized() throws Exception {
        TravelPermissionRequestDto request = new TravelPermissionRequestDto(
            "testuser2",
            "BR",
            "Unauthorized test"
        );

        mockMvc.perform(post("/api/travel-permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isUnauthorized());
    }
}