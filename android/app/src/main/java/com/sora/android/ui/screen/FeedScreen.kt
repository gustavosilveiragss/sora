package com.sora.android.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FeedScreen(
    onNavigateToProfile: (Long) -> Unit = {},
    onNavigateToPost: (Long) -> Unit = {},
    onNavigateToCountryCollection: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier,
    refreshTrigger: Int = 0
) {
    HomeScreen(
        modifier = modifier,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToCountryCollection = onNavigateToCountryCollection,
        refreshTrigger = refreshTrigger
    )
}
