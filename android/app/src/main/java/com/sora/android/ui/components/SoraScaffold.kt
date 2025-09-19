package com.sora.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.sora.android.core.error.ErrorManager
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoraScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.background,
    contentColor: androidx.compose.ui.graphics.Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoraScaffoldWithErrorHandling(
    errorManager: ErrorManager,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.background,
    contentColor: androidx.compose.ui.graphics.Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit
) {
    val errorState by errorManager.errorState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        snackbarHost = {
            ErrorSnackbar(
                snackbarHostState = errorManager.snackbarHostState
            )
        },
        content = content
    )

    // Global Error Dialog
    ErrorDialog(
        errorState = errorState,
        onDismiss = errorManager::hideError
    )
}