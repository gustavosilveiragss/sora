package com.sora.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sora.android.R
import com.sora.android.domain.model.UserModel
import com.sora.android.domain.model.UserSearchResultModel
import com.sora.android.ui.components.SearchBar
import com.sora.android.ui.components.SoraScaffold
import com.sora.android.ui.components.UserListItem
import com.sora.android.ui.theme.SoraIcons
import com.sora.android.ui.theme.SoraTextSecondary
import com.sora.android.ui.viewmodel.ExploreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onNavigateToProfile: (Long) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    SoraScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.explore),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues).padding(top = 5.dp)
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = stringResource(R.string.search_users_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                uiState.isLoading -> {
                    LoadingShimmer()
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = viewModel::retry
                    )
                }

                uiState.searchResults.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    EmptyResultsState()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.searchResults) { searchResult ->
                            val userModel = UserModel(
                                id = searchResult.id,
                                username = searchResult.username,
                                firstName = searchResult.firstName,
                                lastName = searchResult.lastName,
                                profilePicture = searchResult.profilePicture,
                                bio = null,
                                followersCount = 0,
                                followingCount = 0,
                                countriesVisitedCount = 0
                            )

                            UserListItem(
                                user = userModel,
                                onClick = { onNavigateToProfile(searchResult.id) },
                                showFollowButton = true,
                                isFollowing = uiState.followingUserIds.contains(searchResult.id),
                                onFollowClick = {
                                    viewModel.toggleFollow(
                                        searchResult.id,
                                        uiState.followingUserIds.contains(searchResult.id)
                                    )
                                },
                                currentUserId = uiState.currentUserId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyInitialState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = SoraIcons.Search,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = SoraTextSecondary
            )

            Text(
                text = stringResource(R.string.search_users_to_start),
                style = MaterialTheme.typography.bodyLarge,
                color = SoraTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyResultsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = SoraIcons.Users,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = SoraTextSecondary
            )

            Text(
                text = stringResource(R.string.no_results_found),
                style = MaterialTheme.typography.bodyLarge,
                color = SoraTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = SoraIcons.AlertCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = SoraTextSecondary
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = SoraTextSecondary,
                textAlign = TextAlign.Center
            )

            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun LoadingShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) {
            UserItemShimmer()
        }
    }
}

@Composable
private fun UserItemShimmer() {
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surfaceVariant
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(shimmerBrush, CircleShape)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .background(shimmerBrush, RoundedCornerShape(4.dp))
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .background(shimmerBrush, RoundedCornerShape(4.dp))
                )
            }

            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(32.dp)
                    .background(shimmerBrush, RoundedCornerShape(16.dp))
            )
        }
    }
}
