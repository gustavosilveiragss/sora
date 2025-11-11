package com.sora.android.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.sora.android.R
import com.sora.android.domain.model.UserModel
import com.sora.android.ui.components.SearchBar
import com.sora.android.ui.components.SoraPostCard
import com.sora.android.ui.components.UserListItem
import com.sora.android.ui.screen.explore.components.ExploreGlobe
import com.sora.android.ui.theme.SoraIcons
import com.sora.android.ui.theme.SoraTextSecondary
import com.sora.android.ui.viewmodel.ExploreViewModel
import com.sora.android.ui.viewmodel.ExploreGlobeViewModel

@Composable
fun ExploreScreen(
    onNavigateToProfile: (Long) -> Unit,
    onNavigateToComments: (Long) -> Unit = {},
    onNavigateToCountryCollection: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    viewModel: ExploreViewModel = hiltViewModel(),
    globeViewModel: ExploreGlobeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val explorePosts = viewModel.explorePosts.collectAsLazyPagingItems()
    val likeModifications by viewModel.likeModifications.collectAsState()

    val globeData by globeViewModel.globeData.collectAsState()
    val userLocation by globeViewModel.userLocation.collectAsState()
    val selectedTimeframe by globeViewModel.selectedTimeframe.collectAsState()
    val isGlobeLoading by globeViewModel.isLoading.collectAsState()

    var showGlobe by remember { mutableStateOf(true) }
    val isGlobeInteracting = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = stringResource(R.string.search_users_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (uiState.hasSearched && uiState.searchQuery.isNotBlank()) {
                SearchResultsOverlay(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateToProfile = onNavigateToProfile
                )
            } else {
                ExploreContent(
                    showGlobe = showGlobe,
                    onToggleView = { showGlobe = !showGlobe },
                    explorePosts = explorePosts,
                    likeModifications = likeModifications,
                    currentUserId = uiState.currentUserId,
                    selectedTimeframe = selectedTimeframe,
                    onTimeframeChange = { timeframe ->
                        viewModel.setTimeframe(timeframe)
                        globeViewModel.setTimeframe(timeframe)
                    },
                    globeData = globeData,
                    userLocation = userLocation,
                    isGlobeLoading = isGlobeLoading,
                    isGlobeInteracting = isGlobeInteracting,
                    onCountryClick = { countryCode ->
                        uiState.currentUserId?.let { userId ->
                            onNavigateToCountryCollection(userId, countryCode, selectedTimeframe, "likesCount")
                        }
                    },
                    onToggleLike = viewModel::toggleLike,
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToComments = onNavigateToComments
                )
            }
        }
    }
}

@Composable
private fun SearchResultsOverlay(
    uiState: ExploreViewModel.ExploreUiState,
    viewModel: ExploreViewModel,
    onNavigateToProfile: (Long) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                LoadingShimmer()
            }

            uiState.error != null -> {
                ErrorState(
                    message = uiState.error,
                    onRetry = viewModel::retry
                )
            }

            uiState.searchResults.isEmpty() -> {
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

@Composable
private fun ExploreContent(
    showGlobe: Boolean,
    onToggleView: () -> Unit,
    explorePosts: androidx.paging.compose.LazyPagingItems<com.sora.android.domain.model.PostModel>,
    likeModifications: Map<Long, com.sora.android.ui.viewmodel.LikeModification>,
    currentUserId: Long?,
    selectedTimeframe: String,
    onTimeframeChange: (String) -> Unit,
    globeData: com.sora.android.domain.model.GlobeDataModel?,
    userLocation: com.mapbox.geojson.Point?,
    isGlobeLoading: Boolean,
    isGlobeInteracting: MutableState<Boolean>,
    onCountryClick: (String) -> Unit,
    onToggleLike: (Long, Boolean, Int) -> Unit,
    onNavigateToProfile: (Long) -> Unit,
    onNavigateToComments: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = !isGlobeInteracting.value,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.explore_trending),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onToggleView) {
                    Icon(
                        imageVector = if (showGlobe) SoraIcons.Menu else SoraIcons.Globe,
                        contentDescription = if (showGlobe)
                            stringResource(R.string.profile_show_list)
                        else
                            stringResource(R.string.profile_show_globe),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            TimeframeSelector(
                selectedTimeframe = selectedTimeframe,
                onTimeframeChange = onTimeframeChange
            )
        }

        if (showGlobe) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(550.dp)
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    ExploreGlobe(
                        globeData = globeData,
                        userLocation = userLocation,
                        isLoading = isGlobeLoading,
                        onCountryClick = onCountryClick,
                        isGlobeInteracting = isGlobeInteracting
                    )
                }
            }
        } else {
            when {
                explorePosts.loadState.refresh is LoadState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                explorePosts.loadState.refresh is LoadState.Error -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.error_loading_explore),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { explorePosts.refresh() }) {
                                Text(text = stringResource(R.string.try_again))
                            }
                        }
                    }
                }
                explorePosts.itemCount == 0 -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.explore_empty_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.explore_empty_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    items(
                        count = explorePosts.itemCount,
                        key = { index -> explorePosts[index]?.id ?: index },
                        contentType = { "post" }
                    ) { index ->
                        explorePosts[index]?.let { post ->
                            val modification = likeModifications[post.id]
                            val displayLiked = modification?.isLiked ?: post.isLikedByCurrentUser
                            val displayLikesCount = modification?.likesCount ?: post.likesCount

                            val imageUrls = remember(post.id) {
                                post.media.map { it.cloudinaryUrl }
                            }

                            SoraPostCard(
                                postId = post.id,
                                username = post.author.username,
                                profileImageUrl = post.author.profilePicture,
                                postImageUrls = imageUrls,
                                caption = post.caption,
                                likesCount = displayLikesCount,
                                commentsCount = post.commentsCount,
                                isLiked = displayLiked,
                                timestamp = post.createdAt,
                                cityName = post.cityName,
                                countryName = post.country.nameKey,
                                isOwnPost = currentUserId != null && post.author.id == currentUserId,
                                currentUserId = currentUserId,
                                onProfileClick = { onNavigateToProfile(post.author.id) },
                                onCommentAuthorProfileClick = onNavigateToProfile,
                                onLikeClick = {
                                    onToggleLike(post.id, displayLiked, displayLikesCount)
                                },
                                onCommentClick = { onNavigateToComments(post.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    if (explorePosts.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeframeSelector(
    selectedTimeframe: String,
    onTimeframeChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimeframeChip(
            label = stringResource(R.string.explore_timeframe_week),
            isSelected = selectedTimeframe == "week",
            onClick = { onTimeframeChange("week") },
            modifier = Modifier.weight(1f)
        )
        TimeframeChip(
            label = stringResource(R.string.explore_timeframe_month),
            isSelected = selectedTimeframe == "month",
            onClick = { onTimeframeChange("month") },
            modifier = Modifier.weight(1f)
        )
        TimeframeChip(
            label = stringResource(R.string.explore_timeframe_all),
            isSelected = selectedTimeframe == "all",
            onClick = { onTimeframeChange("all") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimeframeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        modifier = modifier
    )
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
