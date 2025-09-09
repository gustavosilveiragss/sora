package com.sora.backend.service;

import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.Collection;
import com.sora.backend.model.Country;
import com.sora.backend.model.Post;
import com.sora.backend.model.PostMedia;
import com.sora.backend.model.PostVisibilityType;
import com.sora.backend.model.TravelPermissionStatus;
import com.sora.backend.model.UserAccount;
import com.sora.backend.repository.CollectionRepository;
import com.sora.backend.repository.CountryRepository;
import com.sora.backend.repository.PostMediaRepository;
import com.sora.backend.repository.PostRepository;
import com.sora.backend.repository.TravelPermissionRepository;
import com.sora.backend.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.sora.backend.util.MessageUtil;
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


    public List<Post> createPost(UserAccount author, String countryCode, String collectionCode, String cityName, Double cityLatitude, Double cityLongitude, String caption, String collaborationOption, Long collaboratorUserId, String sharingOption) {
        Country country = countryRepository.findByCode(countryCode).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("country.not.found")));
        Collection collection = collectionRepository.findByCode(collectionCode).orElseThrow(() -> new ServiceException(MessageUtil.getMessage("collection.not.found")));
        List<Post> createdPosts = new ArrayList<>();
        String sharedPostGroupId = null;
        boolean isCollaborating = "COLLABORATE_WITH_USER".equals(collaborationOption) && collaboratorUserId != null;

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
        if (!post.getAuthor().getId().equals(author.getId())) throw new ServiceException(MessageUtil.getMessage("post.not.authorized"));
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
        return postRepository.findBySharedPostGroupId(sharedPostGroupId);
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
                System.out.println(e.getMessage());
            }
        }
        postMediaRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    private void validatePostEditPermission(Post post, UserAccount currentUser) {
        boolean isAuthor = post.getAuthor().getId().equals(currentUser.getId());
        boolean hasActivePermission = false;
        if (!isAuthor) hasActivePermission = travelPermissionRepository.existsByGranteeIdAndCountryIdAndStatus(currentUser.getId(), post.getCountry().getId(), TravelPermissionStatus.ACTIVE);
        if (!isAuthor && !hasActivePermission) throw new ServiceException(MessageUtil.getMessage("post.edit.not.authorized"));
    }

    private void validatePostDeletePermission(Post post, UserAccount currentUser) {
        if (!post.getProfileOwner().getId().equals(currentUser.getId())) throw new ServiceException(MessageUtil.getMessage("post.delete.not.authorized"));
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
}