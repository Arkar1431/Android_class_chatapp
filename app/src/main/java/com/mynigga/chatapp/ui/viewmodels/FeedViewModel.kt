package com.mynigga.chatapp.ui.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mynigga.chatapp.models.Comment
import com.mynigga.chatapp.models.Post
import com.mynigga.chatapp.models.User

/**
 * Manages the Social Feed, including global posts and community interactions.
 */
class FeedViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    // --- UI States ---

    private val _posts = mutableStateOf<List<Post>>(emptyList())
    val posts: State<List<Post>> = _posts

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    init {
        fetchFeed()
    }

    // --- Feed Actions ---

    /**
     * Listens for real-time updates to the global posts collection.
     */
    fun fetchFeed() {
        _isLoading.value = true
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                _isLoading.value = false
                if (e != null || snapshot == null) return@addSnapshotListener
                _posts.value = snapshot.toObjects(Post::class.java)
            }
    }

    /**
     * Creates a new status post in the feed.
     */
    fun createPost(user: User, content: String, imageUrl: String? = null) {
        if (content.isBlank() && imageUrl == null) return

        val postRef = firestore.collection("posts").document()
        val newPost = Post(
            postId = postRef.id,
            authorId = user.uid,
            authorName = user.name,
            authorProfileUrl = user.profileImageUrl,
            content = content,
            imageUrl = imageUrl,
            timestamp = Timestamp.now()
        )
        postRef.set(newPost)
    }

    /**
     * Adds a comment to a specific post.
     */
    fun addComment(post: Post, user: User, content: String, imageUrl: String? = null) {
        if (content.isBlank() && imageUrl == null) return

        val commentRef = firestore.collection("posts")
            .document(post.postId)
            .collection("comments")
            .document()

        val newComment = Comment(
            commentId = commentRef.id,
            postId = post.postId,
            authorId = user.uid,
            authorName = user.name,
            authorProfileUrl = user.profileImageUrl,
            content = content,
            imageUrl = imageUrl,
            timestamp = Timestamp.now()
        )
        commentRef.set(newComment)
    }
}
