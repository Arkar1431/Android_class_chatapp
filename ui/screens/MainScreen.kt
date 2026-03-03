package com.mynigga.chatapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mynigga.chatapp.auth.AuthViewModel
import com.mynigga.chatapp.models.Chat
import com.mynigga.chatapp.models.User

sealed class BottomNavItem(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String) {
    object Chats : BottomNavItem("chats", Icons.Default.Chat, "Chats")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
    object Settings : BottomNavItem("settings", Icons.Default.Settings, "Settings")
}

@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    onChatClick: (Chat, User) -> Unit
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(BottomNavItem.Chats, BottomNavItem.Profile, BottomNavItem.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedItem) {
                0 -> ChatListScreen(
                    currentUserId = authViewModel.currentUser?.uid ?: "",
                    onChatClick = onChatClick
                )
                1 -> ProfileScreen(authViewModel)
                2 -> SettingsPlaceholder()
            }
        }
    }
}
