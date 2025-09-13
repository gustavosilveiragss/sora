package com.sora.backend.service;

import com.sora.backend.dto.CountryCollectionResponseDto;
import com.sora.backend.dto.CountryCollectionsResponseDto;
import com.sora.backend.dto.LastActiveCountryDto;
import com.sora.backend.dto.UserSummaryDto;
import com.sora.backend.dto.UserTravelStatsDto;
import com.sora.backend.model.Country;
import com.sora.backend.model.TravelPermission;
import com.sora.backend.model.TravelPermissionStatus;
import com.sora.backend.model.UserAccount;
import com.sora.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserTravelService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private TravelPermissionRepository travelPermissionRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private LikePostRepository likePostRepository;

    @Autowired
    private CommentRepository commentRepository;

    public List<Country> getCountriesVisitedByUser(Long userId) {
        return postRepository.findDistinctCountriesByProfileOwnerId(userId);
    }

    public long getTotalCountriesVisited(Long userId) {
        return postRepository.countDistinctCountriesByProfileOwnerId(userId);
    }

    public long getTotalCitiesVisited(Long userId) {
        return postRepository.countDistinctCitiesByProfileOwnerId(userId);
    }

    public LocalDateTime getFirstVisitToCountry(Long userId, Long countryId) {
        return postRepository.findFirstPostDateInCountryByIds(userId, countryId);
    }

    public LocalDateTime getLastVisitToCountry(Long userId, Long countryId) {
        return postRepository.findLastPostDateInCountryByIds(userId, countryId);
    }

    public long getPostsCountInCountry(Long userId, Long countryId) {
        return postRepository.countByProfileOwnerIdAndCountryId(userId, countryId);
    }

    public boolean hasVisitedCountry(Long userId, String countryCode) {
        return countryRepository.findByCode(countryCode)
                .map(country -> postRepository.existsByProfileOwnerIdAndCountryId(userId, country.getId()))
                .orElse(false);
    }

    public UserTravelStatsDto getUserTravelStatistics(Long userId) {
        int countriesCount = (int) postRepository.countDistinctCountriesByProfileOwnerId(userId);
        int citiesCount = (int) postRepository.countDistinctCitiesByProfileOwnerId(userId);
        int postsCount = (int) postRepository.countByProfileOwnerId(userId);
        int likesReceived = (int) likePostRepository.countLikesReceivedByUserId(userId);
        int commentsReceived = (int) commentRepository.countCommentsReceivedByUserId(userId);
        return new UserTravelStatsDto(countriesCount, citiesCount, postsCount, likesReceived, commentsReceived);
    }

    public List<LastActiveCountryDto> getLastActiveCountries(Long userId, int limit) {
        List<LastActiveCountryDto> countries = postRepository.findLastActiveCountriesByUserId(userId);
        return countries.stream().limit(limit).toList();
    }

    public List<String> getCommonCountries(Long currentUserId, Long targetUserId) {
        return postRepository.findCommonCountriesBetweenUsers(currentUserId, targetUserId);
    }

    public CountryCollectionsResponseDto getUserCountryCollections(Long userId) {
        UserAccount user = userAccountRepository.findById(userId).orElse(null);
        if (user == null) {
            return new CountryCollectionsResponseDto(userId, null, 0, 0, 0, List.of());
        }

        List<Country> countriesVisited = postRepository.findDistinctCountriesByProfileOwnerId(userId);
        List<CountryCollectionResponseDto> countryDtos = countriesVisited.stream()
                .map(country -> mapToCountryCollectionDto(userId, country))
                .collect(Collectors.toList());

        UserTravelStatsDto stats = getUserTravelStatistics(userId);
        
        return new CountryCollectionsResponseDto(
                userId,
                user.getUsername(),
                stats.countriesVisitedCount(),
                stats.citiesVisitedCount(),
                stats.totalPostsCount(),
                countryDtos
        );
    }

    private CountryCollectionResponseDto mapToCountryCollectionDto(Long userId, Country country) {
        List<TravelPermission> activePermissions = travelPermissionRepository
                .findByGrantorIdAndCountryIdAndStatus(
                        userId,
                        country.getId(),
                        TravelPermissionStatus.ACTIVE
                );

        List<UserSummaryDto> collaborators = activePermissions.stream()
                .map(permission -> mapToUserSummaryDto(permission.getGrantee()))
                .collect(Collectors.toList());

        int postsCount = (int) postRepository.countByProfileOwnerIdAndCountryId(userId, country.getId());
        List<String> cities = postRepository.findDistinctCitiesByUserAndCountry(userId, country.getId());
        
        LocalDateTime firstPostDate = postRepository.findFirstPostDateInCountryByIds(userId, country.getId());
        LocalDateTime lastPostDate = postRepository.findLastPostDateInCountryByIds(userId, country.getId());
        
        LocalDate firstVisitDate = firstPostDate != null ? firstPostDate.toLocalDate() : null;
        LocalDate lastVisitDate = lastPostDate != null ? lastPostDate.toLocalDate() : null;
        
        int visitCount = cities.size();

        return new CountryCollectionResponseDto(
                country.getId(),
                country.getCode(),
                country.getNameKey(),
                country.getLatitude(),
                country.getLongitude(),
                firstVisitDate,
                lastVisitDate,
                visitCount,
                postsCount,
                cities,
                collaborators,
                !activePermissions.isEmpty()
        );
    }

    private UserSummaryDto mapToUserSummaryDto(UserAccount user) {
        int countriesCount = (int) getTotalCountriesVisited(user.getId());
        
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
}