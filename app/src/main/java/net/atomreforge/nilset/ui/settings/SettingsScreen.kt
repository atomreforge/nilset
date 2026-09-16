package net.atomreforge.nilset.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.atomreforge.nilset.R
import net.atomreforge.nilset.core.theme.ThemeMode
import net.atomreforge.nilset.data.remote.ServerConnectionStatus
import net.atomreforge.nilset.ui.theme.themeContainerColor
import net.atomreforge.nilset.ui.theme.themeContainerBorderColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    onOpenConsole: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenThemeSettings: () -> Unit,
    onOpenCustomSettings: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeSettings by viewModel.themeSettings.collectAsStateWithLifecycle()
    val serverConnection by viewModel.serverConnection.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val isLoggedOut by viewModel.isLoggedOut.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val modeLabel = stringResource(
        when (themeSettings.mode) {
            ThemeMode.LIGHT -> R.string.theme_mode_light
            ThemeMode.DARK -> R.string.theme_mode_dark
        },
    )

    LaunchedEffect(isLoggedOut) {
        if (isLoggedOut) onLoggedOut()
    }
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeContainerColor(),
                ),
                actions = {
                    val isRetryEnabled = serverConnection.status == ServerConnectionStatus.DISCONNECTED &&
                        serverConnection.canRetry
                    val statusMessage = stringResource(
                        when {
                            serverConnection.status == ServerConnectionStatus.CONNECTED ->
                                R.string.settings_server_connected
                            serverConnection.status == ServerConnectionStatus.CHECKING ->
                                R.string.settings_server_checking
                            isRetryEnabled -> R.string.settings_server_retrying
                            else -> R.string.settings_server_retry_cooldown
                        },
                    )
                    IconButton(
                        onClick = {
                            if (isRetryEnabled) {
                                viewModel.retryServerConnection()
                            }
                            snackbarScope.launch {
                                snackbarHostState.showSnackbar(statusMessage)
                            }
                        },
                        modifier = Modifier.offset(x = (-6).dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (serverConnection.status == ServerConnectionStatus.CONNECTED) {
                                    R.drawable.ic_tick
                                } else {
                                    R.drawable.ic_cross
                                },
                            ),
                            contentDescription = stringResource(
                                when (serverConnection.status) {
                                    ServerConnectionStatus.CONNECTED -> R.string.settings_server_connected
                                    ServerConnectionStatus.CHECKING -> R.string.settings_server_checking
                                    else -> R.string.settings_server_disconnected
                                },
                            ),
                            tint = when (serverConnection.status) {
                                ServerConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                                ServerConnectionStatus.CHECKING -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.error
                            },
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsUserCard(
                userInfo = sessionState.userInfo,
                fallbackUsername = sessionState.username,
                showBorder = themeSettings.showCardBorders,
                onLogout = viewModel::logout,
            )

            Surface(
                onClick = onOpenConsole,
                shape = RoundedCornerShape(8.dp),
                border = if (themeSettings.showCardBorders) {
                    BorderStroke(1.dp, themeContainerBorderColor())
                } else {
                    null
                },
                color = themeContainerColor(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_open_console))
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_console),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            Surface(
                onClick = onOpenNotificationSettings,
                shape = RoundedCornerShape(8.dp),
                border = if (themeSettings.showCardBorders) {
                    BorderStroke(1.dp, themeContainerBorderColor())
                } else {
                    null
                },
                color = themeContainerColor(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_notification))
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_notification),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            Surface(
                onClick = onOpenThemeSettings,
                shape = RoundedCornerShape(8.dp),
                border = if (themeSettings.showCardBorders) {
                    BorderStroke(1.dp, themeContainerBorderColor())
                } else {
                    null
                },
                color = themeContainerColor(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_theme))
                    },
                    supportingContent = {
                        Text(
                            text = buildString {
                                append(themeSettings.palette.label)
                                append(" · ")
                                append(modeLabel)
                            },
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_theme),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            Surface(
                onClick = onOpenCustomSettings,
                shape = RoundedCornerShape(8.dp),
                border = if (themeSettings.showCardBorders) {
                    BorderStroke(1.dp, themeContainerBorderColor())
                } else {
                    null
                },
                color = themeContainerColor(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_custom))
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_customize),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}
