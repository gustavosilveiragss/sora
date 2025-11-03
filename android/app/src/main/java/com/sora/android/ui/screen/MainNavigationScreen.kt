package com.sora.android.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.sora.android.R
import com.sora.android.core.util.CameraManager
import com.sora.android.core.util.PermissionManager
import com.sora.android.ui.components.SoraBottomNavigationBar
import com.sora.android.ui.navigation.SoraScreens
import com.sora.android.ui.screen.profile.EditProfileScreen
import com.sora.android.ui.screen.profile.ProfileScreen
import com.sora.android.ui.screen.profile.TravelStatsScreen
import com.sora.android.ui.screen.settings.SettingsScreen
import com.sora.android.ui.screens.createpost.CreatePostBottomSheet
import com.sora.android.ui.viewmodel.NotificationViewModel
import com.sora.android.ui.viewmodel.ProfileViewModel

@Composable
fun MainNavigationScreen(
    navController: NavHostController = rememberNavController(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val notificationUiState by notificationViewModel.uiState.collectAsState()
    val userProfile by profileViewModel.userProfile.collectAsState()

    LaunchedEffect(notificationUiState.unreadCount) {
        Log.d("SORA_MAIN_NAV", "Contador de notificacoes atualizado: ${notificationUiState.unreadCount}")
    }

    val cameraManager = remember { CameraManager() }
    val permissionManager = remember { PermissionManager() }

    var showCreatePostBottomSheet by remember { mutableStateOf(false) }
    var feedRefreshTrigger by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            SoraBottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (route == SoraScreens.CreatePost.route) {
                        showCreatePostBottomSheet = true
                    } else {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                unreadNotificationsCount = notificationUiState.unreadCount,
                profilePictureUrl = userProfile?.profilePicture
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SoraScreens.Feed.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(SoraScreens.Feed.route) {
                FeedScreen(
                    onNavigateToProfile = { userId ->
                        navController.navigate(SoraScreens.UserProfile.createRoute(userId))
                    },
                    onNavigateToPost = { postId ->
                        navController.navigate(SoraScreens.PostDetails.createRoute(postId))
                    },
                    refreshTrigger = feedRefreshTrigger
                )
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

            composable(SoraScreens.Notifications.route) {
                NotificationsScreen(
                    viewModel = notificationViewModel
                )
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
                    },
                    onNavigateToUserProfile = { userId ->
                        navController.navigate(SoraScreens.UserProfile.createRoute(userId))
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
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditProfile = { },
                    onNavigateToTravelStats = { },
                    onNavigateToFollowers = { viewedUserId ->
                        navController.navigate(SoraScreens.Followers.createRoute(viewedUserId))
                    },
                    onNavigateToFollowing = { viewedUserId ->
                        navController.navigate(SoraScreens.Following.createRoute(viewedUserId))
                    },
                    onNavigateToSettings = { },
                    onNavigateToCountryCollection = { userId, countryCode ->
                        navController.navigate(SoraScreens.CountryCollection.createRoute(userId, countryCode))
                    },
                    onNavigateToUserProfile = { userId ->
                        navController.navigate(SoraScreens.UserProfile.createRoute(userId))
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

        if (showCreatePostBottomSheet) {
            CreatePostBottomSheet(
                onDismiss = {
                    showCreatePostBottomSheet = false
                },
                onPostCreated = {
                    showCreatePostBottomSheet = false
                    feedRefreshTrigger++
                },
                cameraManager = cameraManager,
                permissionManager = permissionManager
            )
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