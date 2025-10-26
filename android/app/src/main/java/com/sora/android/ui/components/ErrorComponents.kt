package com.sora.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sora.android.R
import com.sora.android.core.error.ErrorState
import com.sora.android.core.error.ErrorType
import com.sora.android.ui.theme.*

@Composable
fun ErrorDialog(
    errorState: ErrorState,
    onDismiss: () -> Unit
) {
    if (errorState.isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnClickOutside = true,
                dismissOnBackPress = true
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = SoraIcons.AlertCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.error),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = getErrorMessage(errorState.message, errorState.type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (errorState.actionLabel != null && errorState.onAction != null) {
                            Button(
                                onClick = {
                                    errorState.onAction.invoke()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(errorState.actionLabel)
                            }
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorSnackbar(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
    ) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            actionColor = MaterialTheme.colorScheme.error,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = SoraIcons.AlertCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.error),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

object ErrorComponents {
    @Composable
    fun ErrorMessage(
        message: String,
        onRetry: (() -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        com.sora.android.ui.components.ErrorMessage(
            message = message,
            onRetry = onRetry,
            modifier = modifier
        )
    }
}

@Composable
private fun getErrorMessage(message: String, type: ErrorType): String {
    return when {
        message.startsWith("error_") -> {
            when (message) {
                "error_network_connection" -> stringResource(R.string.error_network_connection)
                "error_network_timeout" -> stringResource(R.string.error_network_timeout)
                "error_server_unavailable" -> stringResource(R.string.error_server_unavailable)
                "error_invalid_credentials" -> stringResource(R.string.error_invalid_credentials)
                "error_validation" -> stringResource(R.string.error_validation)
                "error_unknown" -> stringResource(R.string.error_unknown)
                else -> stringResource(R.string.error_generic)
            }
        }
        else -> {
            message.ifBlank { stringResource(R.string.error_generic) }
        }
    }
}