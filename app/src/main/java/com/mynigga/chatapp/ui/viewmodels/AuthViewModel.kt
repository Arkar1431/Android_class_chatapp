package com.mynigga.chatapp.ui.viewmodels

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.mynigga.chatapp.models.User

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    object NeedsPasswordSet : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _authState = mutableStateOf<AuthState>(
        if (auth.currentUser != null) AuthState.Authenticated else AuthState.Unauthenticated
    )
    val authState: State<AuthState> = _authState

    private val _userData = mutableStateOf<User?>(null)
    val userData: State<User?> = _userData

    val currentUser: FirebaseUser? get() = auth.currentUser

    init {
        auth.currentUser?.let { 
            fetchUserData(it.uid)
        }
    }

    fun signIn(email: String, password: String) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
            _authState.value = AuthState.Error("Email and password are required.")
            return
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onAuthSuccess(task.result?.user)
                else handleException(task.exception)
            }
    }

    fun signUp(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.length < 6) {
            _authState.value = AuthState.Error("Invalid input. Password must be 6+ chars.")
            return
        }

        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(trimmedEmail, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onAuthSuccess(task.result?.user)
                else handleException(task.exception)
            }
    }

    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) onAuthSuccess(task.result?.user)
            else handleException(task.exception)
        }
    }

    fun signOut() {
        auth.signOut()
        _userData.value = null
        _authState.value = AuthState.Unauthenticated
    }

    fun finalizeAccountSetup(name: String, password: String) {
        val user = auth.currentUser ?: return
        val cleanName = name.trim().lowercase()
        
        if (cleanName.isEmpty() || password.length < 6) {
            _authState.value = AuthState.Error("Please enter a valid username and password.")
            return
        }

        _authState.value = AuthState.Loading
        
        firestore.collection("usernames").document(cleanName).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    _authState.value = AuthState.Error("Username '$name' is already taken.")
                } else {
                    performFinalize(name, password, cleanName, user)
                }
            }
            .addOnFailureListener { _authState.value = AuthState.Error("Connection error. Try again.") }
    }

    private fun performFinalize(name: String, password: String, cleanName: String, user: FirebaseUser) {
        user.updatePassword(password).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                user.updateProfile(profileUpdates).addOnCompleteListener {
                    val batch = firestore.batch()
                    val userRef = firestore.collection("users").document(user.uid)
                    val usernameRef = firestore.collection("usernames").document(cleanName)
                    
                    batch.update(userRef, "name", name)
                    batch.update(userRef, "onboardingComplete", true)
                    batch.set(usernameRef, mapOf("uid" to user.uid))
                    
                    batch.commit().addOnCompleteListener { fsTask ->
                        if (fsTask.isSuccessful) {
                            _authState.value = AuthState.Authenticated
                        } else {
                            _authState.value = AuthState.Error("Failed to save data.")
                        }
                    }
                }
            } else {
                handleException(authTask.exception)
            }
        }
    }

    fun changeAccountPassword(oldPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        _authState.value = AuthState.Loading

        val credential = EmailAuthProvider.getCredential(email, oldPassword)
        user.reauthenticate(credential).addOnCompleteListener { reauth ->
            if (reauth.isSuccessful) {
                user.updatePassword(newPassword).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _authState.value = AuthState.Authenticated
                    } else {
                        handleException(task.exception)
                    }
                }
            } else {
                val exception = reauth.exception
                val message = if (exception is FirebaseAuthInvalidCredentialsException) {
                    "Old password is wrong"
                } else {
                    "Verification failed. Please check your connection."
                }
                _authState.value = AuthState.Error(message)
            }
        }
    }

    fun updateProfile(name: String, imageUrl: String?) {
        val user = auth.currentUser ?: return
        _authState.value = AuthState.Loading

        val updates = mutableMapOf<String, Any>("name" to name)
        if (imageUrl != null) updates["profileImageUrl"] = imageUrl

        firestore.collection("users").document(user.uid).update(updates)
            .addOnCompleteListener { fsTask ->
                if (fsTask.isSuccessful) {
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error("Profile update failed.")
                }
            }
    }

    fun deleteAccount(password: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        _authState.value = AuthState.Loading

        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential).addOnCompleteListener { reauth ->
            if (reauth.isSuccessful) {
                user.delete().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _authState.value = AuthState.Unauthenticated
                    } else {
                        handleException(task.exception)
                    }
                }
            } else {
                val exception = reauth.exception
                val message = if (exception is FirebaseAuthInvalidCredentialsException) {
                    "Old password is wrong"
                } else {
                    "Verification failed. Please check your connection."
                }
                _authState.value = AuthState.Error(message)
            }
        }
    }

    private fun onAuthSuccess(firebaseUser: FirebaseUser?) {
        if (firebaseUser == null) return
        val userRef = firestore.collection("users").document(firebaseUser.uid)
        
        userRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val newUser = User(
                    uid = firebaseUser.uid,
                    name = "PENDING_SETUP",
                    email = firebaseUser.email ?: "",
                    profileImageUrl = firebaseUser.photoUrl?.toString() ?: "",
                    onboardingComplete = false
                )
                userRef.set(newUser).addOnCompleteListener { 
                    fetchUserData(firebaseUser.uid)
                    _authState.value = AuthState.NeedsPasswordSet
                }
            } else {
                val existingUser = doc.toObject(User::class.java)
                fetchUserData(firebaseUser.uid)
                
                if (existingUser?.onboardingComplete == true || (existingUser?.name != "PENDING_SETUP" && existingUser?.name != "")) {
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.NeedsPasswordSet
                }
            }
        }.addOnFailureListener {
            _authState.value = AuthState.Error("Connection error. Please check your internet.")
        }
    }

    private fun fetchUserData(uid: String) {
        firestore.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            _userData.value = snapshot?.toObject(User::class.java)
        }
    }

    fun resetState() {
        _authState.value = if (auth.currentUser != null) {
            if (_userData.value?.onboardingComplete == false) AuthState.NeedsPasswordSet else AuthState.Authenticated
        } else AuthState.Idle
    }

    private fun handleException(exception: Exception?) {
        Log.e("Auth", "Exception: ", exception)
        val msg = when (exception) {
            is FirebaseAuthInvalidUserException -> "Account not found."
            is FirebaseAuthInvalidCredentialsException -> "Incorrect credentials."
            is FirebaseNetworkException -> "Network error. Please check your internet connection."
            else -> "Authentication failed. Try again."
        }
        _authState.value = AuthState.Error(msg)
    }
}
