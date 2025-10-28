package com.sora.android.data.remote

import com.sora.android.domain.model.*
import com.sora.android.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody

interface ApiService {

    @GET("messages/hello")
    suspend fun getHelloWorld(): Message

    // Authentication
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponseModel>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponseModel>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenRefreshResponseModel>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    // User Management
    @GET("users/profile")
    suspend fun getCurrentUserProfile(): Response<UserProfileModel>

    @PUT("users/profile")
    suspend fun updateUserProfile(@Body request: UpdateProfileRequest): Response<UserProfileModel>

    @Multipart
    @POST("users/profile/picture")
    suspend fun uploadProfilePicture(@Part file: MultipartBody.Part): Response<ProfilePictureResponse>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") userId: Long): Response<UserProfileModel>

    @GET("gamification/users/{userId}/stats")
    suspend fun getUserTravelStats(@Path("userId") userId: Long): Response<UserGamificationStatsResponseDto>

    @GET("users/search")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("countryCode") countryCode: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<PagedResponse<UserSearchResultModel>>

    // Social Features
    @POST("users/{id}/follow")
    suspend fun followUser(@Path("id") userId: Long): Response<FollowModel>

    @DELETE("users/{id}/follow")
    suspend fun unfollowUser(@Path("id") userId: Long): Response<Unit>

    @GET("users/{id}/followers")
    suspend fun getUserFollowers(
        @Path("id") userId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponse<UserModel>>

    @GET("users/{id}/following")
    suspend fun getUserFollowing(
        @Path("id") userId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponse<UserModel>>

    // Country Collections
    @GET("country-collections")
    suspend fun getCurrentUserCountryCollections(): Response<CountryCollectionsResponse>

    @GET("country-collections/{userId}")
    suspend fun getUserCountryCollections(@Path("userId") userId: Long): Response<CountryCollectionsResponse>

    @GET("country-collections/{userId}/{countryCode}/posts")
    suspend fun getCountryPosts(
        @Path("userId") userId: Long,
        @Path("countryCode") countryCode: String,
        @Query("collectionCode") collectionCode: String? = null,
        @Query("cityName") cityName: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "createdAt",
        @Query("sortDirection") sortDirection: String = "DESC"
    ): Response<CountryPostsResponse>

    // Posts
    @POST("posts")
    suspend fun createPost(@Body request: PostCreateRequest): Response<List<PostModel>>

    @GET("posts/{id}")
    suspend fun getPostById(@Path("id") postId: Long): Response<PostModel>

    @PUT("posts/{id}")
    suspend fun updatePost(@Path("id") postId: Long, @Body request: PostUpdateRequest): Response<PostModel>

    @DELETE("posts/{id}")
    suspend fun deletePost(@Path("id") postId: Long): Response<Unit>

    @Multipart
    @POST("posts/{id}/media")
    suspend fun uploadPostMedia(
        @Path("id") postId: Long,
        @Part files: Array<MultipartBody.Part>
    ): Response<MediaUploadResponse>

    @GET("posts/shared-group/{groupId}")
    suspend fun getSharedPostGroup(@Path("groupId") groupId: String): Response<List<PostModel>>

    @GET("posts/feed")
    suspend fun getFeed(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponse<PostModel>>

    // Likes
    @POST("posts/{id}/like")
    suspend fun likePost(@Path("id") postId: Long): Response<LikeCreateResponse>

    @DELETE("posts/{id}/like")
    suspend fun unlikePost(@Path("id") postId: Long): Response<Unit>

    @GET("posts/{id}/likes")
    suspend fun getPostLikes(
        @Path("id") postId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponse<LikeModel>>

    @GET("posts/{id}/likes/count")
    suspend fun getPostLikesCount(@Path("id") postId: Long): Response<LikesCountResponse>

    // Comments
    @POST("posts/{id}/comments")
    suspend fun createComment(@Path("id") postId: Long, @Body request: CommentCreateRequest): Response<CommentCreateResponse>

    @POST("comments/{id}/reply")
    suspend fun replyToComment(@Path("id") commentId: Long, @Body request: CommentCreateRequest): Response<CommentCreateResponse>

    @GET("posts/{id}/comments")
    suspend fun getPostComments(
        @Path("id") postId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponse<CommentResponseDto>>

    @PUT("comments/{id}")
    suspend fun updateComment(@Path("id") commentId: Long, @Body request: CommentCreateRequest): Response<CommentModel>

    @DELETE("comments/{id}")
    suspend fun deleteComment(@Path("id") commentId: Long): Response<Unit>

    @POST("comments/{id}/like")
    suspend fun likeComment(@Path("id") commentId: Long): Response<Unit>

    @DELETE("comments/{id}/like")
    suspend fun unlikeComment(@Path("id") commentId: Long): Response<Unit>

    // Travel Permissions
    @POST("travel-permissions")
    suspend fun grantTravelPermission(@Body request: TravelPermissionRequest): Response<TravelPermissionModel>

    @POST("travel-permissions/{id}/accept")
    suspend fun acceptTravelPermission(@Path("id") permissionId: Long): Response<TravelPermissionModel>

    @POST("travel-permissions/{id}/decline")
    suspend fun declineTravelPermission(@Path("id") permissionId: Long): Response<TravelPermissionModel>

    @DELETE("travel-permissions/{id}")
    suspend fun revokeTravelPermission(@Path("id") permissionId: Long): Response<Unit>

    @GET("travel-permissions/granted")
    suspend fun getGrantedPermissions(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponse<TravelPermissionModel>>

    @GET("travel-permissions/received")
    suspend fun getReceivedPermissions(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponse<TravelPermissionModel>>

    // Notifications
    @GET("notifications")
    suspend fun getNotifications(
        @Query("unreadOnly") unreadOnly: Boolean = false,
        @Query("type") type: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<NotificationsResponse>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") notificationId: Long): Response<NotificationMarkReadResponse>

    @PUT("notifications/mark-all-read")
    suspend fun markAllNotificationsAsRead(): Response<MarkAllReadResponse>

    @GET("notifications/unread-count")
    suspend fun getUnreadNotificationsCount(): Response<UnreadCountResponse>

    // Globe Interface
    @GET("globe/main")
    suspend fun getMainGlobeData(): Response<GlobeDataModel>

    @GET("globe/profile/{userId}")
    suspend fun getProfileGlobeData(@Path("userId") userId: Long): Response<GlobeDataModel>

    @GET("globe/explore")
    suspend fun getExploreGlobeData(
        @Query("timeframe") timeframe: String = "week",
        @Query("minPosts") minPosts: Int = 1
    ): Response<GlobeDataModel>

    @GET("globe/countries/{countryCode}/recent")
    suspend fun getCountryRecentPosts(
        @Path("countryCode") countryCode: String,
        @Query("days") days: Int = 30,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponse<PostModel>>

    // Gamification
    @GET("gamification/users/{userId}/stats")
    suspend fun getUserStats(@Path("userId") userId: Long): Response<UserStatsModel>

    @GET("gamification/leaderboard")
    suspend fun getLeaderboard(
        @Query("metric") metric: String = "countries",
        @Query("timeframe") timeframe: String = "all",
        @Query("limit") limit: Int = 20
    ): Response<LeaderboardResponseDto>

    @GET("gamification/users/{userId}/rankings")
    suspend fun getUserRankings(@Path("userId") userId: Long): Response<RankingsDto>

    @GET("gamification/users/{userId}/countries-visited")
    suspend fun getCountriesVisited(@Path("userId") userId: Long): Response<CountryVisitedListResponse>

    @GET("gamification/users/{userId}/recent-destinations")
    suspend fun getRecentDestinations(
        @Path("userId") userId: Long,
        @Query("limit") limit: Int = 10
    ): Response<RecentDestinationsResponse>

    @GET("gamification/followers-leaderboard")
    suspend fun getFollowersLeaderboard(
        @Query("metric") metric: String = "countries",
        @Query("limit") limit: Int = 10
    ): Response<LeaderboardResponseDto>

    @GET("gamification/following-leaderboard")
    suspend fun getFollowingLeaderboard(
        @Query("metric") metric: String = "countries",
        @Query("limit") limit: Int = 10
    ): Response<LeaderboardResponseDto>

    @GET("gamification/users/{userId}/stats")
    suspend fun getUserGamificationStats(@Path("userId") userId: Long): Response<UserGamificationStatsResponseDto>

    // Location Services (Public endpoints)
    @GET("locations/countries")
    suspend fun getAllCountries(): Response<List<CountryModel>>

    @GET("locations/search")
    suspend fun searchLocations(
        @Query("q") query: String,
        @Query("countryCode") countryCode: String? = null,
        @Query("limit") limit: Int = 10
    ): Response<LocationSearchResultModel>

    @GET("locations/reverse")
    suspend fun reverseGeocode(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double
    ): Response<ReverseGeocodeResultModel>

    @GET("locations/countries/{countryCode}/cities")
    suspend fun searchCitiesInCountry(
        @Path("countryCode") countryCode: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 10
    ): Response<List<CityModel>>

    @GET("locations/popular")
    suspend fun getPopularDestinations(
        @Query("limit") limit: Int = 10,
        @Query("days") days: Int = 30
    ): Response<List<CountryModel>>

    // Collections
    @GET("collections")
    suspend fun getAllCollections(): Response<List<CollectionModel>>

    @GET("collections/{id}")
    suspend fun getCollectionById(@Path("id") collectionId: Long): Response<CollectionModel>

    @GET("collections/by-code/{code}")
    suspend fun getCollectionByCode(@Path("code") code: String): Response<CollectionModel>

    @GET("collections/default")
    suspend fun getDefaultCollection(): Response<CollectionModel>
}