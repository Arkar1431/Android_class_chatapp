package com.mynigga.chatapp.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val bio: String = "",
    val friends: List<String> = emptyList(),
    val friendRequests: List<String> = emptyList(), // Incoming requests
    val sentRequests: List<String> = emptyList(),   // Outgoing requests
    val onboardingComplete: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)

data class UsernameRegistry(
    val uid: String = ""
)

data class Chat(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val deletedBy: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastTimestamp: Timestamp = Timestamp.now(),
    val lastSenderId: String = "",
    val lastRead: Map<String, Timestamp> = emptyMap()
)

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val content: String = "",
    val type: String = "text",
    val timestamp: Timestamp = Timestamp.now(),
    val readBy: List<String> = emptyList()
)

data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorProfileUrl: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val likeCount: Int = 0
)

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorProfileUrl: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp = Timestamp.now()
)
