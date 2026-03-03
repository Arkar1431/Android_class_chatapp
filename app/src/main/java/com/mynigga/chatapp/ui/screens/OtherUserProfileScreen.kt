package com.mynigga.chatapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mynigga.chatapp.models.User
import com.mynigga.chatapp.ui.components.ProfileActionButton
import com.mynigga.chatapp.ui.components.ProfileHeader
import com.google.firebase.firestore.FirebaseFirestore
import com.mynigga.chatapp.ui.viewmodels.AuthViewModel

/**
 * Professional Profile View for other users.
 * Strictly manages the Friend Request lifecycle and messaging permissions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(
    userId: String,
    authViewModel: AuthViewModel,
    isAlreadyFriend: Boolean,
    onBack: () -> Unit,
    onMessageClick: (User) -> Unit,
    onAddFriendClick: (String) -> Unit
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUserData by authViewModel.userData
    
    var userProfile by remember { mutableStateOf<User?>(null) }
    
    // Local state to prevent "fallback" UI flicker during network lag
    var localRequestSent by remember { mutableStateOf(false) }

    // 1. Real-time data sync for the target user
    DisposableEffect(userId) {
        val docRef = firestore.collection("users").document(userId)
        val listener = docRef.addSnapshotListener { snapshot, _ ->
            userProfile = snapshot?.toObject(User::class.java)
        }
        onDispose { listener.remove() }
    }

    // 2. State Resolution Logic
    val areWeFriends = isAlreadyFriend || currentUserData?.friends?.contains(userId) == true
    val isRequestSent = localRequestSent || currentUserData?.sentRequests?.contains(userId) == true
    val hasReceivedRequest = currentUserData?.friendRequests?.contains(userId) == true

    Scaffold(
        topBar = { OtherUserTopBar(onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            userProfile?.let { user ->
                // Header (Reusable Component)
                ProfileHeader(
                    imageUrl = user.profileImageUrl,
                    name = user.name,
                    friendCount = user.friends.size
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons with Contextual States
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    when {
                        areWeFriends -> {
                            // CASE: FRIENDS -> Message Active
                            ProfileActionButton(
                                text = "Message",
                                onClick = { onMessageClick(user) },
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        }
                        isRequestSent -> {
                            // CASE: REQUEST SENT -> Locked
                            ProfileActionButton(
                                text = "Request Sent",
                                onClick = { },
                                enabled = false,
                                modifier = Modifier.weight(1f),
                                containerColor = Color.LightGray,
                                contentColor = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            ProfileActionButton(
                                text = "Message",
                                onClick = { },
                                enabled = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        hasReceivedRequest -> {
                            // CASE: THEY ADDED YOU -> Prompt to accept
                            ProfileActionButton(
                                text = "Respond to Request",
                                onClick = { onBack() /* Returns to notifications */ },
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        }
                        else -> {
                            // CASE: STRANGERS -> Add Friend Active
                            ProfileActionButton(
                                text = "Add Friend",
                                onClick = { 
                                    localRequestSent = true // Instant UI change
                                    onAddFriendClick(user.uid) 
                                    Toast.makeText(context, "Request sent!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            ProfileActionButton(
                                text = "Message",
                                onClick = { },
                                enabled = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Help Text for better UX
                if (!areWeFriends) {
                    Text(
                        text = if (isRequestSent) "Waiting for their approval..." else "You can only message friends.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtherUserTopBar(onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
            detectVerticalDragGestures { _, dragAmount -> if (dragAmount > 20) onBack() }
        }
    ) {
        Box(modifier = Modifier.padding(top = 12.dp).width(40.dp).height(4.dp).background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }
}
