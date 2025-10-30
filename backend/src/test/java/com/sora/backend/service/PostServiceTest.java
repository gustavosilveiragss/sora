package com.sora.backend.service;

import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.Collection;
import com.sora.backend.model.Country;
import com.sora.backend.model.Post;
import com.sora.backend.model.PostVisibilityType;
import com.sora.backend.model.TravelPermission;
import com.sora.backend.model.TravelPermissionStatus;
import com.sora.backend.model.UserAccount;
import com.sora.backend.repository.CollectionRepository;
import com.sora.backend.repository.CountryRepository;
import com.sora.backend.repository.PostRepository;
import com.sora.backend.repository.TravelPermissionRepository;
import com.sora.backend.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TravelPermissionRepository travelPermissionRepository;

    private UserAccount testUser;
    private UserAccount collaborator;
    private Country testCountry;
    private Collection testCollection;

    @BeforeEach
    void setUp() {
        testUser = createUser("testuser", "test@example.com", "Test", "User");
        collaborator = createUser("collaborator", "collab@example.com", "Collab", "User");
        testCountry = createCountry("BR", "country.brazil", -14.235, -51.9253);
        testCollection = createCollection("GENERAL", "collection.general", "camera", 1, true);
    }

    @Test
    void createPost_shouldCreatePersonalPost() {
        createInitialPostForUser(testUser, testCountry);

        List<Post> result = postService.createPost(
                testUser,
                "BR",
                "GENERAL",
                "São Paulo",
                -23.5558,
                -46.6396,
                "Amazing city visit",
                "PERSONAL_ONLY",
                null,
                null
        );

        assertThat(result).hasSize(1);
        Post post = result.getFirst();
        assertThat(post.getAuthor().getId()).isEqualTo(testUser.getId());
        assertThat(post.getProfileOwner().getId()).isEqualTo(testUser.getId());
        assertThat(post.getCountry().getCode()).isEqualTo("BR");
        assertThat(post.getCityName()).isEqualTo("São Paulo");
        assertThat(post.getCaption()).isEqualTo("Amazing city visit");
        assertThat(post.getVisibilityType()).isEqualTo(PostVisibilityType.PERSONAL);
        assertThat(post.getSharedPostGroupId()).isNull();
    }

    @Test
    void createPost_shouldCreateCollaborativePostsInBothProfiles() {
        createInitialPostForUser(testUser, testCountry);
        createActivePermission(collaborator, testUser, testCountry);

        List<Post> result = postService.createPost(
                testUser,
                "BR",
                "GENERAL",
                "Rio de Janeiro",
                -22.9068,
                -43.1729,
                "Beach time with friend",
                "PERSONAL_ONLY",
                collaborator.getId(),
                "BOTH_PROFILES"
        );

        assertThat(result).hasSize(2);

        Post authorPost = result.getFirst();
        assertThat(authorPost.getAuthor().getId()).isEqualTo(testUser.getId());
        assertThat(authorPost.getProfileOwner().getId()).isEqualTo(testUser.getId());
        assertThat(authorPost.getVisibilityType()).isEqualTo(PostVisibilityType.SHARED);
        assertThat(authorPost.getSharedPostGroupId()).isNotNull();

        Post collaboratorPost = result.get(1);
        assertThat(collaboratorPost.getAuthor().getId()).isEqualTo(testUser.getId());
        assertThat(collaboratorPost.getProfileOwner().getId()).isEqualTo(collaborator.getId());
        assertThat(collaboratorPost.getVisibilityType()).isEqualTo(PostVisibilityType.SHARED);
        assertThat(collaboratorPost.getSharedPostGroupId()).isEqualTo(authorPost.getSharedPostGroupId());
    }

    @Test
    void updatePost_shouldUpdateCaptionAndCollection() {
        createInitialPostForUser(testUser, testCountry);
        Post post = createPost(testUser, testUser, testCountry, testCollection, "Original caption");
        createCollection("CULINARY", "collection.culinary", "restaurant", 2, false);

        Post updated = postService.updatePost(
                post.getId(),
                testUser,
                "Updated caption",
                "CULINARY"
        );

        assertThat(updated.getCaption()).isEqualTo("Updated caption");
        assertThat(updated.getCollection().getCode()).isEqualTo("CULINARY");
        assertThat(updated.getUpdatedAt()).isAfter(post.getCreatedAt());
    }

    @Test
    void getUserPosts_shouldReturnUserPostsPaginated() {
        createInitialPostForUser(testUser, testCountry);
        createPost(testUser, testUser, testCountry, testCollection, "Post 1");
        createPost(testUser, testUser, testCountry, testCollection, "Post 2");
        createPost(testUser, testUser, testCountry, testCollection, "Post 3");

        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> result = postService.getUserPosts(testUser, pageable);

        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getContent()).allMatch(p -> p.getProfileOwner().getId().equals(testUser.getId()));
    }

    private UserAccount createUser(String username, String email, String firstName, String lastName) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hashedpassword");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setIsActive(true);
        return userAccountRepository.save(user);
    }

    private Country createCountry(String code, String nameKey, Double latitude, Double longitude) {
        Country country = new Country();
        country.setCode(code);
        country.setNameKey(nameKey);
        country.setLatitude(latitude);
        country.setLongitude(longitude);
        country.setTimezone("America/Sao_Paulo");
        return countryRepository.save(country);
    }

    private Collection createCollection(String code, String nameKey, String iconName, Integer sortOrder, Boolean isDefault) {
        return collectionRepository.findByCode(code).orElseGet(() -> {
            Collection collection = new Collection();
            collection.setCode(code);
            collection.setNameKey(nameKey);
            collection.setIconName(iconName);
            collection.setSortOrder(sortOrder);
            collection.setIsDefault(isDefault);
            return collectionRepository.save(collection);
        });
    }

    private Post createPost(UserAccount author, UserAccount profileOwner, Country country, Collection collection, String caption) {
        Post post = new Post();
        post.setAuthor(author);
        post.setProfileOwner(profileOwner);
        post.setCountry(country);
        post.setCollection(collection);
        post.setCityName("Test City");
        post.setCityLatitude(-23.5558);
        post.setCityLongitude(-46.6396);
        post.setCaption(caption);
        post.setVisibilityType(PostVisibilityType.PERSONAL);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    private void createInitialPostForUser(UserAccount user, Country country) {
        Post initialPost = new Post();
        initialPost.setAuthor(user);
        initialPost.setProfileOwner(user);
        initialPost.setCountry(country);
        initialPost.setCollection(testCollection);
        initialPost.setCityName("Initial City");
        initialPost.setCityLatitude(-23.5558);
        initialPost.setCityLongitude(-46.6396);
        initialPost.setCaption("Initial post");
        initialPost.setVisibilityType(PostVisibilityType.PERSONAL);
        initialPost.setCreatedAt(LocalDateTime.now());
        initialPost.setUpdatedAt(LocalDateTime.now());
        postRepository.save(initialPost);
    }

    private void createActivePermission(UserAccount grantor, UserAccount grantee, Country country) {
        TravelPermission permission = new TravelPermission();
        permission.setGrantor(grantor);
        permission.setGrantee(grantee);
        permission.setCountry(country);
        permission.setStatus(TravelPermissionStatus.ACTIVE);
        permission.setInvitationMessage("Test permission");
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(LocalDateTime.now());
        travelPermissionRepository.save(permission);
    }
}
