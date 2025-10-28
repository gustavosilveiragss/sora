package com.sora.android.ui.components

import androidx.compose.foundation.layout.*
import com.sora.android.ui.theme.SoraIcons
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.WindowInsets
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
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = SoraIcons.ArrowLeft,
                    contentDescription = stringResource(R.string.back),
                    tint = SoraTextPrimary
                )
            }
        },
        actions = actions ?: {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SoraSurface,
            titleContentColor = SoraTextPrimary,
            actionIconContentColor = SoraTextPrimary
        ),
        windowInsets = WindowInsets(0.dp)
    )
}