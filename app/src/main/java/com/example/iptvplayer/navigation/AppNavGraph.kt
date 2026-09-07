package com.example.iptvplayer.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.iptvplayer.MainViewModel
import com.example.iptvplayer.ui.category.CategoryScreen
import com.example.iptvplayer.ui.channel.ChannelListScreen
import com.example.iptvplayer.ui.episode.EpisodeListScreen
import com.example.iptvplayer.ui.favorites.FavoritesScreen
import com.example.iptvplayer.ui.home.HomeScreen
import com.example.iptvplayer.ui.login.LoginScreen
import com.example.iptvplayer.ui.player.PlayerScreen
import com.example.iptvplayer.ui.search.SearchScreen
import com.example.iptvplayer.ui.series.SeriesListScreen
import com.example.iptvplayer.ui.settings.SettingsScreen

/** Builds a player route, optionally tagging the content with its parent series. */
private fun playerRoute(url: String, title: String, seriesId: Int? = null): String {
    val base = "player/${Uri.encode(url)}/${Uri.encode(title)}"
    return if (seriesId != null) "$base?seriesId=$seriesId" else base
}

@Composable
fun AppNavGraph(navController: NavHostController, viewModel: MainViewModel) {
    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(viewModel = viewModel, onLoggedIn = {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }

        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onOpenCategories = { type -> navController.navigate("categories/$type") },
                onOpenFavorites = { navController.navigate("favorites") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onProfileSwitched = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("favorites") {
            FavoritesScreen(
                onBack = { navController.popBackStack() },
                onPlay = { channel ->
                    navController.navigate(playerRoute(channel.streamUrl, channel.name, channel.seriesId))
                }
            )
        }

        composable(
            "categories/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "LIVE"
            CategoryScreen(
                viewModel = viewModel,
                type = type,
                onBack = { navController.popBackStack() },
                onOpenCategory = { categoryId ->
                    if (type == "SERIES") {
                        navController.navigate("seriesList/${Uri.encode(categoryId)}")
                    } else {
                        navController.navigate("channels/$type/${Uri.encode(categoryId)}")
                    }
                },
                onSearch = { navController.navigate("search/$type") }
            )
        }

        composable(
            "search/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "LIVE"
            SearchScreen(
                viewModel = viewModel,
                type = type,
                onBack = { navController.popBackStack() },
                onPlayChannel = { url, title ->
                    navController.navigate(playerRoute(url, title))
                },
                onOpenSeries = { seriesId ->
                    navController.navigate("episodes/$seriesId")
                }
            )
        }

        composable(
            "channels/{type}/{categoryId}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "LIVE"
            val categoryId = Uri.decode(backStackEntry.arguments?.getString("categoryId"))
            ChannelListScreen(
                viewModel = viewModel,
                type = type,
                categoryId = categoryId,
                onBack = { navController.popBackStack() },
                onPlay = { url, title ->
                    navController.navigate(playerRoute(url, title))
                }
            )
        }

        composable(
            "seriesList/{categoryId}",
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = Uri.decode(backStackEntry.arguments?.getString("categoryId"))
            SeriesListScreen(
                viewModel = viewModel,
                categoryId = categoryId,
                onBack = { navController.popBackStack() },
                onOpenSeries = { seriesId ->
                    navController.navigate("episodes/$seriesId")
                },
                onResumeEpisode = { url, title, seriesId ->
                    navController.navigate(playerRoute(url, title, seriesId))
                }
            )
        }

        composable(
            "episodes/{seriesId}",
            arguments = listOf(navArgument("seriesId") { type = NavType.IntType })
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getInt("seriesId") ?: 0
            EpisodeListScreen(
                viewModel = viewModel,
                seriesId = seriesId,
                onBack = { navController.popBackStack() },
                onPlay = { url, title ->
                    navController.navigate(playerRoute(url, title, seriesId))
                }
            )
        }

        composable(
            "player/{url}/{title}?seriesId={seriesId}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("seriesId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val url = Uri.decode(backStackEntry.arguments?.getString("url"))
            val title = Uri.decode(backStackEntry.arguments?.getString("title"))
            val seriesIdArg = backStackEntry.arguments?.getInt("seriesId") ?: -1
            PlayerScreen(
                viewModel = viewModel,
                streamUrl = url,
                title = title,
                seriesId = seriesIdArg.takeIf { it >= 0 },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
