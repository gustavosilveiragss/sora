package com.sora.android.ui.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.sora.android.ui.theme.SoraIcons
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.sora.android.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
//import com.sora.android.R
import com.sora.android.ui.component.SoraButton
import com.sora.android.ui.component.SoraTextField
import com.sora.android.ui.viewmodel.AuthViewModel
import com.sora.android.core.error.ErrorManager
import com.sora.android.ui.components.SoraScaffoldWithErrorHandling
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    errorManager: ErrorManager = viewModel.errorManager
) {
    val uiState by viewModel.uiState.collectAsState()
    val loginForm by viewModel.loginForm.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            onNavigateToHome()
            viewModel.resetSuccessStates()
        }
    }

    SoraScaffoldWithErrorHandling(
        errorManager = errorManager
    ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp)
            .verticalScroll(scrollState)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = SoraIcons.Globe,
                contentDescription = stringResource(R.string.app_logo),
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        SoraTextField(
            value = loginForm.email,
            onValueChange = viewModel::updateLoginEmail,
            label = stringResource(R.string.email),
            placeholder = stringResource(R.string.enter_email),
            leadingIcon = {
                Icon(
                    imageVector = SoraIcons.Email,
                    contentDescription = stringResource(R.string.email),
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            isError = loginForm.emailError != null,
            errorMessage = loginForm.emailError,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        SoraTextField(
            value = loginForm.password,
            onValueChange = viewModel::updateLoginPassword,
            label = stringResource(R.string.password),
            placeholder = stringResource(R.string.enter_password),
            leadingIcon = {
                Icon(
                    imageVector = SoraIcons.Lock,
                    contentDescription = stringResource(R.string.password),
                )
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) SoraIcons.Eye else SoraIcons.EyeOff,
                        contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (loginForm.isValid) viewModel.login()
                }
            ),
            isError = loginForm.passwordError != null,
            errorMessage = loginForm.passwordError,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        SoraButton(
            text = stringResource(R.string.login),
            onClick = viewModel::login,
            enabled = loginForm.isValid && !uiState.isLoading,
            isLoading = uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { },
            enabled = !uiState.isLoading
        ) {
            Text(
                text = stringResource(R.string.forgot_password),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.dont_have_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onNavigateToRegister,
                enabled = !uiState.isLoading
            ) {
                Text(
                    text = stringResource(R.string.sign_up),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
    }
}