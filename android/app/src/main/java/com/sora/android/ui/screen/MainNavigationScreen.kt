package com.sora.android.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.sora.android.R
import com.sora.android.ui.components.SoraBottomNavigationBar
import com.sora.android.ui.navigation.SoraScreens
import com.sora.android.ui.screen.profile.EditProfileScreen
import com.sora.android.ui.screen.profile.ProfileScreen
import com.sora.android.ui.screen.profile.TravelStatsScreen
import com.sora.android.ui.screen.profile.UserProfileScreen
import com.sora.android.ui.screen.settings.SettingsScreen

@Composable
fun MainNavigationScreen(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            SoraBottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SoraScreens.MainGlobe.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(SoraScreens.MainGlobe.route) {
                PlaceholderScreen(title = stringResource(R.string.main_globe))
            }

            composable(SoraScreens.Explore.route) {
                ExploreScreen(
                    onNavigateToProfile = { userId ->
                        navController.navigate(SoraScreens.UserProfile.createRoute(userId))
                    }
                )
            }

            composable(SoraScreens.CreatePost.route) {
                PlaceholderScreen(title = stringResource(R.string.create_post))
            }
            composable(SoraScreens.Profile.route) {
                ProfileScreen(
                    onNavigateToEditProfile = {
                        navController.navigate(SoraScreens.EditProfile.route)
                    },
                    onNavigateToTravelStats = {
                        navController.navigate(SoraScreens.TravelStats.route)
                    },
                    onNavigateToFollowers = { userId ->
                        navController.navigate(SoraScreens.Followers.createRoute(userId))
                    },
                    onNavigateToFollowing = { userId ->
                        navController.navigate(SoraScreens.Following.createRoute(userId))
                    },
                    onNavigateToSettings = {
                        navController.navigate(SoraScreens.Settings.route)
                    },
                    onNavigateToCountryCollection = { userId, countryCode ->
                        navController.navigate(SoraScreens.CountryCollection.createRoute(userId, countryCode))
                    }
                )
            }

            composable(SoraScreens.EditProfile.route) {
                EditProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onProfileUpdated = { }
                )
            }

            composable(SoraScreens.TravelStats.route) {
                TravelStatsScreen (
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = SoraScreens.Followers.route,
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getLong("userId") ?: 0L
                FollowersScreen(
                    userId = userId,
                    onBackClick = { navController.popBackStack() },
                    onUserClick = { clickedUserId ->
                        navController.navigate(SoraScreens.UserProfile.createRoute(clickedUserId))
                    }
                )
            }

            composable(
                route = SoraScreens.Following.route,
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getLong("userId") ?: 0L
                FollowingScreen(
                    userId = userId,
                    onBackClick = { navController.popBackStack() },
                    onUserClick = { clickedUserId ->
                        navController.navigate(SoraScreens.UserProfile.createRoute(clickedUserId))
                    }
                )
            }

            composable(
                route = SoraScreens.UserProfile.route,
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) {
                UserProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToFollowers = { viewedUserId ->
                        navController.navigate(SoraScreens.Followers.createRoute(viewedUserId))
                    },
                    onNavigateToFollowing = { viewedUserId ->
                        navController.navigate(SoraScreens.Following.createRoute(viewedUserId))
                    },
                    onNavigateToCountryCollection = { userId, countryCode ->
                        navController.navigate(SoraScreens.CountryCollection.createRoute(userId, countryCode))
                    }
                )
            }

            composable(SoraScreens.TravelPermissions.route) {
                PlaceholderScreen(title = stringResource(R.string.travel_permissions))
            }

            composable(
                route = SoraScreens.CountryCollection.route,
                arguments = listOf(
                    navArgument("userId") { type = NavType.LongType },
                    navArgument("countryCode") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getLong("userId") ?: 0L
                val countryCode = backStackEntry.arguments?.getString("countryCode") ?: ""

                com.sora.android.ui.screen.country.CountryCollectionScreen(
                    userId = userId,
                    countryCode = countryCode,
                    onPostClick = { postId ->
                        navController.navigate(SoraScreens.PostDetails.createRoute(postId))
                    },
                    onProfileClick = { clickedUserId ->
                        navController.navigate(SoraScreens.UserProfile.createRoute(clickedUserId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(SoraScreens.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}