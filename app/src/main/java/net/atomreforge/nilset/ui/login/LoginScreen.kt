package net.atomreforge.nilset.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import net.atomreforge.nilset.R

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    snackbarScope: CoroutineScope,
    onNavigateToConsole: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.loginMessage) {
        uiState.loginMessage?.let { message ->
            val shouldNavigateHome = uiState.isLoginSuccess
            viewModel.onLoginMessageShown()
            if (shouldNavigateHome) onNavigateToHome()
            snackbarScope.launch {
                val displayJob = launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Indefinite,
                    )
                }
                delay(1200)
                if (snackbarHostState.currentSnackbarData?.visuals?.message == message) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                }
                displayJob.join()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.weight(1f))

        OutlinedTextField(
            value = uiState.username,
            onValueChange = viewModel::onUsernameChanged,
            label = { Text(stringResource(R.string.username_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChanged,
            label = { Text(stringResource(R.string.password_hint)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            Button(
                onClick = viewModel::onLoginClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.btn_login), style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onNavigateToConsole,
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.console_link),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
