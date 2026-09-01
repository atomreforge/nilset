package net.atomreforge.nilset

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import net.atomreforge.nilset.const.AppRoutes
import net.atomreforge.nilset.ui.console.ConsoleScreen
import net.atomreforge.nilset.ui.login.LoginScreen
import net.atomreforge.nilset.ui.main.MainScreen
import net.atomreforge.nilset.ui.session.SessionViewModel
import net.atomreforge.nilset.ui.theme.ATOMTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ATOMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val sessionViewModel: SessionViewModel = hiltViewModel()
                    val isSessionReady by sessionViewModel.isSessionReady.collectAsStateWithLifecycle()
                    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()

                    if (!isSessionReady) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                        return@Surface
                    }

                    val snackbarHostState = SnackbarHostState()
                    val snackbarScope = rememberCoroutineScope()
                    val navController = rememberNavController()
                    val startDestination = remember {
                        if (sessionState.isLoggedIn) AppRoutes.MAIN else AppRoutes.LOGIN
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                        ) {
                            composable(AppRoutes.LOGIN) {
                                LoginScreen(
                                    snackbarHostState = snackbarHostState,
                                    snackbarScope = snackbarScope,
                                    onNavigateToConsole = { navController.navigate(AppRoutes.CONSOLE) },
                                    onNavigateToHome = {
                                        navController.navigate(AppRoutes.MAIN) {
                                            launchSingleTop = true
                                            popUpTo(AppRoutes.LOGIN) {
                                                inclusive = true
                                            }
                                        }
                                    },
                                )
                            }
                            composable(AppRoutes.CONSOLE) {
                                ConsoleScreen(onNavigateBack = { navController.popBackStack() })
                            }
                            composable(AppRoutes.MAIN) {
                                MainScreen(
                                    onOpenConsole = {
                                        navController.navigate(AppRoutes.CONSOLE) {
                                            launchSingleTop = true
                                            popUpTo(AppRoutes.MAIN) {
                                                saveState = true
                                            }
                                            restoreState = true
                                        }
                                    },
                                )
                            }
                        }
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 72.dp, start = 16.dp, end = 16.dp),
                        ) { data ->
                            val isSuccess = data.visuals.message.startsWith("登录成功")
                            Surface(
                                modifier = Modifier.widthIn(max = 280.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.inverseSurface,
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (isSuccess) {
                                        Text(
                                            text = "✓",
                                            color = Color(0xFF4CAF50),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = data.visuals.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
