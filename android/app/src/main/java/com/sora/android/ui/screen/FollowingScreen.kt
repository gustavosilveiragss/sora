package com.sora.android.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sora.android.R
import com.sora.android.ui.components.LeaderboardCard
import com.sora.android.ui.components.SearchBar
import com.sora.android.ui.components.SoraTopBar
import com.sora.android.ui.components.UserListItem
import com.sora.android.ui.viewmodel.FollowingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
    userId: Long,
    onBackClick: () -> Unit,
    onUserClick: (Long) -> Unit,
    viewModel: FollowingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadFollowing(userId)
    }

    Scaffold(
        topBar = {
            SoraTopBar(
                title = stringResource(R.string.following),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { viewModel.loadFollowing(userId) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }

            uiState.following.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_following),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = viewModel::updateSearchQuery,
                            placeholder = stringResource(R.string.search_following)
                        )
                    }

                    if (viewModel.isCurrentUserProfile()) {
                        item {
                            LeaderboardCard(
                                leaderboard = uiState.leaderboard,
                                isLoading = uiState.isLoadingLeaderboard,
                                onMetricChange = viewModel::loadLeaderboard
                            )
                        }
                    }

                    if (uiState.filteredFollowing.isEmpty() && uiState.searchQuery.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_results_found),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(uiState.filteredFollowing) { followedUser ->
                            UserListItem(
                                user = followedUser,
                                onClick = { onUserClick(followedUser.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}