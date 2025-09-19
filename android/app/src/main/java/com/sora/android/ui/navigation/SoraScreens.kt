package com.sora.android.ui.navigation

sealed class SoraScreens(val route: String) {
    object Splash : SoraScreens("splash")
    object Login : SoraScreens("login")
    object Register : SoraScreens("register")
    object Home : SoraScreens("home")
    object Profile : SoraScreens("profile")
    object Feed : SoraScreens("feed")
    object Post : SoraScreens("post")
    object CreatePost : SoraScreens("create_post")
    object Country : SoraScreens("country")
    object Globe : SoraScreens("globe")
    object Search : SoraScreens("search")
    object Notifications : SoraScreens("notifications")
    object Settings : SoraScreens("settings")

    object UserProfile : SoraScreens("user_profile/{userId}") {
        fun createRoute(userId: Long) = "user_profile/$userId"
    }

    object CountryDetails : SoraScreens("country_details/{countryCode}") {
        fun createRoute(countryCode: String) = "country_details/$countryCode"
    }

    object PostDetails : SoraScreens("post_details/{postId}") {
        fun createRoute(postId: Long) = "post_details/$postId"
    }
}