package com.sora.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sora.android.R
import com.sora.android.core.util.formatTimeAgo
import com.sora.android.domain.model.CommentModel
import com.sora.android.ui.theme.*
import com.sora.android.ui.viewmodel.CommentWithReplies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    comments: List<CommentWithReplies>,
    currentUserId: Long?,
    isLoading: Boolean,
    isPosting: Boolean,
    onDismiss: () -> Unit,
    onPostComment: (String) -> Unit,
    onReplyComment: (Long, String, String) -> Unit,
    onDeleteComment: (Long) -> Unit,
    onToggleReplies: (Long) -> Unit,
    onLikeComment: (Long) -> Unit,
    onUnlikeComment: (Long) -> Unit,
    onProfileClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var commentText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<Pair<Long, String>?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        containerColor = SoraSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SoraTextSecondary.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
        ) {
            Text(
                text = stringResource(R.string.comments),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SoraTextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textAlign = TextAlign.Center
            )

            Divider(color = SoraTextSecondary.copy(alpha = 0.1f))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = SoraPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                comments.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(comments) { commentWithReplies ->
                            CommentItem(
                                comment = commentWithReplies.comment,
                                replies = commentWithReplies.replies,
                                isExpanded = commentWithReplies.isExpanded,
                                currentUserId = currentUserId,
                                onReply = { commentId, username ->
                                    replyingTo = Pair(commentId, username)
                                },
                                onDelete = onDeleteComment,
                                onToggleReplies = onToggleReplies,
                                onLikeComment = onLikeComment,
                                onUnlikeComment = onUnlikeComment,
                                onProfileClick = onProfileClick
                            )
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.no_comments_yet),
                                style = MaterialTheme.typography.bodyLarge,
                                color = SoraTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.be_first_to_comment),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SoraTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Divider(color = SoraTextSecondary.copy(alpha = 0.1f))

            CommentInputField(
                commentText = commentText,
                onCommentTextChange = { commentText = it },
                replyingTo = replyingTo?.second,
                onCancelReply = { replyingTo = null },
                isPosting = isPosting,
                onPost = {
                    if (commentText.isNotBlank()) {
                        replyingTo?.let { (commentId, username) ->
                            onReplyComment(commentId, username, commentText)
                        } ?: onPostComment(commentText)
                        commentText = ""
                        replyingTo = null
                    }
                }
            )
        }
    }
}

@Composable
private fun CommentItem(
    comment: CommentModel,
    replies: List<CommentModel> = emptyList(),
    isExpanded: Boolean = false,
    currentUserId: Long?,
    onReply: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onToggleReplies: (Long) -> Unit = {},
    onLikeComment: (Long) -> Unit,
    onUnlikeComment: (Long) -> Unit,
    onProfileClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = comment.author.profilePicture,
            contentDescription = stringResource(R.string.profile_picture),
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(
                    if (currentUserId != null && comment.author.id != currentUserId) {
                        Modifier.clickable { onProfileClick(comment.author.id) }
                    } else {
                        Modifier
                    }
                ),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = comment.author.username,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SoraTextPrimary,
                    modifier = if (currentUserId != null && comment.author.id != currentUserId) {
                        Modifier.clickable { onProfileClick(comment.author.id) }
                    } else {
                        Modifier
                    }
                )

                Text(
                    text = formatTimeAgo(comment.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = SoraTextSecondary
                )
            }

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = SoraTextPrimary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (comment.repliesCount > 0) {
                    Text(
                        text = if (isExpanded) {
                            stringResource(R.string.hide_replies)
                        } else {
                            stringResource(R.string.view_replies, comment.repliesCount)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SoraTextSecondary,
                        modifier = Modifier.clickable {
                            onToggleReplies(comment.id)
                        }
                    )
                }

                Text(
                    text = stringResource(R.string.reply),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SoraTextSecondary,
                    modifier = Modifier.clickable {
                        onReply(comment.id, comment.author.username)
                    }
                )

                if (currentUserId != null && comment.author.id == currentUserId) {
                    Text(
                        text = stringResource(R.string.delete_comment),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SoraTextSecondary,
                        modifier = Modifier.clickable {
                            onDelete(comment.id)
                        }
                    )
                }
            }
        }

        Icon(
            imageVector = if (comment.isLikedByCurrentUser) SoraIcons.HeartFilled else SoraIcons.Heart,
            contentDescription = stringResource(R.string.like),
            modifier = Modifier
                .size(16.dp)
                .clickable {
                    if (comment.isLikedByCurrentUser) {
                        onUnlikeComment(comment.id)
                    } else {
                        onLikeComment(comment.id)
                    }
                },
            tint = if (comment.isLikedByCurrentUser) SoraRed else SoraTextSecondary
        )
    }

    if (isExpanded && replies.isNotEmpty()) {
        Column(
            modifier = Modifier.padding(start = 44.dp)
        ) {
            replies.forEach { reply ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = reply.author.profilePicture,
                        contentDescription = stringResource(R.string.profile_picture),
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .then(
                                if (currentUserId != null && reply.author.id != currentUserId) {
                                    Modifier.clickable { onProfileClick(reply.author.id) }
                                } else {
                                    Modifier
                                }
                            ),
                        contentScale = ContentScale.Crop
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = reply.author.username,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = SoraTextPrimary,
                                modifier = if (currentUserId != null && reply.author.id != currentUserId) {
                                    Modifier.clickable { onProfileClick(reply.author.id) }
                                } else {
                                    Modifier
                                }
                            )
                            Text(
                                text = formatTimeAgo(reply.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = SoraTextSecondary
                            )
                        }

                        Text(
                            text = reply.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = SoraTextPrimary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.reply),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = SoraTextSecondary,
                                modifier = Modifier.clickable {
                                    onReply(comment.id, reply.author.username)
                                }
                            )

                            if (currentUserId != null && reply.author.id == currentUserId) {
                                Text(
                                    text = stringResource(R.string.delete_comment),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = SoraTextSecondary,
                                    modifier = Modifier.clickable {
                                        onDelete(reply.id)
                                    }
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = if (reply.isLikedByCurrentUser) SoraIcons.HeartFilled else SoraIcons.Heart,
                        contentDescription = stringResource(R.string.like),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                if (reply.isLikedByCurrentUser) {
                                    onUnlikeComment(reply.id)
                                } else {
                                    onLikeComment(reply.id)
                                }
                            },
                        tint = if (reply.isLikedByCurrentUser) SoraRed else SoraTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentInputField(
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    replyingTo: String?,
    onCancelReply: () -> Unit,
    isPosting: Boolean,
    onPost: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SoraSurface)
    ) {
        if (replyingTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoraTextSecondary.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.reply) + " @$replyingTo",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoraTextSecondary
                )
                Icon(
                    imageVector = SoraIcons.X,
                    contentDescription = stringResource(R.string.cancel),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onCancelReply() },
                    tint = SoraTextSecondary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = commentText,
                onValueChange = onCommentTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = stringResource(R.string.add_comment),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoraTextSecondary
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = SoraTextPrimary,
                    unfocusedTextColor = SoraTextPrimary
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                enabled = !isPosting
            )

            if (commentText.isNotBlank()) {
                if (isPosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = SoraPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.post_comment),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SoraPrimary,
                        modifier = Modifier.clickable { onPost() }
                    )
                }
            }
        }
    }
}
