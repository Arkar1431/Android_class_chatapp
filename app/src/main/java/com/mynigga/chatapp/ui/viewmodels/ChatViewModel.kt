package com.mynigga.chatapp.ui.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mynigga.chatapp.models.Chat
import com.mynigga.chatapp.models.Message
import com.mynigga.chatapp.models.User

/**
 * Manages 1-on-1 chats and real-time messaging.
 */
class ChatViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    // --- UI States ---
    private val _chats = mutableStateOf<List<Pair<Chat, User>>>(emptyList())
    val chats: State<List<Pair<Chat, User>>> = _chats

    private val _messages = mutableStateOf<List<Message>>(emptyList())
    val messages: State<List<Message>> = _messages

    private val _currentChat = mutableStateOf<Chat?>(null)
    val currentChat: State<Chat?> = _currentChat

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    // --- Chat List Actions ---

    /**
     * Fetches chats, but filters out those "deleted" by the current user.
     */
    fun fetchChats(currentUserId: String) {
        _isLoading.value = true
        firestore.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val documents = snapshot?.documents ?: emptyList()
                if (documents.isEmpty()) {
                    _chats.value = emptyList()
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val chatList = mutableListOf<Pair<Chat, User>>()
                var processedCount = 0
                
                documents.forEach { doc ->
                    val chat = doc.toObject(Chat::class.java)?.copy(chatId = doc.id)
                    if (chat != null && !chat.deletedBy.contains(currentUserId)) {
                        val otherId = chat.participants.find { it != currentUserId } ?: currentUserId
                        firestore.collection("users").document(otherId).get()
                            .addOnSuccessListener { userDoc ->
                                val otherUser = userDoc.toObject(User::class.java) ?: User(uid = otherId, name = "Unknown")
                                chatList.add(chat to otherUser)
                                processedCount++
                                if (processedCount == documents.size) finalizeChatList(chatList)
                            }
                            .addOnFailureListener {
                                processedCount++
                                if (processedCount == documents.size) finalizeChatList(chatList)
                            }
                    } else {
                        processedCount++
                        if (processedCount == documents.size) finalizeChatList(chatList)
                    }
                }
            }
    }

    /**
     * Starts or resumes a chat with another user.
     */
    fun startChat(currentUserId: String, otherUser: User, onChatCreated: (Chat, User) -> Unit) {
        firestore.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                val existingChatDoc = snapshot.documents.find { doc ->
                    val participants = doc.get("participants") as? List<String>
                    participants?.contains(otherUser.uid) == true
                }

                if (existingChatDoc != null) {
                    val chat = existingChatDoc.toObject(Chat::class.java)?.copy(chatId = existingChatDoc.id)
                    if (chat != null) {
                        if (chat.deletedBy.contains(currentUserId)) {
                            firestore.collection("chats").document(chat.chatId)
                                .update("deletedBy", FieldValue.arrayRemove(currentUserId))
                        }
                        onChatCreated(chat, otherUser)
                    }
                } else {
                    val newChat = Chat(
                        participants = listOf(currentUserId, otherUser.uid),
                        lastMessage = "",
                        lastTimestamp = Timestamp.now()
                    )
                    firestore.collection("chats").add(newChat).addOnSuccessListener { ref ->
                        onChatCreated(newChat.copy(chatId = ref.id), otherUser)
                    }
                }
            }
    }

    /**
     * Marks a chat as deleted for the current user.
     */
    fun deleteChatForUser(chatId: String, currentUserId: String, onComplete: () -> Unit) {
        val chatRef = firestore.collection("chats").document(chatId)
        chatRef.get().addOnSuccessListener { doc ->
            val chat = doc.toObject(Chat::class.java) ?: return@addOnSuccessListener
            chatRef.update("deletedBy", FieldValue.arrayUnion(currentUserId)).addOnSuccessListener {
                val updatedDeletedBy = chat.deletedBy.toMutableList().apply { add(currentUserId) }
                if (updatedDeletedBy.containsAll(chat.participants)) {
                    chatRef.delete()
                }
                onComplete()
            }
        }
    }

    // --- Messaging Actions ---

    /**
     * Starts listening to messages in a specific chat.
     */
    fun fetchMessages(chatId: String) {
        // Listen to Chat metadata for 'seen' status
        firestore.collection("chats").document(chatId)
            .addSnapshotListener { snapshot, _ ->
                _currentChat.value = snapshot?.toObject(Chat::class.java)?.copy(chatId = snapshot.id)
            }

        // Listen to messages
        firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val msgList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(messageId = doc.id)
                }
                _messages.value = msgList ?: emptyList()
            }
    }

    /**
     * Updates the 'last read' timestamp for the user.
     */
    fun markAsRead(chatId: String, currentUserId: String) {
        val now = Timestamp.now()
        firestore.collection("chats").document(chatId)
            .update("lastRead.$currentUserId", now)
    }

    /**
     * Sends a message and updates chat metadata.
     */
    fun sendMessage(chatId: String, senderId: String, content: String) {
        if (content.isBlank()) return
        val message = Message(senderId = senderId, content = content, timestamp = Timestamp.now())
        val chatRef = firestore.collection("chats").document(chatId)

        firestore.runBatch { batch ->
            val msgRef = chatRef.collection("messages").document()
            batch.set(msgRef, message)
            
            batch.update(chatRef, mapOf(
                "lastMessage" to content,
                "lastTimestamp" to message.timestamp,
                "lastSenderId" to senderId,
                "deletedBy" to emptyList<String>(),
                "lastRead.$senderId" to message.timestamp
            ))
        }
    }

    // --- Private Helpers ---

    private fun finalizeChatList(chatList: List<Pair<Chat, User>>) {
        _chats.value = chatList.sortedByDescending { it.first.lastTimestamp }
        _isLoading.value = false
    }
}
