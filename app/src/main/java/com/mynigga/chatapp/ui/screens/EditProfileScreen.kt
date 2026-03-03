package com.mynigga.chatapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.mynigga.chatapp.ui.viewmodels.AuthState
import com.mynigga.chatapp.ui.viewmodels.AuthViewModel
import com.mynigga.chatapp.ui.viewmodels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: AuthViewModel,
    profileViewModel: ProfileViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToAvatarSelection: () -> Unit
) {
    val userData by viewModel.userData
    val authState by viewModel.authState
    
    var selectedImageUrl by remember { mutableStateOf(userData?.profileImageUrl ?: "") }

    // Update selected image whenever user data changes
    LaunchedEffect(userData?.profileImageUrl) {
        selectedImageUrl = userData?.profileImageUrl ?: ""
    }

    // Nested scroll connection to detect swipe down
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 50) onBack()
                return Offset.Zero
            }
        }
    }

    Scaffold(
        topBar = { EditProfileTopBar(onSwipeDown = onBack) },
        modifier = Modifier.nestedScroll(nestedScrollConnection)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
//            Text(
//                text = "Profile Customization",
//                style = MaterialTheme.typography.headlineSmall,
//                fontWeight = FontWeight.Bold
//            )
            Spacer(modifier = Modifier.height(32.dp))

            // Section 1: Profile Photo
            ProfilePhotoSection(
                imageUrl = selectedImageUrl,
                onChangeClick = onNavigateToAvatarSelection
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 0.5.dp,
                color = Color.LightGray.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Section 2: Account Settings
            AccountSecuritySection(
                onNavigateToChangePassword = onNavigateToChangePassword,
                onNavigateToDeleteAccount = onNavigateToDeleteAccount,
                onLogoutClick = { viewModel.signOut() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileTopBar(onSwipeDown: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 20) onSwipeDown()
                }
            }
    ) {
        // Modal Drag Handle
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .width(40.dp)
                .height(4.dp)
                .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
        )
        TopAppBar(
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
private fun ProfilePhotoSection(
    imageUrl: String,
    onChangeClick: () -> Unit
) {
    Text(
        text = "Profile Photo",
        style = MaterialTheme.typography.labelLarge,
        color = Color.Gray
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = imageUrl.ifEmpty { android.R.drawable.ic_menu_gallery }
                ),
                contentDescription = "Current Avatar",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.LightGray, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "Current Photo", style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = onChangeClick) {
            Text(
                text = "Change",
                color = Color(0xFF1976D2),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AccountSecuritySection(
    onNavigateToChangePassword: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Text(
        text = "Account Settings",
        style = MaterialTheme.typography.labelLarge,
        color = Color.Gray
    )
    Spacer(modifier = Modifier.height(8.dp))
    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = onNavigateToChangePassword,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "Change Password",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }
        TextButton(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "Logout",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }
        TextButton(
            onClick = onNavigateToDeleteAccount,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "Delete Account",
                color = Color.Red,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }
    }
}
