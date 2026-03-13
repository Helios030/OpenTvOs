package con.open.tvos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import con.open.tvos.ui.screens.detail.DetailScreen
import con.open.tvos.ui.screens.home.HomeScreen
import con.open.tvos.ui.screens.live.LiveTvScreen
import con.open.tvos.ui.screens.player.PlayerScreen
import con.open.tvos.ui.screens.search.SearchScreen
import con.open.tvos.ui.screens.settings.SettingsScreen

/**
 * Navigation Routes
 */
object NavRoutes {
    const val HOME = "home"
    const val DETAIL = "detail/{vodId}"
    const val PLAYER = "player/{vodId}/{episodeIndex}"
    const val LIVE_TV = "live"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    // Helper functions for creating routes with arguments
    fun detail(vodId: String) = "detail/$vodId"
    fun player(vodId: String, episodeIndex: Int) = "player/$vodId/$episodeIndex"
}

/**
 * Legacy Routes object (kept for backward compatibility)
 */
object Routes {
    const val HOME = "home"
    const val DETAIL = "detail/{vodId}"
    const val PLAYER = "player/{vodId}/{episodeIndex}"
    const val LIVE_TV = "live"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    // Helper functions for creating routes with arguments
    fun detail(vodId: String) = "detail/$vodId"
    fun player(vodId: String, episodeIndex: Int) = "player/$vodId/$episodeIndex"
}

/**
 * Navigation Arguments
 */
object NavArgs {
    const val VOD_ID = "vodId"
    const val EPISODE_INDEX = "episodeIndex"
}

/**
 * Main Navigation Graph
 */
@Composable
fun TvNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.HOME
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Home Screen
        composable(Routes.HOME) {
            HomeScreen(
                onMovieClick = { vodId ->
                    navController.navigate(Routes.detail(vodId))
                },
                onSearchClick = {
                    navController.navigate(Routes.SEARCH)
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
                onLiveTvClick = {
                    navController.navigate(Routes.LIVE_TV)
                }
            )
        }

        // Detail Screen
        composable(Routes.DETAIL) { backStackEntry ->
            val vodId = backStackEntry.arguments?.getString(NavArgs.VOD_ID) ?: ""
            DetailScreen(
                vodId = vodId,
                onPlayClick = { episodeIndex ->
                    navController.navigate(Routes.player(vodId, episodeIndex))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Player Screen
        composable(Routes.PLAYER) { backStackEntry ->
            val vodId = backStackEntry.arguments?.getString(NavArgs.VOD_ID) ?: ""
            val episodeIndex = backStackEntry.arguments?.getString(NavArgs.EPISODE_INDEX)?.toIntOrNull() ?: 0
            PlayerScreen(
                vodId = vodId,
                episodeIndex = episodeIndex,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Live TV Screen
        composable(Routes.LIVE_TV) {
            LiveTvScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Search Screen
        composable(Routes.SEARCH) {
            SearchScreen(
                onMovieClick = { vodId ->
                    navController.navigate(Routes.detail(vodId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        }
}

// Extension functions for NavGraphBuilder to support MainActivity pattern

fun NavGraphBuilder.homeScreen(navController: NavHostController) {
    composable(NavRoutes.HOME) {
        HomeScreen(
            onMovieClick = { vodId ->
                navController.navigate(NavRoutes.detail(vodId))
            },
            onSearchClick = {
                navController.navigate(NavRoutes.SEARCH)
            },
            onSettingsClick = {
                navController.navigate(NavRoutes.SETTINGS)
            },
            onLiveTvClick = {
                navController.navigate(NavRoutes.LIVE_TV)
            }
        )
    }
}

fun NavGraphBuilder.detailScreen(navController: NavHostController) {
    composable(NavRoutes.DETAIL) { backStackEntry ->
        val vodId = backStackEntry.arguments?.getString(NavArgs.VOD_ID) ?: ""
        DetailScreen(
            vodId = vodId,
            onPlayClick = { episodeIndex ->
                navController.navigate(NavRoutes.player(vodId, episodeIndex))
            },
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}

fun NavGraphBuilder.playerScreen(navController: NavHostController) {
    composable(NavRoutes.PLAYER) { backStackEntry ->
        val vodId = backStackEntry.arguments?.getString(NavArgs.VOD_ID) ?: ""
        val episodeIndex = backStackEntry.arguments?.getString(NavArgs.EPISODE_INDEX)?.toIntOrNull() ?: 0
        PlayerScreen(
            vodId = vodId,
            episodeIndex = episodeIndex,
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}

fun NavGraphBuilder.liveTvScreen(navController: NavHostController) {
    composable(NavRoutes.LIVE_TV) {
        LiveTvScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}

fun NavGraphBuilder.searchScreen(navController: NavHostController) {
    composable(NavRoutes.SEARCH) {
        SearchScreen(
            onMovieClick = { vodId ->
                navController.navigate(NavRoutes.detail(vodId))
            },
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}

fun NavGraphBuilder.settingsScreen(navController: NavHostController) {
    composable(NavRoutes.SETTINGS) {
        SettingsScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}
