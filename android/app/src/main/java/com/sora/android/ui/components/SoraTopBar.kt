package com.sora.android.ui.components

import androidx.compose.foundation.layout.*
import com.sora.android.ui.theme.SoraIcons
import androidx.compose.ui.res.stringResource
import com.sora.android.R
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sora.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoraTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SoraTextPrimary
            )
        },
        modifier = modifier,
        navigationIcon = navigationIcon ?: {},
        actions = actions ?: {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SoraSurface,
            titleContentColor = SoraTextPrimary,
            actionIconContentColor = SoraTextPrimary
        )
    )
}

@Composable
fun SoraActionIcon(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = SoraTextPrimary
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

@Composable
fun SoraInstagramTopBar(
    onCameraClick: () -> Unit = {},
    onDirectClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SoraActionIcon(
            icon = SoraIcons.Camera,
            contentDescription = stringResource(R.string.camera),
            onClick = onCameraClick
        )

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = SoraTextPrimary,
            textAlign = TextAlign.Center
        )

        SoraActionIcon(
            icon = SoraIcons.MessageSquare,
            contentDescription = stringResource(R.string.direct_messages),
            onClick = onDirectClick
        )
    }
}