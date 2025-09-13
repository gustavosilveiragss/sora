package com.sora.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sora.backend.model.*;
import com.sora.backend.model.PostVisibilityType;
import com.sora.backend.repository.*;
import com.sora.backend.security.JwtUtil;
import com.sora.backend.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtUtil jwtUtil;

    @Autowired
    protected UserAccountService userAccountService;

    @Autowired
    protected UserAccountRepository userAccountRepository;

    @Autowired
    protected PostRepository postRepository;

    @Autowired
    protected CountryRepository countryRepository;

    @Autowired
    protected CollectionRepository collectionRepository;

    @Autowired
    protected FollowRepository followRepository;

    @Autowired
    protected TravelPermissionRepository travelPermissionRepository;

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected LikePostRepository likePostRepository;

    protected UserAccount testUser1;
    protected UserAccount testUser2;
    protected String testUser1Token;
    protected String testUser2Token;

    @BeforeEach
    @Transactional
    void setUp() {
        createTestUsers();
        createEssentialTestData();
    }

    protected void createTestUsers() {
        testUser1 = userAccountService.registerUser(
            "testuser1",
            "test1@email.com",
            "Password123@",
            "Test",
            "User 1",
            "Test bio 1"
        );

        testUser2 = userAccountService.registerUser(
            "testuser2", 
            "test2@email.com",
            "Password123@",
            "Test",
            "User 2",
            "Test bio 2"
        );

        UserDetails userDetails1 = userAccountService.loadUserByUsername(testUser1.getEmail());
        UserDetails userDetails2 = userAccountService.loadUserByUsername(testUser2.getEmail());

        testUser1Token = jwtUtil.generateAccessToken(userDetails1);
        testUser2Token = jwtUtil.generateAccessToken(userDetails2);
    }

    protected void createTestData() {
        createCollections();
        createCountries();
    }
    
    protected void createEssentialTestData() {
        createCollections();
        createCountries();
    }

    protected void createCollections() {
        if (!collectionRepository.findByCode("GENERAL").isPresent()) {
            var general = new com.sora.backend.model.Collection();
            general.setCode("GENERAL");
            general.setNameKey("collection.general");
            general.setIconName("camera");
            general.setSortOrder(1);
            general.setIsDefault(true);
            collectionRepository.save(general);
        }

        if (!collectionRepository.findByCode("CULINARY").isPresent()) {
            var culinary = new com.sora.backend.model.Collection();
            culinary.setCode("CULINARY");
            culinary.setNameKey("collection.culinary");
            culinary.setIconName("restaurant");
            culinary.setSortOrder(2);
            culinary.setIsDefault(false);
            collectionRepository.save(culinary);
        }

        if (!collectionRepository.findByCode("EVENTS").isPresent()) {
            var events = new com.sora.backend.model.Collection();
            events.setCode("EVENTS");
            events.setNameKey("collection.events");
            events.setIconName("celebration");
            events.setSortOrder(3);
            events.setIsDefault(false);
            collectionRepository.save(events);
        }

        if (!collectionRepository.findByCode("OTHERS").isPresent()) {
            var others = new com.sora.backend.model.Collection();
            others.setCode("OTHERS");
            others.setNameKey("collection.others");
            others.setIconName("more_horiz");
            others.setSortOrder(4);
            others.setIsDefault(false);
            collectionRepository.save(others);
        }
    }

    protected void createCountries() {
        if (countryRepository.findByCode("BR").isEmpty()) {
            var brazil = new com.sora.backend.model.Country();
            brazil.setCode("BR");
            brazil.setNameKey("country.brazil");
            brazil.setLatitude(-14.235);
            brazil.setLongitude(-51.9253);
            brazil.setTimezone("America/Sao_Paulo");
            countryRepository.save(brazil);
        }

        if (countryRepository.findByCode("US").isEmpty()) {
            var usa = new com.sora.backend.model.Country();
            usa.setCode("US");
            usa.setNameKey("country.usa");
            usa.setLatitude(39.8283);
            usa.setLongitude(-98.5795);
            usa.setTimezone("America/New_York");
            countryRepository.save(usa);
        }

        if (countryRepository.findByCode("FR").isEmpty()) {
            var france = new com.sora.backend.model.Country();
            france.setCode("FR");
            france.setNameKey("country.france");
            france.setLatitude(46.2276);
            france.setLongitude(2.2137);
            france.setTimezone("Europe/Paris");
            countryRepository.save(france);
        }

        if (countryRepository.findByCode("JP").isEmpty()) {
            var japan = new com.sora.backend.model.Country();
            japan.setCode("JP");
            japan.setNameKey("country.japan");
            japan.setLatitude(36.2048);
            japan.setLongitude(138.2529);
            japan.setTimezone("Asia/Tokyo");
            countryRepository.save(japan);
        }
    }

    protected void createTestPosts() {
        Country brazil = countryRepository.findByCode("BR").orElseThrow();
        Country usa = countryRepository.findByCode("US").orElseThrow();
        Country france = countryRepository.findByCode("FR").orElseThrow();
        Country japan = countryRepository.findByCode("JP").orElseThrow();
        
        var generalCollection = collectionRepository.findByCode("GENERAL").orElseThrow();
        var culinaryCollection = collectionRepository.findByCode("CULINARY").orElseThrow();
        var eventsCollection = collectionRepository.findByCode("EVENTS").orElseThrow();

        createPost(testUser1, testUser1, brazil, generalCollection, "São Paulo", "Exploring São Paulo city");
        createPost(testUser1, testUser1, brazil, culinaryCollection, "Rio de Janeiro", "Amazing Brazilian food");
        createPost(testUser1, testUser1, usa, generalCollection, "New York", "Times Square experience");
        createPost(testUser1, testUser1, usa, eventsCollection, "Miami", "Miami Beach party");
        createPost(testUser1, testUser1, france, culinaryCollection, "Paris", "French cuisine masterclass");
        createPost(testUser1, testUser1, japan, generalCollection, "Tokyo", "Exploring Tokyo city");

        createPost(testUser2, testUser2, brazil, generalCollection, "Brasília", "Capital city tour");
        createPost(testUser2, testUser2, brazil, eventsCollection, "Salvador", "Carnival celebration");
        createPost(testUser2, testUser2, usa, generalCollection, "Los Angeles", "Hollywood adventure");
        createPost(testUser2, testUser2, usa, culinaryCollection, "Chicago", "Deep dish pizza");
    }
    
    protected Post createPost(UserAccount author, UserAccount profileOwner, Country country, com.sora.backend.model.Collection collection, String cityName, String caption) {
        Post post = new Post();
        post.setAuthor(author);
        post.setProfileOwner(profileOwner);
        post.setCountry(country);
        post.setCollection(collection);
        post.setCityName(cityName);
        post.setCaption(caption);
        post.setVisibilityType(PostVisibilityType.PERSONAL);
        return postRepository.save(post);
    }

    protected void createDefaultTravelPermissions() {
        Country brazil = countryRepository.findByCode("BR").orElseThrow();
        Country usa = countryRepository.findByCode("US").orElseThrow();
        Country france = countryRepository.findByCode("FR").orElseThrow();
        Country japan = countryRepository.findByCode("JP").orElseThrow();

        createTravelPermission(testUser1, testUser1, brazil);
        createTravelPermission(testUser1, testUser1, usa);  
        createTravelPermission(testUser1, testUser1, france);

        createTravelPermission(testUser2, testUser2, brazil);
        createTravelPermission(testUser2, testUser2, usa);
    }

    protected void createTravelPermission(UserAccount grantor, UserAccount grantee, Country country) {
        TravelPermission permission = new TravelPermission();
        permission.setGrantor(grantor);
        permission.setGrantee(grantee);
        permission.setCountry(country);
        permission.setStatus(TravelPermissionStatus.ACTIVE);
        permission.setInvitationMessage("Self-permission for testing");
        permission.setRespondedAt(java.time.LocalDateTime.now());
        travelPermissionRepository.save(permission);
    }

    protected void createDefaultFollowRelationships() {
        Follow follow = new Follow();
        follow.setFollower(testUser1);
        follow.setFollowing(testUser2);
        follow.setCreatedAt(java.time.LocalDateTime.now());
        followRepository.save(follow);

        Follow followBack = new Follow();
        followBack.setFollower(testUser2);
        followBack.setFollowing(testUser1);
        followBack.setCreatedAt(java.time.LocalDateTime.now());
        followRepository.save(followBack);
    }

    protected void createDefaultLikesData() {
        var allPosts = postRepository.findAll();
        if (allPosts.size() >= 4) {
            var user1Posts = allPosts.stream()
                .filter(post -> post.getAuthor().getId().equals(testUser1.getId()))
                .limit(2)
                .toList();
            
            for (Post post : user1Posts) {
                LikePost like = new LikePost();
                like.setUser(testUser2);
                like.setPost(post);
                like.setCreatedAt(java.time.LocalDateTime.now());
                likePostRepository.save(like);
            }

            var user2Posts = allPosts.stream()
                .filter(post -> post.getAuthor().getId().equals(testUser2.getId()))
                .limit(2)
                .toList();
                
            for (Post post : user2Posts) {
                LikePost like = new LikePost();
                like.setUser(testUser1);
                like.setPost(post);
                like.setCreatedAt(java.time.LocalDateTime.now());
                likePostRepository.save(like);
            }
        }
    }

    protected String asJsonString(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}