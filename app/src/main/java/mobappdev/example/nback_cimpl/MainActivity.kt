package mobappdev.example.nback_cimpl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mobappdev.example.nback_cimpl.ui.screens.GameScreen
import mobappdev.example.nback_cimpl.ui.screens.HomeScreen
import mobappdev.example.nback_cimpl.ui.screens.ScoreHistoryScreen
import mobappdev.example.nback_cimpl.ui.screens.SettingsScreen
import mobappdev.example.nback_cimpl.ui.theme.NBack_CImplTheme
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM

/**
 * MainActivity for the Dual N-Back mobile application.
 *
 * Implements MVVM with Jetpack Compose and Navigation Compose.
 * Screen navigation is managed exclusively in the UI layer (NavHost)
 * while the ViewModel exposes state and receives intents.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NBack_CImplTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val gameViewModel: GameVM = viewModel(
                        factory = GameVM.Factory
                    )
                    NBackAppNavHost(gameViewModel = gameViewModel)
                }
            }
        }
    }
}

/**
 * Navigation graph definition for the application.
 *
 * Routes:
 * - "home": Main menu with mode selector, high score, and settings summary.
 * - "game": Active stimulus round (visual grid and/or spoken letters).
 * - "settings": Persistent settings configuration.
 * - "history": Saved score history backed by Room database.
 */
//16 our controllers
//16.2 enforces separation of converns, keep viewmodel business-logic focused
@Composable
fun NBackAppNavHost(
    gameViewModel: GameVM,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                vm = gameViewModel,
                onNavigateToGame = { navController.navigate("game") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHistory = { navController.navigate("history") }
            )
        }

        composable("game") {
            GameScreen(
                vm = gameViewModel,
                onNavigateBack = {
                    navController.popBackStack(route = "home", inclusive = false)
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                vm = gameViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("history") {
            ScoreHistoryScreen(
                vm = gameViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
