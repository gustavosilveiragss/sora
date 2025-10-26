package com.sora.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sora.android.ui.screen.auth.LoginScreen
import com.sora.android.ui.screen.auth.RegisterScreen
import com.sora.android.ui.screen.auth.SplashScreen
import com.sora.android.ui.screen.MainNavigationScreen
import com.sora.android.ui.viewmodel.AuthViewModel

@Composable
fun SoraNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val isInitialized by authViewModel.isInitialized.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (!isInitialized) {
            SoraScreens.Splash.route
        } else if (isLoggedIn) {
            SoraScreens.MainGlobe.route
        } else {
            SoraScreens.Login.route
        }
    ) {
        composable(SoraScreens.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(SoraScreens.Login.route) {
                        popUpTo(SoraScreens.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(SoraScreens.MainGlobe.route) {
                        popUpTo(SoraScreens.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(SoraScreens.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(SoraScreens.Register.route)
                },
                onNavigateToHome = {
                    navController.navigate(SoraScreens.MainGlobe.route) {
                        popUpTo(SoraScreens.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(SoraScreens.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(SoraScreens.MainGlobe.route) {
                        popUpTo(SoraScreens.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(SoraScreens.MainGlobe.route) {
            MainNavigationScreen()
        }
    }
}