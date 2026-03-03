package com.mynigga.chatapp.ui.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mynigga.chatapp.models.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages user discovery and friend request workflows.
 */
class UserSearchViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    // --- UI States ---

    private val _searchResults = mutableStateOf<List<User>>(emptyList())
    val searchResults: State<List<User>> = _searchResults

    private val _isSearching = mutableStateOf(false)
    val isSearching: State<Boolean> = _isSearching

    // --- Search Actions ---

    /**
     * Search users by unique username ID.
     */
    fun searchUsers(query: String, currentUserId: String) {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            // Debounce delay to prevent excessive Firestore reads
            delay(300)

            firestore.collection("usernames").document(cleanQuery).get()
                .addOnSuccessListener { nameDoc ->
                    val targetUid = nameDoc.getString("uid")
                    if (targetUid != null && targetUid != currentUserId) {
                        firestore.collection("users").document(targetUid).get()
                            .addOnSuccessListener { userDoc ->
                                val userProfile = userDoc.toObject(User::class.java)
                                _searchResults.value = if (userProfile != null) listOf(userProfile) else emptyList()
                                _isSearching.value = false
                            }
                            .addOnFailureListener { _isSearching.value = false }
                    } else {
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                    }
                }
                .addOnFailureListener { _isSearching.value = false }
        }
    }

    // --- Social Actions ---

    /**
     * Sends a friend request: Updates both sender's sent list and receiver's incoming list.
     */
    fun sendFriendRequest(senderId: String, targetUserId: String) {
        val senderRef = firestore.collection("users").document(senderId)
        val targetRef = firestore.collection("users").document(targetUserId)

        firestore.runBatch { batch ->
            // Mark as sent on sender's profile
            batch.update(senderRef, "sentRequests", FieldValue.arrayUnion(targetUserId))
            
            // Add to receiver's incoming requests
            batch.update(targetRef, "friendRequests", FieldValue.arrayUnion(senderId))
        }
    }

    /**
     * Accepts a friend request: Synchronizes friend lists and clears pending requests.
     */
    fun acceptFriendRequest(myId: String, senderId: String) {
        val myRef = firestore.collection("users").document(myId)
        val senderRef = firestore.collection("users").document(senderId)

        firestore.runBatch { batch ->
            // Link both users as friends
            batch.update(myRef, "friends", FieldValue.arrayUnion(senderId))
            batch.update(senderRef, "friends", FieldValue.arrayUnion(myId))
            
            // Remove the request from both sides
            batch.update(myRef, "friendRequests", FieldValue.arrayRemove(senderId))
            batch.update(senderRef, "sentRequests", FieldValue.arrayRemove(myId))
        }
    }

    /**
     * Rejects a friend request: Clears pending request status on both sides.
     */
    fun rejectFriendRequest(myId: String, senderId: String) {
        val myRef = firestore.collection("users").document(myId)
        val senderRef = firestore.collection("users").document(senderId)

        firestore.runBatch { batch ->
            batch.update(myRef, "friendRequests", FieldValue.arrayRemove(senderId))
            batch.update(senderRef, "sentRequests", FieldValue.arrayRemove(myId))
        }
    }
}
