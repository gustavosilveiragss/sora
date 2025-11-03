package com.sora.android.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FeedScreen(
    onNavigateToProfile: (Long) -> Unit = {},
    onNavigateToPost: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    refreshTrigger: Int = 0
) {
    HomeScreen(
        modifier = modifier,
        onNavigateToProfile = onNavigateToProfile,
        refreshTrigger = refreshTrigger
    )
}
