package com.sora.backend.service;

import com.sora.backend.exception.ServiceException;
import com.sora.backend.exception.BusinessLogicException;
import org.springframework.http.HttpStatus;
import com.sora.backend.model.Collection;
import com.sora.backend.model.Country;
import com.sora.backend.model.Post;
import com.sora.backend.model.PostMedia;
import com.sora.backend.model.PostVisibilityType;
import com.sora.backend.model.TravelPermissionStatus;
import com.sora.backend.model.UserAccount;
import com.sora.backend.repository.CollectionRepository;
import com.sora.backend.repository.CountryRepository;
import com.sora.backend.repository.LikePostRepository;
import com.sora.backend.repository.PostMediaRepository;
import com.sora.backend.repository.PostRepository;
import com.sora.backend.repository.TravelPermissionRepository;
import com.sora.backend.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.sora.backend.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PostService {
    
    private static final Logger logger = LoggerFactory.getLogger(PostService.class);

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostMediaRepository postMediaRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private TravelPermissionRepository travelPermissionRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private UserTravelService userTravelService;

    @Autowired
    private LikePostRepository likePostRepository;

    @Autowired
    private LikePostService likePostService;

    @Autowired
    private CommentService commentService;


    public List<Post> createPost(UserAccount author, String countryCode, String collectionCode, String cityName, Double cityLatitude, Double cityLongitude, String caption, String collaborationOption, Long collaboratorUserId, String sharingOption) {
        Country country = countryRepository.findByCode(countryCode).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("country.not.found")));
        Collection collection = collectionRepository.findByCode(collectionCode).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("collection.not.found")));
        List<Post> createdPosts = new ArrayList<>();
        String sharedPostGroupId = null;
        boolean isCollaborating = collaboratorUserId != null;

        if (isCollaborating) {
            UserAccount collaborator = userAccountRepository.findById(collaboratorUserId).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("user.not.found")));
            validateCollaborationPermission(author, collaborator, country);
            sharedPostGroupId = UUID.randomUUID().toString();

            if ("BOTH_PROFILES".equals(sharingOption)) {
                Post authorPost = createSinglePost(author, author, country, collection, cityName, cityLatitude, cityLongitude, caption, PostVisibilityType.SHARED, sharedPostGroupId);
                createdPosts.add(authorPost);
                Post collaboratorPost = createSinglePost(author, collaborator, country, collection, cityName, cityLatitude, cityLongitude, caption, PostVisibilityType.SHARED, sharedPostGroupId);
                createdPosts.add(collaboratorPost);
            } else if ("COLLABORATOR_ONLY".equals(sharingOption)) {
                Post collaboratorPost = createSinglePost(author, collaborator, country, collection, cityName, cityLatitude, cityLongitude, caption, PostVisibilityType.PERSONAL, null);
                createdPosts.add(collaboratorPost);
            }
        } else {
            validatePersonalPostPermission(author, country);
            Post personalPost = createSinglePost(author, author, country, collection, cityName, cityLatitude, cityLongitude, caption, PostVisibilityType.PERSONAL, null);
            createdPosts.add(personalPost);
        }

        return createdPosts;
    }

    private Post createSinglePost(UserAccount author, UserAccount profileOwner, Country country, Collection collection, String cityName, Double cityLatitude, Double cityLongitude, String caption, PostVisibilityType visibilityType, String sharedPostGroupId) {
        
        Post post = new Post();
        post.setAuthor(author);
        post.setProfileOwner(profileOwner);
        post.setCountry(country);
        post.setCollection(collection);
        post.setCityName(cityName);
        post.setCityLatitude(cityLatitude);
        post.setCityLongitude(cityLongitude);
        post.setCaption(caption);
        post.setVisibilityType(visibilityType);
        post.setSharedPostGroupId(sharedPostGroupId);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    private void validatePersonalPostPermission(UserAccount author, Country country) {
        boolean hasVisited = postRepository.existsByProfileOwnerIdAndCountryId(author.getId(), country.getId());
        boolean hasPermission = travelPermissionRepository.existsByGranteeIdAndCountryIdAndStatus(author.getId(), country.getId(), TravelPermissionStatus.ACTIVE);
        if (!hasVisited && !hasPermission) throw new ServiceException(MessageUtil.getMessage("post.permission.required"));
    }

    private void validateCollaborationPermission(UserAccount author, UserAccount collaborator, Country country) {
        if (!travelPermissionRepository.existsByGranteeIdAndCountryIdAndStatus(author.getId(), country.getId(), TravelPermissionStatus.ACTIVE))
            throw new ServiceException(MessageUtil.getMessage("post.collaboration.permission.required"));
    }

    public List<PostMedia> uploadPostMedia(Long postId, UserAccount author, List<MultipartFile> files) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("post.not.found")));
        if (!post.getAuthor().getId().equals(author.getId())) throw new BusinessLogicException("POST_MEDIA_FORBIDDEN", MessageUtil.getMessage("post.not.authorized"), HttpStatus.FORBIDDEN);
        List<PostMedia> mediaList = new ArrayList<>();
        int sortOrder = 0;

        for (MultipartFile file : files) {
            try {
                CloudinaryService.CloudinaryUploadResult result = cloudinaryService.uploadImage(file, "posts");
                PostMedia media = new PostMedia();
                media.setPost(post);
                media.setFileName(file.getOriginalFilename());
                media.setCloudinaryUrl(result.url());
                media.setCloudinaryPublicId(result.publicId());
                media.setWidth(result.width());
                media.setHeight(result.height());
                media.setSortOrder(sortOrder++);
                media.setFileSize(result.size());
                PostMedia savedMedia = postMediaRepository.save(media);
                mediaList.add(savedMedia);
            } catch (Exception e) {
                throw new ServiceException(MessageUtil.getMessage("file.upload.failed"), e);
            }
        }
        return mediaList;
    }

    @Transactional(readOnly = true)
    public Post getPostById(Long postId) {
        return postRepository.findById(postId).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("post.not.found")));
    }

    @Transactional(readOnly = true)
    public Page<Post> getUserPosts(UserAccount user, Pageable pageable) {
        return postRepository.findByProfileOwnerIdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Post> getUserCountryPosts(UserAccount user, String countryCode, String collectionCode, Pageable pageable) {
        Country country = countryRepository.findByCode(countryCode).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("country.not.found")));
        if (collectionCode != null) {
            Collection collection = collectionRepository.findByCode(collectionCode).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("collection.not.found")));
            return postRepository.findByProfileOwnerIdAndCountryIdAndCollectionIdOrderByCreatedAtDesc(user.getId(), country.getId(), collection.getId(), pageable);
        }
        return postRepository.findByProfileOwnerIdAndCountryIdOrderByCreatedAtDesc(user.getId(), country.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Post> getSharedPostGroup(String sharedPostGroupId) {
        List<Post> posts = postRepository.findBySharedPostGroupId(sharedPostGroupId);
        if (posts.isEmpty()) {
            throw new ServiceException(MessageUtil.getMessage("post.shared.group.not.found"));
        }
        return posts;
    }

    public Post updatePost(Long postId, UserAccount currentUser, String caption, String collectionCode) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("post.not.found")));
        validatePostEditPermission(post, currentUser);
        if (collectionCode != null) {
            Collection collection = collectionRepository.findByCode(collectionCode).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("collection.not.found")));
            post.setCollection(collection);
        }
        if (caption != null) post.setCaption(caption);
        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    public void deletePost(Long postId, UserAccount currentUser) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("post.not.found")));
        validatePostDeletePermission(post, currentUser);
        List<PostMedia> media = postMediaRepository.findByPostIdOrderBySortOrder(postId);
        for (PostMedia mediaItem : media) {
            try {
                cloudinaryService.deleteImage(mediaItem.getCloudinaryPublicId());
            } catch (Exception e) {
                logger.warn("Failed to delete image from Cloudinary: {}", e.getMessage());
            }
        }
        postMediaRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    private void validatePostEditPermission(Post post, UserAccount currentUser) {
        boolean isAuthor = post.getAuthor().getId().equals(currentUser.getId());
        boolean hasActivePermission = false;
        if (!isAuthor)
            hasActivePermission = travelPermissionRepository.existsByGranteeIdAndCountryIdAndStatus(currentUser.getId(), post.getCountry().getId(), TravelPermissionStatus.ACTIVE);
        if (!isAuthor && !hasActivePermission)
            throw new BusinessLogicException("POST_EDIT_FORBIDDEN", MessageUtil.getMessage("post.edit.not.authorized"), HttpStatus.FORBIDDEN);
    }

    private void validatePostDeletePermission(Post post, UserAccount currentUser) {
        if (!post.getProfileOwner().getId().equals(currentUser.getId()))
            throw new BusinessLogicException("POST_DELETE_FORBIDDEN", MessageUtil.getMessage("post.delete.not.authorized"), HttpStatus.FORBIDDEN);
    }

    @Transactional(readOnly = true)
    public Page<Post> getFeedPosts(List<Long> followedUserIds, Pageable pageable) {
        return postRepository.findByProfileOwnerIdInOrderByCreatedAtDesc(followedUserIds, pageable);
    }

    @Transactional(readOnly = true)
    public List<Post> getRecentPostsByCountry(String countryCode, int daysSince) {
        Country country = countryRepository.findByCode(countryCode).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("country.not.found")));
        LocalDateTime since = LocalDateTime.now().minusDays(daysSince);
        return postRepository.findByCountryIdAndCreatedAtAfterOrderByCreatedAtDesc(country.getId(), since);
    }

    public List<Post> createPost(UserAccount author, com.sora.backend.dto.PostCreateRequestDto request) {
        return createPost(author, request.countryCode(), request.collectionCode(), request.cityName(), 
                         request.cityLatitude(), request.cityLongitude(), request.caption(), 
                         request.collaborationOption() != null ? request.collaborationOption().name() : "PERSONAL_ONLY", 
                         request.collaboratorUserId(), request.sharingOption());
    }

    public List<PostMedia> uploadMedia(Long postId, MultipartFile[] files, UserAccount currentUser) {
        List<MultipartFile> fileList = List.of(files);
        return uploadPostMedia(postId, currentUser, fileList);
    }

    public java.util.Optional<Post> findById(Long postId) {
        return postRepository.findById(postId);
    }

    public Post updatePost(Long postId, com.sora.backend.dto.PostUpdateRequestDto request, UserAccount currentUser) {
        return updatePost(postId, currentUser, request.caption(), request.collectionCode());
    }

    public List<Post> getPostsBySharedGroup(String groupId) {
        return getSharedPostGroup(groupId);
    }

    public boolean isPostLikedByUser(Long postId, Long userId) {
        return likePostRepository.existsByUserIdAndPostId(userId, postId);
    }

    public com.sora.backend.dto.CountryPostsResponseDto getCountryPosts(Long userId, String countryCode, String collectionCode, String cityName, org.springframework.data.domain.Pageable pageable) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("user.not.found")));
        Country country = countryRepository.findByCode(countryCode)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("country.not.found")));

        Page<Post> posts;
        if (collectionCode != null && cityName != null) {
            Collection collection = collectionRepository.findByCode(collectionCode).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("collection.not.found")));
            posts = postRepository.findByProfileOwnerIdAndCountryIdAndCollectionIdAndCityNameOrderByCreatedAtDesc(userId, country.getId(), collection.getId(), cityName, pageable);
        } else if (collectionCode != null) {
            Collection collection = collectionRepository.findByCode(collectionCode).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("collection.not.found")));
            posts = postRepository.findByProfileOwnerIdAndCountryIdAndCollectionIdOrderByCreatedAtDesc(userId, country.getId(), collection.getId(), pageable);
        } else if (cityName != null) {
            posts = postRepository.findByProfileOwnerIdAndCountryIdAndCityNameOrderByCreatedAtDesc(userId, country.getId(), cityName, pageable);
        } else {
            posts = postRepository.findByProfileOwnerIdAndCountryIdOrderByCreatedAtDesc(userId, country.getId(), pageable);
        }

        com.sora.backend.dto.UserSummaryDto userDto = new com.sora.backend.dto.UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfilePicture(),
                0,
                false
        );

        com.sora.backend.dto.CountryDto countryDto = new com.sora.backend.dto.CountryDto(
                country.getId(),
                country.getCode(),
                country.getNameKey(),
                country.getLatitude(),
                country.getLongitude(),
                country.getTimezone()
        );

        LocalDateTime firstVisitDate = postRepository.findFirstPostDateInCountryByIds(userId, country.getId());
        LocalDateTime lastVisitDate = postRepository.findLastPostDateInCountryByIds(userId, country.getId());
        int totalPostsCount = (int) postRepository.countByProfileOwnerIdAndCountryId(userId, country.getId());
        List<String> cities = postRepository.findDistinctCitiesByUserAndCountry(userId, country.getId());

        com.sora.backend.dto.CountryPostsResponseDto.VisitInfoDto visitInfo = new com.sora.backend.dto.CountryPostsResponseDto.VisitInfoDto(
                firstVisitDate != null ? firstVisitDate.toLocalDate() : null,
                lastVisitDate != null ? lastVisitDate.toLocalDate() : null,
                1,
                totalPostsCount,
                cities
        );

        return new com.sora.backend.dto.CountryPostsResponseDto(
                countryDto,
                userDto,
                visitInfo,
                posts.map(this::mapToPostResponseDto)
        );
    }

    private com.sora.backend.dto.PostResponseDto mapToPostResponseDto(Post post) {
        return new com.sora.backend.dto.PostResponseDto(
                post.getId(),
                mapToUserSummaryDto(post.getAuthor()),
                mapToUserSummaryDto(post.getProfileOwner()),
                mapToCountryDto(post.getCountry()),
                mapToCollectionDto(post.getCollection()),
                post.getCityName(),
                post.getCityLatitude(),
                post.getCityLongitude(),
                post.getCaption(),
                post.getMedia().stream().map(this::mapToMediaDto).toList(),
                (int) likePostService.getPostLikesCount(post.getId()),
                (int) commentService.getPostCommentsCount(post.getId()),
                false,
                post.getVisibilityType(),
                post.getSharedPostGroupId(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private com.sora.backend.dto.UserSummaryDto mapToUserSummaryDto(UserAccount user) {
        return new com.sora.backend.dto.UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfilePicture(),
                0,
                false
        );
    }

    private com.sora.backend.dto.CountryDto mapToCountryDto(Country country) {
        return new com.sora.backend.dto.CountryDto(
                country.getId(),
                country.getCode(),
                country.getNameKey(),
                country.getLatitude(),
                country.getLongitude(),
                country.getTimezone()
        );
    }

    private com.sora.backend.dto.CollectionDto mapToCollectionDto(Collection collection) {
        return new com.sora.backend.dto.CollectionDto(
                collection.getId(),
                collection.getCode(),
                collection.getNameKey(),
                collection.getIconName(),
                collection.getSortOrder(),
                collection.getIsDefault()
        );
    }

    private com.sora.backend.dto.MediaDto mapToMediaDto(PostMedia media) {
        String thumbnailUrl = media.getCloudinaryUrl() != null ? 
                media.getCloudinaryUrl().replace("/upload/", "/upload/c_fill,w_300,h_300/") : null;
        
        return new com.sora.backend.dto.MediaDto(
                media.getId(),
                media.getFileName(),
                media.getCloudinaryPublicId(),
                media.getCloudinaryUrl(),
                thumbnailUrl,
                media.getMediaType(),
                media.getFileSize(),
                media.getWidth(),
                media.getHeight(),
                media.getSortOrder(),
                media.getUploadedAt()
        );
    }
}