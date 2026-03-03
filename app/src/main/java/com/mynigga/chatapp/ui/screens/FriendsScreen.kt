package com.mynigga.chatapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.mynigga.chatapp.models.User
import com.mynigga.chatapp.ui.viewmodels.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onFriendClick: (User) -> Unit // New callback for clicking a friend
) {
    val userData by authViewModel.userData
    var searchQuery by remember { mutableStateOf("") }
    
    val pendingRequestCount = userData?.friendRequests?.size ?: 0
    
    // State to hold the actual friend objects
    var friendsList by remember { mutableStateOf<List<User>>(emptyList()) }
    val firestore = remember { FirebaseFirestore.getInstance() }

    LaunchedEffect(userData?.friends) {
        val uids = userData?.friends ?: emptyList()
        if (uids.isNotEmpty()) {
            firestore.collection("users")
                .whereIn("uid", uids)
                .addSnapshotListener { snapshot, _ ->
                    friendsList = snapshot?.toObjects(User::class.java) ?: emptyList()
                }
        } else {
            friendsList = emptyList()
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 40 && source == NestedScrollSource.UserInput) {
                    onBack()
                }
                return super.onPostScroll(consumed, available, source)
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 20) onBack()
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                )
                TopAppBar(
                    title = { Text("My Friends", fontWeight = FontWeight.Bold) },
                    actions = {
                        BadgedBox(
                            badge = {
                                if (pendingRequestCount > 0) {
                                    Badge(containerColor = Color.Red) {
                                        Text(pendingRequestCount.toString(), color = Color.White)
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            IconButton(onClick = onNavigateToRequests) {
                                Icon(Icons.Default.Notifications, contentDescription = "View Requests")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        modifier = Modifier.nestedScroll(nestedScrollConnection)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("Search friends...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.LightGray.copy(alpha = 0.2f),
                    unfocusedContainerColor = Color.LightGray.copy(alpha = 0.2f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (friendsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No friends added yet", color = Color.Gray)
                }
            } else {
                val filteredFriends = friendsList.filter { 
                    it.name.contains(searchQuery, ignoreCase = true) 
                }

                Text(
                    text = "${filteredFriends.size} Results",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredFriends) { friend ->
                        FriendListItem(
                            friend = friend,
                            onClick = { onFriendClick(friend) } // Pass the click event
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendListItem(friend: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Make the whole row clickable
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(friend.profileImageUrl.ifEmpty { android.R.drawable.ic_menu_gallery }),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .border(1.dp, Color.LightGray, CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = friend.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = "Active recently", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
