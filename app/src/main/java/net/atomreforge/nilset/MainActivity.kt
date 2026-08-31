package net.atomreforge.nilset

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import net.atomreforge.nilset.ui.console.ConsoleScreen
import net.atomreforge.nilset.ui.login.LoginScreen
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
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        enterTransition = { fadeIn(tween(260)) },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(260)) },
                        popExitTransition = { fadeOut(tween(200)) },
                    ) {
                        composable("login") {
                            LoginScreen(onNavigateToConsole = { navController.navigate("console") })
                        }
                        composable("console") {
                            ConsoleScreen(onNavigateBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
