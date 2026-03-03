package com.mynigga.chatapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mynigga.chatapp.ui.components.ProfileActionButton
import com.mynigga.chatapp.ui.components.ProfileHeader
import com.mynigga.chatapp.ui.viewmodels.AuthViewModel

/**
 * Professional Profile Tab - Main branding and entry point for settings.
 * Refactored to use reusable profile components.
 */
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onFriendsClick: () -> Unit,
    onEditProfileClick: () -> Unit
) {
    val userData by viewModel.userData
    
    val displayName = userData?.name ?: "User"
    val profileImageUrl = userData?.profileImageUrl
    val friendCount = userData?.friends?.size ?: 0

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Reusable Header ---
        ProfileHeader(
            imageUrl = profileImageUrl,
            name = displayName,
            friendCount = friendCount
        )

        // --- Action Section ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            ProfileActionButton(
                text = "Edit Profile",
                onClick = onEditProfileClick,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            ProfileActionButton(
                text = "Friends",
                onClick = onFriendsClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
