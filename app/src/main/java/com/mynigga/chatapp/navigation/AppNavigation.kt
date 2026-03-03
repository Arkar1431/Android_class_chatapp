package com.mynigga.chatapp.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mynigga.chatapp.models.Chat
import com.mynigga.chatapp.models.User
import com.mynigga.chatapp.ui.screens.*
import com.mynigga.chatapp.ui.viewmodels.AuthState
import com.mynigga.chatapp.ui.viewmodels.AuthViewModel
import com.mynigga.chatapp.ui.viewmodels.ChatViewModel
import com.mynigga.chatapp.ui.viewmodels.UserSearchViewModel

/**
 * Navigation destinations for the application.
 * Priority determines the swipe direction (Higher = Deeper).
 */
enum class Screen(val priority: Int) {
    Login(0), SignUp(1), SetPassword(1),
    Chats(2), FindFriends(2), Profile(2),
    ChatDetail(3), OtherUserProfile(4), Friends(4), FriendRequests(5), EditProfile(4),
    ChangePassword(5), DeleteAccount(5), AvatarSelection(5)
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    searchViewModel: UserSearchViewModel = viewModel()
) {
    val authState by authViewModel.authState
    val currentUserData by authViewModel.userData
    
    // Navigation State
    var currentScreen by remember { mutableStateOf(Screen.Login) }
    var previousScreen by remember { mutableStateOf(Screen.Login) }
    
    // Shared state for deep navigation data passing
    var activeChat by remember { mutableStateOf<Chat?>(null) }
    var activeOtherUser by remember { mutableStateOf<User?>(null) }
    var selectedProfileId by remember { mutableStateOf("") }

    /**
     * Unified navigation function that tracks history for back transitions.
     */
    val updateScreen: (Screen) -> Unit = { nextScreen ->
        previousScreen = currentScreen
        currentScreen = nextScreen
    }

    // --- Authentication & Onboarding Guard ---
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // If the user is authenticated but still on an auth screen, move them into the app
                if (currentScreen.priority < 2) updateScreen(Screen.Chats)
            }
            is AuthState.NeedsPasswordSet -> {
                updateScreen(Screen.SetPassword)
            }
            is AuthState.Unauthenticated -> {
                // If logout occurs, return to login if not already there
                if (currentScreen != Screen.Login) updateScreen(Screen.Login)
            }
            else -> {}
        }
    }

    val showBottomBar = currentScreen in listOf(Screen.Chats, Screen.FindFriends, Screen.Profile)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigationBar(
                    currentScreen = currentScreen,
                    onNavigate = { updateScreen(it) }
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val isBottomUp = targetState.priority > initialState.priority && targetState.priority >= 3
                val isSlideDown = targetState.priority < initialState.priority && initialState.priority >= 3
                val isTabSwitch = initialState.priority == 2 && targetState.priority == 2

                when {
                    isBottomUp -> slideInVertically(animationSpec = tween(400)) { it } + fadeIn() togetherWith fadeOut()
                    isSlideDown -> fadeIn() togetherWith slideOutVertically(animationSpec = tween(400)) { it } + fadeOut()
                    isTabSwitch -> fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                    targetState.priority > initialState.priority -> slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    else -> slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "GlobalNav"
        ) { target ->
            Box(modifier = Modifier.padding(if (showBottomBar) innerPadding else PaddingValues(0.dp))) {
                when (target) {
                    // --- Auth Flow ---
                    Screen.Login -> LoginScreen(
                        viewModel = authViewModel,
                        onNavigateToSignUp = { updateScreen(Screen.SignUp) },
                        onLoginSuccess = { updateScreen(Screen.Chats) }
                    )
                    Screen.SignUp -> SignUpScreen(viewModel = authViewModel, onBack = { updateScreen(Screen.Login) })
                    Screen.SetPassword -> SetPasswordScreen(viewModel = authViewModel, onComplete = { updateScreen(Screen.Chats) })
                    
                    // --- Main Tab Flow ---
                    Screen.Chats -> ChatListScreen(
                        currentUserId = authViewModel.currentUser?.uid ?: "",
                        chatViewModel = chatViewModel,
                        onChatClick = { chat, user ->
                            activeChat = chat
                            activeOtherUser = user
                            updateScreen(Screen.ChatDetail)
                        }
                    )
                    Screen.FindFriends -> FindFriendsScreen(
                        authViewModel = authViewModel,
                        searchViewModel = searchViewModel,
                        onUserClick = { id -> 
                            selectedProfileId = id
                            updateScreen(Screen.OtherUserProfile) 
                        },
                        onNavigateToRequests = { updateScreen(Screen.FriendRequests) }
                    )
                    Screen.Profile -> ProfileScreen(
                        viewModel = authViewModel,
                        onFriendsClick = { updateScreen(Screen.Friends) },
                        onEditProfileClick = { updateScreen(Screen.EditProfile) }
                    )

                    // --- Detail & Setting Screens (Pop-ups) ---
                    Screen.ChatDetail -> activeChat?.let { chat ->
                        activeOtherUser?.let { user ->
                            ConversationScreen(
                                chatId = chat.chatId,
                                currentUserId = authViewModel.currentUser?.uid ?: "",
                                otherUser = user,
                                chatViewModel = chatViewModel,
                                onBack = { updateScreen(Screen.Chats) }
                            )
                        }
                    }
                    Screen.OtherUserProfile -> OtherUserProfileScreen(
                        userId = selectedProfileId,
                        authViewModel = authViewModel,
                        isAlreadyFriend = currentUserData?.friends?.contains(selectedProfileId) == true,
                        onBack = { updateScreen(Screen.FindFriends) },
                        onAddFriendClick = { id -> 
                            searchViewModel.sendFriendRequest(authViewModel.currentUser?.uid ?: "", id) 
                        },
                        onMessageClick = { user ->
                            chatViewModel.startChat(authViewModel.currentUser?.uid ?: "", user) { chat, other ->
                                activeChat = chat
                                activeOtherUser = other
                                updateScreen(Screen.ChatDetail)
                            }
                        }
                    )
                    Screen.Friends -> FriendsScreen(
                        authViewModel = authViewModel,
                        onBack = { updateScreen(Screen.Profile) },
                        onNavigateToRequests = { updateScreen(Screen.FriendRequests) },
                        onFriendClick = { friend ->
                            chatViewModel.startChat(authViewModel.currentUser?.uid ?: "", friend) { chat, other ->
                                activeChat = chat
                                activeOtherUser = other
                                updateScreen(Screen.ChatDetail)
                            }
                        }
                    )
                    Screen.FriendRequests -> FriendRequestsScreen(
                        currentUserId = authViewModel.currentUser?.uid ?: "",
                        requestIds = currentUserData?.friendRequests ?: emptyList(),
                        onBack = { 
                            // Determine return path based on navigation entry point
                            if (previousScreen == Screen.Friends) updateScreen(Screen.Friends) 
                            else updateScreen(Screen.FindFriends) 
                        },
                        searchViewModel = searchViewModel
                    )
                    Screen.EditProfile -> EditProfileScreen(
                        viewModel = authViewModel,
                        onBack = { updateScreen(Screen.Profile) },
                        onNavigateToDeleteAccount = { updateScreen(Screen.DeleteAccount) },
                        onNavigateToChangePassword = { updateScreen(Screen.ChangePassword) },
                        onNavigateToAvatarSelection = { updateScreen(Screen.AvatarSelection) }
                    )
                    Screen.ChangePassword -> ChangePasswordScreen(viewModel = authViewModel, onBack = { updateScreen(Screen.EditProfile) })
                    Screen.DeleteAccount -> DeleteAccountScreen(authViewModel = authViewModel, onBack = { updateScreen(Screen.EditProfile) })
                    Screen.AvatarSelection -> AvatarSelectionScreen(
                        viewModel = authViewModel,
                        onBack = { updateScreen(Screen.EditProfile) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppBottomNavigationBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
        val tabs = listOf(
            Triple(Screen.Chats, Icons.Rounded.Forum, "Chats"),
            Triple(Screen.FindFriends, Icons.Rounded.Search, "Find"),
            Triple(Screen.Profile, Icons.Rounded.Face, "Profile")
        )

        tabs.forEach { (screen, icon, label) ->
            NavigationBarItem(
                icon = { Icon(icon, null) },
                label = { Text(label) },
                selected = currentScreen == screen,
                onClick = { onNavigate(screen) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray
                )
            )
        }
    }
}
