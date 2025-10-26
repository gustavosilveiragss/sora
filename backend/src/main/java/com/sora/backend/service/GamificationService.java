package com.sora.backend.service;

import com.sora.backend.dto.*;
import com.sora.backend.model.Country;
import com.sora.backend.model.UserAccount;
import com.sora.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GamificationService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private LikePostRepository likePostRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserTravelService userTravelService;

    public UserGamificationStatsResponseDto getUserTravelStats(Long userId) {
        UserAccount user = userAccountRepository.findById(userId).orElse(null);
        if (user == null) return null;

        UserTravelStatsDto travelStats = userTravelService.getUserTravelStatistics(userId);
        
        UserGamificationStatsResponseDto.TravelStatsDto stats = new UserGamificationStatsResponseDto.TravelStatsDto(
                travelStats.countriesVisitedCount(),
                travelStats.citiesVisitedCount(),
                travelStats.totalPostsCount(),
                travelStats.totalLikesReceived(),
                travelStats.totalCommentsReceived(),
                (int) followRepository.countFollowersByUserId(userId),
                (int) followRepository.countByFollowerId(userId),
                user.getCreatedAt().toLocalDate(),
                (int) java.time.temporal.ChronoUnit.DAYS.between(user.getCreatedAt().toLocalDate(), LocalDate.now()),
                travelStats.countriesVisitedCount() > 0 ? (double) travelStats.totalPostsCount() / travelStats.countriesVisitedCount() : 0.0
        );

        UserSummaryDto userDto = new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfilePicture(),
                travelStats.countriesVisitedCount(),
                false
        );

        UserGamificationStatsResponseDto.RankingsDto rankings = getUserRankings(userId, user);
        List<UserGamificationStatsResponseDto.AchievementDto> achievements = getUserAchievements(travelStats);
        List<UserGamificationStatsResponseDto.ContinentStatsDto> continentStats = getContinentStats(userId);

        return new UserGamificationStatsResponseDto(
                userDto,
                stats,
                rankings,
                achievements,
                continentStats
        );
    }

    public LeaderboardResponseDto getLeaderboard(UserAccount currentUser, String metric, String timeframe, int limit) {
        List<Long> mutualUserIds = followRepository.findMutualFollowerIds(currentUser.getId());
        mutualUserIds.add(currentUser.getId());

        return buildLeaderboard(currentUser, mutualUserIds, metric, timeframe, limit);
    }

    public LeaderboardResponseDto getFollowersLeaderboard(UserAccount currentUser, String metric, int limit) {
        List<Long> followerIds = followRepository.findByFollowingId(currentUser.getId(), org.springframework.data.domain.PageRequest.of(0, 1000))
                .stream()
                .map(follow -> follow.getFollower().getId())
                .collect(Collectors.toList());

        followerIds.add(currentUser.getId());

        return buildLeaderboard(currentUser, followerIds, metric, "all", limit);
    }

    public LeaderboardResponseDto getFollowingLeaderboard(UserAccount currentUser, String metric, int limit) {
        List<Long> followingIds = followRepository.findByFollowerId(currentUser.getId(), org.springframework.data.domain.PageRequest.of(0, 1000))
                .stream()
                .map(follow -> follow.getFollowing().getId())
                .collect(Collectors.toList());

        followingIds.add(currentUser.getId());

        return buildLeaderboard(currentUser, followingIds, metric, "all", limit);
    }

    private LeaderboardResponseDto buildLeaderboard(UserAccount currentUser, List<Long> userIds, String metric, String timeframe, int limit) {
        List<UserAccount> users = userAccountRepository.findAllById(userIds);
        List<LeaderboardResponseDto.LeaderboardEntryDto> entries = new ArrayList<>();

        for (UserAccount user : users) {
            int score = getScoreForMetric(user.getId(), metric, timeframe);

            LeaderboardResponseDto.LeaderboardEntryDto entry = new LeaderboardResponseDto.LeaderboardEntryDto(
                    0,
                    mapToUserSummaryDto(user),
                    score,
                    getScoreName(metric),
                    user.getId().equals(currentUser.getId())
            );

            entries.add(entry);
        }

        entries.sort((a, b) -> Integer.compare(b.score(), a.score()));

        List<LeaderboardResponseDto.LeaderboardEntryDto> limitedEntries = new ArrayList<>();
        for (int i = 0; i < Math.min(entries.size(), limit); i++) {
            limitedEntries.add(new LeaderboardResponseDto.LeaderboardEntryDto(
                    i + 1,
                    entries.get(i).user(),
                    entries.get(i).score(),
                    entries.get(i).scoreName(),
                    entries.get(i).isCurrentUser()
            ));
        }

        Integer currentUserPosition = null;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).isCurrentUser()) {
                currentUserPosition = i + 1;
                break;
            }
        }

        return new LeaderboardResponseDto(
                metric,
                timeframe,
                currentUserPosition,
                limitedEntries
        );
    }

    public UserGamificationStatsResponseDto.RankingsDto getUserRankings(Long userId, UserAccount currentUser) {
        List<Long> mutualUserIds = followRepository.findMutualFollowerIds(currentUser.getId());
        mutualUserIds.add(currentUser.getId());

        int totalUsers = mutualUserIds.size();

        int countriesRank = calculateRankForMetric(userId, mutualUserIds, "countries");
        int postsRank = calculateRankForMetric(userId, mutualUserIds, "posts");

        UserGamificationStatsResponseDto.RankingPositionDto countriesRanking = new UserGamificationStatsResponseDto.RankingPositionDto(
                countriesRank,
                totalUsers,
                totalUsers > 0 ? ((double) (totalUsers - countriesRank + 1) / totalUsers) * 100 : 0.0
        );

        UserGamificationStatsResponseDto.RankingPositionDto postsRanking = new UserGamificationStatsResponseDto.RankingPositionDto(
                postsRank,
                totalUsers,
                totalUsers > 0 ? ((double) (totalUsers - postsRank + 1) / totalUsers) * 100 : 0.0
        );

        return new UserGamificationStatsResponseDto.RankingsDto(
                countriesRanking,
                postsRanking
        );
    }

    public CountryVisitedListResponseDto getCountriesVisited(Long userId) {
        UserAccount user = userAccountRepository.findById(userId).orElse(null);
        if (user == null) return null;

        List<Country> countriesVisited = postRepository.findDistinctCountriesByProfileOwnerId(userId);
        
        List<CountryVisitedListResponseDto.CountryVisitedDetailDto> countryDetails = countriesVisited.stream()
                .map(country -> {
                    List<String> cities = postRepository.findDistinctCitiesByUserAndCountry(userId, country.getId());
                    CountryDto countryDto = mapToCountryDto(country);
                    
                    java.time.LocalDateTime firstPostDate = postRepository.findFirstPostDateInCountryByIds(userId, country.getId());
                    java.time.LocalDateTime lastPostDate = postRepository.findLastPostDateInCountryByIds(userId, country.getId());
                    
                    LocalDate firstVisitDate = firstPostDate != null ? firstPostDate.toLocalDate() : null;
                    LocalDate lastVisitDate = lastPostDate != null ? lastPostDate.toLocalDate() : null;
                    int visitCount = cities.size();
                    
                    return new CountryVisitedListResponseDto.CountryVisitedDetailDto(
                            countryDto,
                            firstVisitDate,
                            lastVisitDate,
                            visitCount,
                            (int) postRepository.countByProfileOwnerIdAndCountryId(userId, country.getId()),
                            cities
                    );
                })
                .collect(Collectors.toList());

        return new CountryVisitedListResponseDto(
                userId,
                user.getUsername(),
                countryDetails.size(),
                countryDetails
        );
    }

    public RecentDestinationsResponseDto getRecentDestinations(Long userId, int limit) {
        UserAccount user = userAccountRepository.findById(userId).orElse(null);
        if (user == null) return null;

        List<LastActiveCountryDto> recentCountries = postRepository.findLastActiveCountriesByUserId(userId);
        
        List<RecentDestinationsResponseDto.RecentDestinationDto> destinations = recentCountries.stream()
                .limit(limit)
                .map(countryDto -> {
                    Country country = countryRepository.findByCode(countryDto.countryCode()).orElse(null);
                    if (country == null) return null;
                    
                    String lastCity = postRepository.findLastCityVisitedInCountry(userId, country.getId());
                    java.time.LocalDateTime lastPostDate = postRepository.findLastPostDateInCountryByIds(userId, country.getId());
                    int recentPostsCount = (int) postRepository.countByProfileOwnerIdAndCountryIdAndCreatedAtAfter(
                            userId, 
                            country.getId(), 
                            lastPostDate != null ? lastPostDate.minusDays(30) : java.time.LocalDateTime.now().minusDays(30)
                    );
                    
                    return new RecentDestinationsResponseDto.RecentDestinationDto(
                            mapToCountryDto(country),
                            lastCity,
                            lastPostDate,
                            recentPostsCount
                    );
                })
                .filter(dest -> dest != null)
                .collect(Collectors.toList());

        return new RecentDestinationsResponseDto(
                userId,
                user.getUsername(),
                destinations
        );
    }

    private int getScoreForMetric(Long userId, String metric, String timeframe) {
        UserTravelStatsDto stats = userTravelService.getUserTravelStatistics(userId);

        return switch (metric.toLowerCase()) {
            case "countries" -> stats.countriesVisitedCount();
            case "cities" -> stats.citiesVisitedCount();
            case "posts" -> stats.totalPostsCount();
            default -> 0;
        };
    }

    private String getScoreName(String metric) {
        return switch (metric.toLowerCase()) {
            case "countries" -> "countries visited";
            case "cities" -> "cities visited";
            case "posts" -> "posts created";
            default -> "points";
        };
    }

    private int calculateRankForMetric(Long userId, List<Long> userIds, String metric) {
        int userScore = getScoreForMetric(userId, metric, "all");
        int rank = 1;
        
        for (Long otherId : userIds) {
            if (!otherId.equals(userId)) {
                int otherScore = getScoreForMetric(otherId, metric, "all");
                if (otherScore > userScore) {
                    rank++;
                }
            }
        }
        
        return rank;
    }

    private List<UserGamificationStatsResponseDto.AchievementDto> getUserAchievements(UserTravelStatsDto stats) {
        List<UserGamificationStatsResponseDto.AchievementDto> achievements = new ArrayList<>();
        
        if (stats.countriesVisitedCount() >= 5) {
            achievements.add(new UserGamificationStatsResponseDto.AchievementDto(
                    "GLOBE_TROTTER",
                    "achievement.globe_trotter",
                    "achievement.globe_trotter.description",
                    "public",
                    LocalDate.now(),
                    "Visit 5 countries"
            ));
        }
        
        if (stats.totalPostsCount() >= 50 && stats.totalPostsCount() > 0) {
            achievements.add(new UserGamificationStatsResponseDto.AchievementDto(
                    "CONTENT_CREATOR",
                    "achievement.content_creator",
                    "achievement.content_creator.description",
                    "camera",
                    LocalDate.now(),
                    "Create 50 posts"
            ));
        }
        
        return achievements;
    }

    private List<UserGamificationStatsResponseDto.ContinentStatsDto> getContinentStats(Long userId) {
        List<UserGamificationStatsResponseDto.ContinentStatsDto> continentStats = new ArrayList<>();
        
        continentStats.add(new UserGamificationStatsResponseDto.ContinentStatsDto(
                "SA",
                "continent.south_america",
                0,
                12,
                0.0,
                0
        ));
        
        return continentStats;
    }

    private UserSummaryDto mapToUserSummaryDto(UserAccount user) {
        int countriesCount = (int) userTravelService.getTotalCountriesVisited(user.getId());
        
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

    private CountryDto mapToCountryDto(Country country) {
        return new CountryDto(
                country.getId(),
                country.getCode(),
                country.getNameKey(),
                country.getLatitude(),
                country.getLongitude(),
                country.getTimezone()
        );
    }
}