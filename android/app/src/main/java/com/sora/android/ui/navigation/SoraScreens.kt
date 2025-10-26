package com.sora.android.ui.navigation

sealed class SoraScreens(val route: String) {
    data object Splash : SoraScreens("splash")
    data object Login : SoraScreens("login")
    data object Register : SoraScreens("register")

    data object MainGlobe : SoraScreens("main_globe")
    data object Explore : SoraScreens("explore")
    data object CreatePost : SoraScreens("create_post")
    data object Profile : SoraScreens("profile")

    data object EditProfile : SoraScreens("edit_profile")
    data object TravelStats : SoraScreens("travel_stats")
    data object Followers : SoraScreens("followers/{userId}") {
        fun createRoute(userId: Long) = "followers/$userId"
    }
    data object Following : SoraScreens("following/{userId}") {
        fun createRoute(userId: Long) = "following/$userId"
    }
    data object TravelPermissions : SoraScreens("travel_permissions")
    data object InviteUser : SoraScreens("invite_user")
    data object PostDetail : SoraScreens("post_detail/{postId}") {
        fun createRoute(postId: Long) = "post_detail/$postId"
    }
    data object Feed : SoraScreens("feed")
    data object Comments : SoraScreens("comments/{postId}") {
        fun createRoute(postId: Long) = "comments/$postId"
    }
    data object LikesList : SoraScreens("likes_list/{postId}") {
        fun createRoute(postId: Long) = "likes_list/$postId"
    }

    data object Search : SoraScreens("search")
    data object Notifications : SoraScreens("notifications")
    data object Settings : SoraScreens("settings")

    data object UserProfile : SoraScreens("user_profile/{userId}") {
        fun createRoute(userId: Long) = "user_profile/$userId"
    }

    data object CountryDetails : SoraScreens("country_details/{countryCode}") {
        fun createRoute(countryCode: String) = "country_details/$countryCode"
    }

    data object CountryCollection : SoraScreens("country_collection/{userId}/{countryCode}") {
        fun createRoute(userId: Long, countryCode: String) = "country_collection/$userId/$countryCode"
    }

    data object PostDetails : SoraScreens("post_details/{postId}") {
        fun createRoute(postId: Long) = "post_details/$postId"
    }
}