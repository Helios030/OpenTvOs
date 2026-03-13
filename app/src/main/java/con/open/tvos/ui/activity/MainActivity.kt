package con.open.tvos.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import con.open.tvos.ui.navigation.NavRoutes
import con.open.tvos.ui.navigation.homeScreen
import con.open.tvos.ui.navigation.detailScreen
import con.open.tvos.ui.navigation.playerScreen
import con.open.tvos.ui.navigation.liveTvScreen
import con.open.tvos.ui.navigation.searchScreen
import con.open.tvos.ui.navigation.settingsScreen
import con.open.tvos.ui.theme.TVBoxTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity - Main entry point for TVBox application
 * 
 * This activity serves as the Compose host for the entire application.
 * It initializes the navigation graph and sets up the TV-optimized theme.
 * 
 * Features:
 * - Hilt dependency injection
 * - TV-optimized theme with dark mode
 * - Navigation graph with all screens
 * - D-pad navigation support
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            TVBoxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TVBoxApp()
                }
            }
        }
    }
}

/**
 * Main Composable for the TVBox application
 * 
 * Sets up the navigation graph and hosts all screens.
 */
@Composable
fun TVBoxApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        homeScreen(navController)
        detailScreen(navController)
        playerScreen(navController)
        liveTvScreen(navController)
        searchScreen(navController)
        settingsScreen(navController)
    }
}
