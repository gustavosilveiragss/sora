package com.sora.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sora.android.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.sora.android.ui.theme.SoraIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sora.android.ui.theme.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.sora.android.core.util.formatTimeAgo
import com.sora.android.ui.viewmodel.CommentViewModel

@Composable
fun SoraPostCard(
    postId: Long,
    username: String,
    profileImageUrl: String?,
    postImageUrls: List<String>,
    caption: String?,
    likesCount: Int,
    commentsCount: Int,
    isLiked: Boolean,
    timestamp: String,
    cityName: String? = null,
    isOwnPost: Boolean = false,
    currentUserId: Long? = null,
    onProfileClick: () -> Unit = {},
    onCommentAuthorProfileClick: (Long) -> Unit = {},
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timeAgo = formatTimeAgo(timestamp)
    var showComments by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SoraSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            PostHeader(
                username = username,
                profileImageUrl = profileImageUrl,
                cityName = cityName,
                timeAgo = timeAgo,
                isOwnPost = isOwnPost,
                onProfileClick = onProfileClick
            )

            if (postImageUrls.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = postImageUrls.first(),
                        contentDescription = stringResource(R.string.post_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(android.R.drawable.ic_menu_report_image),
                        placeholder = painterResource(android.R.drawable.ic_menu_gallery)
                    )
                }
            }

            PostActions(
                isLiked = isLiked,
                likesCount = likesCount,
                commentsCount = commentsCount,
                onLikeClick = onLikeClick,
                onCommentClick = {
                    showComments = true
                    onCommentClick()
                }
            )

            PostContent(
                caption = caption
            )
        }

        if (showComments) {
            val viewModel: CommentViewModel = hiltViewModel()

            LaunchedEffect(postId) {
                currentUserId?.let { viewModel.setCurrentUserId(it) }
                viewModel.loadComments(postId)
            }

            val comments by viewModel.comments.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val isPosting by viewModel.isPosting.collectAsState()

            CommentBottomSheet(
                comments = comments,
                currentUserId = currentUserId,
                isLoading = isLoading,
                isPosting = isPosting,
                onDismiss = { showComments = false },
                onPostComment = { content ->
                    viewModel.postComment(postId, content)
                },
                onReplyComment = { commentId, targetUsername, content ->
                    viewModel.replyToComment(commentId, postId, targetUsername, content)
                },
                onDeleteComment = { commentId ->
                    viewModel.deleteComment(commentId, postId)
                },
                onToggleReplies = { commentId ->
                    viewModel.toggleReplies(commentId, postId)
                },
                onLikeComment = { commentId ->
                    viewModel.likeComment(commentId, postId)
                },
                onUnlikeComment = { commentId ->
                    viewModel.unlikeComment(commentId, postId)
                },
                onProfileClick = { userId ->
                    showComments = false
                    onCommentAuthorProfileClick(userId)
                }
            )
        }
    }
}

@Composable
private fun PostHeader(
    username: String,
    profileImageUrl: String?,
    cityName: String?,
    timeAgo: String,
    isOwnPost: Boolean,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isOwnPost) {
                    Modifier.clickable { onProfileClick() }
                } else {
                    Modifier
                }
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = profileImageUrl,
            contentDescription = stringResource(R.string.profile_picture),
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = username,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SoraTextPrimary
            )

            if (!cityName.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = SoraIcons.MapPin,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = SoraTextSecondary
                    )
                    Text(
                        text = "$cityName • $timeAgo",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoraTextSecondary
                    )
                }
            } else {
                Text(
                    text = timeAgo,
                    style = MaterialTheme.typography.bodySmall,
                    color = SoraTextSecondary
                )
            }
        }

        if (isOwnPost) {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = SoraIcons.MoreHorizontal,
                    contentDescription = stringResource(R.string.more_options),
                    tint = SoraTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PostActions(
    isLiked: Boolean,
    likesCount: Int,
    commentsCount: Int,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) SoraIcons.HeartFilled else SoraIcons.Heart,
                    contentDescription = if (isLiked) stringResource(R.string.unlike) else stringResource(R.string.like),
                    tint = if (isLiked) SoraRed else SoraTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = likesCount.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = SoraTextPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onCommentClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = SoraIcons.MessageCircle,
                    contentDescription = stringResource(R.string.comment),
                    tint = SoraTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = commentsCount.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = SoraTextPrimary
            )
        }
    }
}

@Composable
private fun PostContent(
    caption: String?
) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = SoraTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}