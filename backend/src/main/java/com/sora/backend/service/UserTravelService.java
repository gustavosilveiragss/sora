package com.sora.backend.service;

import com.sora.backend.dto.LastActiveCountryDto;
import com.sora.backend.dto.UserTravelStatsDto;
import com.sora.backend.model.Country;
import com.sora.backend.model.UserAccount;
import com.sora.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserTravelService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CountryRepository countryRepository;

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
        return countryRepository.findByCode(countryCode).map(country -> postRepository.existsByProfileOwnerIdAndCountryId(userId, country.getId())).orElse(false);
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
}