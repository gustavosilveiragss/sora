package com.sora.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sora.android.ui.theme.*

@Composable
fun SoraPostCard(
    username: String,
    profileImageUrl: String?,
    postImageUrls: List<String>,
    caption: String?,
    likesCount: Int,
    commentsCount: Int,
    isLiked: Boolean,
    timeAgo: String,
    onProfileClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                timeAgo = timeAgo,
                onProfileClick = onProfileClick
            )

            if (postImageUrls.isNotEmpty()) {
                AsyncImage(
                    model = postImageUrls.first(),
                    contentDescription = stringResource(R.string.post_image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
            }

            PostActions(
                isLiked = isLiked,
                onLikeClick = onLikeClick,
                onCommentClick = onCommentClick,
                onShareClick = onShareClick,
                onSaveClick = onSaveClick
            )

            PostContent(
                likesCount = likesCount,
                username = username,
                caption = caption,
                commentsCount = commentsCount,
                timeAgo = timeAgo,
                onViewComments = onCommentClick
            )
        }
    }
}

@Composable
private fun PostHeader(
    username: String,
    profileImageUrl: String?,
    timeAgo: String,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = profileImageUrl,
            contentDescription = stringResource(R.string.profile_picture),
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.dp, SoraGrayLight, CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = username,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SoraTextPrimary
            )
            Text(
                text = timeAgo,
                style = MaterialTheme.typography.bodySmall,
                color = SoraGrayLight
            )
        }

        IconButton(onClick = { /* More options */ }) {
            Icon(
                imageVector = SoraIcons.MoreVertical,
                contentDescription = stringResource(R.string.more_options),
                tint = SoraTextSecondary
            )
        }
    }
}

@Composable
private fun PostActions(
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            IconButton(onClick = onLikeClick) {
                Icon(
                    imageVector = SoraIcons.Heart,
                    contentDescription = if (isLiked) stringResource(R.string.unlike) else stringResource(R.string.like),
                    tint = if (isLiked) SoraRed else SoraTextPrimary
                )
            }

            IconButton(onClick = onCommentClick) {
                Icon(
                    imageVector = SoraIcons.MessageCircle,
                    contentDescription = stringResource(R.string.comment),
                    tint = SoraTextPrimary
                )
            }

            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = SoraIcons.Send,
                    contentDescription = stringResource(R.string.share),
                    tint = SoraTextPrimary
                )
            }
        }

        IconButton(onClick = onSaveClick) {
            Icon(
                imageVector = SoraIcons.Bookmark,
                contentDescription = stringResource(R.string.save),
                tint = SoraTextSecondary
            )
        }
    }
}

@Composable
private fun PostContent(
    likesCount: Int,
    username: String,
    caption: String?,
    commentsCount: Int,
    timeAgo: String,
    onViewComments: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        if (likesCount > 0) {
            Text(
                text = if (likesCount == 1)
                    stringResource(R.string.like_single, likesCount)
                else
                    stringResource(R.string.likes_count, likesCount),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SoraTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (!caption.isNullOrBlank()) {
            Row {
                Text(
                    text = username,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SoraTextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoraTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (commentsCount > 0) {
            Text(
                text = stringResource(R.string.view_all_comments, commentsCount),
                style = MaterialTheme.typography.bodyMedium,
                color = SoraTextSecondary,
                modifier = Modifier.clickable { onViewComments() }
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}