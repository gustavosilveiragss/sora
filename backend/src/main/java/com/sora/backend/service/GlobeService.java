package com.sora.backend.service;

import com.sora.backend.dto.*;
import com.sora.backend.exception.ServiceException;
import com.sora.backend.model.*;
import com.sora.backend.repository.*;
import com.sora.backend.util.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class GlobeService {

    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private FollowRepository followRepository;
    
    @Autowired
    private CountryRepository countryRepository;
    
    @Autowired
    private UserAccountRepository userAccountRepository;
    
    @Autowired
    private LikePostRepository likePostRepository;
    
    @Autowired
    private LikePostService likePostService;
    
    @Autowired
    private CommentService commentService;

    public GlobeDataResponseDto getMainGlobeData(UserAccount currentUser) {
        List<Long> followedUserIds = followRepository.findFollowingUserIds(currentUser.getId());
        followedUserIds.add(currentUser.getId());
        
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Post> recentPosts = postRepository.findRecentPostsByFollowedUsers(followedUserIds, since);
        
        Map<String, List<Post>> postsByCountry = recentPosts.stream()
                .collect(Collectors.groupingBy(post -> post.getCountry().getCode()));
        
        List<CountryMarkerDto> countryMarkers = postsByCountry.entrySet().stream()
                .map(entry -> {
                    String countryCode = entry.getKey();
                    List<Post> countryPosts = entry.getValue();
                    
                    if (countryPosts.isEmpty()) return null;
                    
                    Country country = countryPosts.getFirst().getCountry();
                    
                    Map<Long, List<Post>> postsByUser = countryPosts.stream()
                            .collect(Collectors.groupingBy(post -> post.getProfileOwner().getId()));
                    
                    List<UserSummaryDto> activeUsers = postsByUser.values().stream()
                            .map(posts -> {
                                UserAccount user = posts.getFirst().getProfileOwner();
                                return mapToUserSummaryDto(user);
                            })
                            .collect(Collectors.toList());
                    
                    List<PostSummaryDto> recentPostSummaries = countryPosts.stream()
                            .limit(5)
                            .map(this::mapToPostSummaryDto)
                            .collect(Collectors.toList());
                    
                    return new CountryMarkerDto(
                            countryCode,
                            country.getNameKey(),
                            country.getLatitude(),
                            country.getLongitude(),
                            countryPosts.size(),
                            countryPosts.getFirst().getCreatedAt(),
                            activeUsers,
                            recentPostSummaries
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        return new GlobeDataResponseDto(
                "MAIN",
                countryMarkers.size(),
                recentPosts.size(),
                LocalDateTime.now(),
                countryMarkers
        );
    }

    public GlobeDataResponseDto getProfileGlobeData(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(MessageUtil.getMessage("user.not.found")));
        
        List<Country> countriesVisited = postRepository.findDistinctCountriesByProfileOwnerId(userId);
        
        List<CountryMarkerDto> countryMarkers = countriesVisited.stream()
                .map(country -> {
                    List<Post> countryPosts = postRepository.findByProfileOwnerIdAndCountryIdOrderByCreatedAtDesc(
                            userId, country.getId(), PageRequest.of(0, 5)
                    ).getContent();
                    
                    List<UserSummaryDto> activeUsers = List.of();
                    
                    List<PostSummaryDto> recentPostSummaries = countryPosts.stream()
                            .map(this::mapToPostSummaryDto)
                            .collect(Collectors.toList());
                    
                    LocalDateTime lastPostDate = countryPosts.isEmpty() ? 
                            postRepository.findLastPostDateInCountryByIds(userId, country.getId()) : 
                            countryPosts.getFirst().getCreatedAt();
                    
                    return new CountryMarkerDto(
                            country.getCode(),
                            country.getNameKey(),
                            country.getLatitude(),
                            country.getLongitude(),
                            (int) postRepository.countByProfileOwnerIdAndCountryId(userId, country.getId()),
                            lastPostDate,
                            activeUsers,
                            recentPostSummaries
                    );
                })
                .collect(Collectors.toList());
        
        int totalPosts = countryMarkers.stream()
                .mapToInt(CountryMarkerDto::recentPostsCount)
                .sum();
        
        return new GlobeDataResponseDto(
                "PROFILE",
                countryMarkers.size(),
                totalPosts,
                LocalDateTime.now(),
                countryMarkers
        );
    }

    public GlobeDataResponseDto getExploreGlobeData(String timeframe, int minPosts) {
        int days = switch (timeframe.toLowerCase()) {
            case "week" -> 7;
            case "month" -> 30;
            case "year" -> 365;
            default -> 30;
        };
        
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Post> recentPosts = postRepository.findRecentPostsGlobally(since);
        
        Map<String, List<Post>> postsByCountry = recentPosts.stream()
                .collect(Collectors.groupingBy(post -> post.getCountry().getCode()));
        
        List<CountryMarkerDto> countryMarkers = postsByCountry.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= minPosts)
                .map(entry -> {
                    String countryCode = entry.getKey();
                    List<Post> countryPosts = entry.getValue();
                    
                    if (countryPosts.isEmpty()) return null;
                    
                    Country country = countryPosts.getFirst().getCountry();
                    
                    Map<Long, List<Post>> postsByUser = countryPosts.stream()
                            .collect(Collectors.groupingBy(post -> post.getProfileOwner().getId()));
                    
                    List<UserSummaryDto> activeUsers = postsByUser.entrySet().stream()
                            .limit(3)
                            .map(userEntry -> {
                                UserAccount user = userEntry.getValue().getFirst().getProfileOwner();
                                return mapToUserSummaryDto(user);
                            })
                            .collect(Collectors.toList());
                    
                    List<PostSummaryDto> popularPosts = countryPosts.stream()
                            .sorted((p1, p2) -> Integer.compare(p2.getLikesCount(), p1.getLikesCount()))
                            .limit(3)
                            .map(this::mapToPostSummaryDto)
                            .collect(Collectors.toList());
                    
                    return new CountryMarkerDto(
                            countryCode,
                            country.getNameKey(),
                            country.getLatitude(),
                            country.getLongitude(),
                            countryPosts.size(),
                            countryPosts.stream()
                                    .max((p1, p2) -> p1.getCreatedAt().compareTo(p2.getCreatedAt()))
                                    .map(Post::getCreatedAt)
                                    .orElse(LocalDateTime.now()),
                            activeUsers,
                            popularPosts
                    );
                })
                .filter(Objects::nonNull)
                .sorted((m1, m2) -> Integer.compare(m2.recentPostsCount(), m1.recentPostsCount()))
                .collect(Collectors.toList());
        
        return new GlobeDataResponseDto(
                "EXPLORE",
                countryMarkers.size(),
                recentPosts.size(),
                LocalDateTime.now(),
                countryMarkers
        );
    }

    public Page<PostResponseDto> getCountryRecentPosts(UserAccount currentUser, String countryCode, int days, Pageable pageable) {
        List<Long> followedUserIds = followRepository.findFollowingUserIds(currentUser.getId());
        followedUserIds.add(currentUser.getId());
        
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        Page<Post> posts = postRepository.findRecentPostsByFollowedUsersInCountry(
                followedUserIds, countryCode, since, pageable
        );
        
        List<PostResponseDto> postDtos = posts.getContent().stream()
                .map(post -> mapToPostResponseDto(post, currentUser))
                .collect(Collectors.toList());
        
        return new PageImpl<>(postDtos, pageable, posts.getTotalElements());
    }
    
    private UserSummaryDto mapToUserSummaryDto(UserAccount user) {
        int countriesCount = (int) postRepository.countDistinctCountriesByProfileOwnerId(user.getId());
        
        return new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfilePicture(),
                countriesCount,
                false
        );
    }
    
    private PostSummaryDto mapToPostSummaryDto(Post post) {
        String thumbnailUrl = post.getMedia().stream()
                .findFirst()
                .map(media -> generateThumbnailUrl(media.getCloudinaryUrl()))
                .orElse(null);
        
        return new PostSummaryDto(
                post.getId(),
                mapToUserSummaryDto(post.getAuthor()),
                post.getCityName(),
                thumbnailUrl,
                (int) likePostService.getPostLikesCount(post.getId()),
                post.getCreatedAt()
        );
    }
    
    private PostResponseDto mapToPostResponseDto(Post post, UserAccount currentUser) {
        List<MediaDto> mediaDtos = post.getMedia().stream()
                .map(media -> new MediaDto(
                        media.getId(),
                        media.getFileName(),
                        media.getCloudinaryPublicId(),
                        media.getCloudinaryUrl(),
                        generateThumbnailUrl(media.getCloudinaryUrl()),
                        media.getMediaType(),
                        media.getFileSize(),
                        media.getWidth(),
                        media.getHeight(),
                        media.getSortOrder(),
                        media.getUploadedAt()
                ))
                .collect(Collectors.toList());
        
        boolean isLiked = likePostRepository.existsByUserIdAndPostId(currentUser.getId(), post.getId());
        
        return new PostResponseDto(
                post.getId(),
                mapToUserSummaryDto(post.getAuthor()),
                mapToUserSummaryDto(post.getProfileOwner()),
                new CountryDto(
                        post.getCountry().getId(),
                        post.getCountry().getCode(),
                        post.getCountry().getNameKey(),
                        post.getCountry().getLatitude(),
                        post.getCountry().getLongitude(),
                        post.getCountry().getTimezone()
                ),
                new CollectionDto(
                        post.getCollection().getId(),
                        post.getCollection().getCode(),
                        post.getCollection().getNameKey(),
                        post.getCollection().getIconName(),
                        post.getCollection().getSortOrder(),
                        post.getCollection().getIsDefault()
                ),
                post.getCityName(),
                post.getCityLatitude(),
                post.getCityLongitude(),
                post.getCaption(),
                mediaDtos,
                (int) likePostService.getPostLikesCount(post.getId()),
                (int) commentService.getPostCommentsCount(post.getId()),
                isLiked,
                post.getVisibilityType(),
                post.getSharedPostGroupId(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
    
    private String generateThumbnailUrl(String cloudinaryUrl) {
        if (cloudinaryUrl == null || !cloudinaryUrl.contains("cloudinary.com")) {
            return cloudinaryUrl;
        }
        
        String baseUrl = cloudinaryUrl.substring(0, cloudinaryUrl.lastIndexOf("/") + 1);
        String filename = cloudinaryUrl.substring(cloudinaryUrl.lastIndexOf("/") + 1);
        
        return baseUrl + "c_fill,w_150,h_150/" + filename;
    }
}