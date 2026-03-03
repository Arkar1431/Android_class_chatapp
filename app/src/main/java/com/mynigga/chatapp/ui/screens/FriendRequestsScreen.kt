package com.mynigga.chatapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.mynigga.chatapp.models.User
import com.mynigga.chatapp.ui.viewmodels.UserSearchViewModel
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(
    currentUserId: String,
    requestIds: List<String>,
    onBack: () -> Unit,
    searchViewModel: UserSearchViewModel
) {
    var requestingUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    val firestore = remember { FirebaseFirestore.getInstance() }

    DisposableEffect(requestIds) {
        if (requestIds.isEmpty()) {
            requestingUsers = emptyList()
            return@DisposableEffect onDispose {}
        }
        val listener = firestore.collection("users")
            .whereIn("uid", requestIds)
            .addSnapshotListener { snapshot, _ ->
                requestingUsers = snapshot?.toObjects(User::class.java) ?: emptyList()
            }
        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friend Requests", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (requestingUsers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("No pending requests", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(requestingUsers) { user ->
                    RequestItem(
                        user = user,
                        onAccept = { searchViewModel.acceptFriendRequest(currentUserId, user.uid) },
                        onReject = { searchViewModel.rejectFriendRequest(currentUserId, user.uid) }
                    )
                }
            }
        }
    }
}

@Composable
fun RequestItem(user: User, onAccept: () -> Unit, onReject: () -> Unit) {
    ListItem(
        headlineContent = { Text(user.name, fontWeight = FontWeight.Bold) },
        leadingContent = {
            Image(
                painter = rememberAsyncImagePainter(user.profileImageUrl.ifEmpty { android.R.drawable.ic_menu_gallery }),
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onAccept) { Icon(Icons.Default.Check, "Accept", tint = Color(0xFF2E7D32)) }
                IconButton(onClick = onReject) { Icon(Icons.Default.Close, "Reject", tint = Color.Red) }
            }
        }
    )
}
