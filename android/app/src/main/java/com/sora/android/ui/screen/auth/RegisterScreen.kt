package com.sora.android.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sora.android.R
import com.sora.android.ui.theme.SoraIcons
import com.sora.android.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val registerForm by viewModel.registerForm.collectAsStateWithLifecycle()

    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isRegisterSuccess) {
        if (uiState.isRegisterSuccess) {
            viewModel.resetSuccessStates()
            onNavigateToHome()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            viewModel.errorManager.showSnackbar(error)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(viewModel.errorManager.snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Text(
            text = stringResource(R.string.register),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = registerForm.username,
            onValueChange = viewModel::updateRegisterUsername,
            label = { Text(stringResource(R.string.username)) },
            leadingIcon = { Icon(SoraIcons.AtSign, stringResource(R.string.username), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            isError = registerForm.usernameError != null,
            supportingText = registerForm.usernameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = registerForm.email,
            onValueChange = viewModel::updateRegisterEmail,
            label = { Text(stringResource(R.string.email)) },
            leadingIcon = { Icon(SoraIcons.Email, stringResource(R.string.email), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            isError = registerForm.emailError != null,
            supportingText = registerForm.emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = registerForm.firstName,
            onValueChange = viewModel::updateRegisterFirstName,
            label = { Text(stringResource(R.string.first_name)) },
            leadingIcon = { Icon(SoraIcons.User, stringResource(R.string.first_name), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            isError = registerForm.firstNameError != null,
            supportingText = registerForm.firstNameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = registerForm.lastName,
            onValueChange = viewModel::updateRegisterLastName,
            label = { Text(stringResource(R.string.last_name)) },
            leadingIcon = { Icon(SoraIcons.Users, stringResource(R.string.last_name), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            isError = registerForm.lastNameError != null,
            supportingText = registerForm.lastNameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = registerForm.password,
            onValueChange = viewModel::updateRegisterPassword,
            label = { Text(stringResource(R.string.password)) },
            leadingIcon = { Icon(SoraIcons.Lock, stringResource(R.string.password), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) SoraIcons.EyeOff else SoraIcons.Eye,
                        contentDescription = if (showPassword) stringResource(R.string.hide_password) else stringResource(R.string.show_password),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            isError = registerForm.passwordError != null,
            supportingText = registerForm.passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = registerForm.confirmPassword,
            onValueChange = viewModel::updateRegisterConfirmPassword,
            label = { Text(stringResource(R.string.confirm_password)) },
            leadingIcon = { Icon(SoraIcons.Lock, stringResource(R.string.confirm_password), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                    Icon(
                        if (showConfirmPassword) SoraIcons.EyeOff else SoraIcons.Eye,
                        contentDescription = if (showConfirmPassword) stringResource(R.string.hide_password) else stringResource(R.string.show_password),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            isError = registerForm.confirmPasswordError != null,
            supportingText = registerForm.confirmPasswordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.register() },
            enabled = registerForm.isValid && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(stringResource(R.string.register))
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text(stringResource(R.string.already_have_account))
        }
        }
    }
}