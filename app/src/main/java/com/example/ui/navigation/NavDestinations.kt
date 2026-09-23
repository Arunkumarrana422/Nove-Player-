package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Videos : Screen("videos", "Videos", Icons.Default.VideoLibrary)
    data object Folders : Screen("folders", "Folders", Icons.Default.Folder)
    data object Playlists : Screen("playlists", "Playlists", Icons.Default.PlaylistPlay)
    data object Online : Screen("online", "Network", Icons.Default.Language)

    data object Favorites : Screen("favorites", "Favorites")
    data object History : Screen("history", "Watch History")
    data object Search : Screen("search", "Search")
    data object Settings : Screen("settings", "Settings")
    data object Player : Screen("player", "Player")
    data object Onboarding : Screen("onboarding", "Welcome")
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Videos,
    Screen.Folders,
    Screen.Playlists,
    Screen.Online
)
