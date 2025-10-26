package com.sora.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.sora.android.R
import com.sora.android.domain.model.CollectionCode
import com.sora.android.domain.model.PostModel
import com.sora.android.ui.theme.SoraIcons
import com.sora.android.ui.theme.SoraTextSecondary

data class PostListFilters(
    val collectionCode: CollectionCode? = null,
    val cityName: String? = null,
    val sortBy: String = "createdAt",
    val sortDirection: String = "DESC"
)

@Composable
fun FilterablePostList(
    posts: LazyPagingItems<PostModel>,
    filters: PostListFilters,
    currentUserId: Long?,
    postsLoaded: Boolean = false,
    onFilterChange: (PostListFilters) -> Unit,
    onLikeClick: (postId: Long, isLiked: Boolean, likesCount: Int) -> Unit,
    onCommentClick: (Long) -> Unit,
    onProfileClick: (Long) -> Unit,
    showFilters: Boolean = true,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    header: (@Composable () -> Unit)? = null
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        reverseLayout = false
    ) {
        if (header != null) {
            item {
                header()
            }
        }

        if (showFilters) {
            item {
                CollectionFilterTabRow(
                    selectedCollection = filters.collectionCode,
                    onCollectionSelected = { collection ->
                        onFilterChange(filters.copy(collectionCode = collection))
                    }
                )
            }
        }

        when {
            !postsLoaded -> {
                items(3) {
                    PostLoadingShimmer()
                }
            }

            posts.loadState.refresh is LoadState.Error -> {
                item {
                    PostListErrorState(
                        onRetry = { posts.retry() }
                    )
                }
            }

            posts.itemCount == 0 -> {
                item {
                    PostListEmptyState(
                        hasFilter = filters.collectionCode != null
                    )
                }
            }

            else -> {
                items(
                    count = posts.itemCount,
                    key = posts.itemKey { it.id },
                    contentType = { "post" }
                ) { index ->
                    posts[index]?.let { post ->
                        val onProfileClickStable = remember(post.author.id) {
                            { onProfileClick(post.author.id) }
                        }
                        val onLikeClickStable = remember(post.id, post.isLikedByCurrentUser, post.likesCount) {
                            { onLikeClick(post.id, post.isLikedByCurrentUser, post.likesCount) }
                        }
                        val onCommentClickStable = remember(post.id) {
                            { onCommentClick(post.id) }
                        }

                        SoraPostCard(
                            postId = post.id,
                            username = post.author.username,
                            profileImageUrl = post.author.profilePicture,
                            postImageUrls = post.mediaUrls,
                            caption = post.caption,
                            likesCount = post.likesCount,
                            commentsCount = post.commentsCount,
                            isLiked = post.isLikedByCurrentUser,
                            timestamp = post.createdAt,
                            cityName = post.cityName,
                            isOwnPost = currentUserId != null && post.author.id == currentUserId,
                            currentUserId = currentUserId,
                            onProfileClick = onProfileClickStable,
                            onLikeClick = onLikeClickStable,
                            onCommentClick = onCommentClickStable,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                when (posts.loadState.append) {
                    is LoadState.Loading -> {
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

                    is LoadState.Error -> {
                        item {
                            PostListLoadMoreError(
                                onRetry = { posts.retry() }
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun PostLoadingShimmer() {
    val shimmer = shimmerBrush()
    val shape = MaterialTheme.shapes.small

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(shimmer, shape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(16.dp)
                            .background(shimmer, shape)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(12.dp)
                            .background(shimmer, shape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(shimmer, shape)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .background(shimmer, shape)
            )
        }
    }
}

@Composable
private fun PostListErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = SoraIcons.AlertCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = SoraTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.post_list_error),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = SoraTextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.post_list_retry))
        }
    }
}

@Composable
private fun PostListEmptyState(
    hasFilter: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = SoraIcons.Image,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = SoraTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(
                if (hasFilter) R.string.post_list_empty_filter
                else R.string.post_list_empty
            ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = SoraTextSecondary
        )
    }
}

@Composable
private fun PostListLoadMoreError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.post_list_error),
            style = MaterialTheme.typography.bodyMedium,
            color = SoraTextSecondary
        )

        Spacer(modifier = Modifier.width(8.dp))

        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.post_list_retry))
        }
    }
}

@Composable
private fun shimmerBrush(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surfaceVariant
        )
    )
}
