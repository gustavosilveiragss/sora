package com.sora.android.ui.screen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.sora.android.R
import com.sora.android.ui.components.SoraPostCard
import com.sora.android.ui.theme.SoraIcons
import com.sora.android.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToProfile: (Long) -> Unit = {},
    refreshTrigger: Int = 0
) {
    val posts = viewModel.feedPosts.collectAsLazyPagingItems()
    val error by viewModel.error.collectAsState()
    val likeModifications by viewModel.likeModifications.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    var showGlobe by remember { mutableStateOf(true) }

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            posts.refresh()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                        text = stringResource(R.string.home_feed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { showGlobe = !showGlobe }) {
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

            if (showGlobe) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = SoraIcons.Globe,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.globe_view_coming_soon),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                when {
                    posts.loadState.refresh is LoadState.Loading -> {
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
                    posts.loadState.refresh is LoadState.Error -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.error_loading_feed),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { posts.refresh() }) {
                                    Text(text = stringResource(R.string.try_again))
                                }
                            }
                        }
                    }
                    posts.itemCount == 0 -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.feed_empty_title),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.feed_empty_message),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        items(
                            count = posts.itemCount,
                            key = { index -> posts[index]?.id ?: index },
                            contentType = { "post" }
                        ) { index ->
                            posts[index]?.let { post ->
                                val countryName = getCountryDisplayName(post.country.nameKey, post.country.code)
                                val imageUrls = remember(post.id) {
                                    post.media.map { it.cloudinaryUrl }
                                }

                                val modification = likeModifications[post.id]
                                val displayLiked = modification?.isLiked ?: post.isLikedByCurrentUser
                                val displayLikesCount = modification?.likesCount ?: post.likesCount

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
                                    countryName = countryName,
                                    isOwnPost = currentUserId != null && post.author.id == currentUserId,
                                    currentUserId = currentUserId,
                                    onProfileClick = { onNavigateToProfile(post.author.id) },
                                    onLikeClick = {
                                        viewModel.toggleLike(
                                            post.id,
                                            post.isLikedByCurrentUser,
                                            post.likesCount
                                        )
                                    },
                                    onCommentAuthorProfileClick = onNavigateToProfile,
                                    modifier = Modifier
                                        .padding(bottom = 8.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            }
                        }

                        when {
                            posts.loadState.append is LoadState.Loading -> {
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
                            posts.loadState.append is LoadState.Error -> {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = stringResource(R.string.error_loading_more),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Button(onClick = { posts.retry() }) {
                                            Text(text = stringResource(R.string.try_again))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        error?.let { errorMessage ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(text = stringResource(R.string.dismiss))
                    }
                }
            ) {
                Text(text = errorMessage)
            }
        }
    }
}

@Composable
private fun getCountryDisplayName(countryNameKey: String, fallbackCode: String): String {
    val context = LocalContext.current
    val translationManager = dagger.hilt.android.EntryPointAccessors.fromApplication(
        context.applicationContext,
        TranslationManagerEntryPoint::class.java
    ).translationManager()

    return translationManager.translateAny(countryNameKey, fallbackCode)
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface TranslationManagerEntryPoint {
    fun translationManager(): com.sora.android.core.translation.TranslationManager
}
