package com.mynigga.chatapp.ui.viewmodels

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mynigga.chatapp.BuildConfig
import com.mynigga.chatapp.utils.supabase
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel : ViewModel() {
    private val _avatarUrls = mutableStateOf<List<String>>(emptyList())
    val avatarUrls: State<List<String>> = _avatarUrls

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    init {
        loadInitialAvatars()
    }

    /**
     * Loads avatars from static resources (BuildConfig) and remote storage (Supabase).
     */
    private fun loadInitialAvatars() {
        // 1. Load static avatars from BuildConfig
        val staticAvatars = listOfNotNull(
            BuildConfig.AVATAR_1, BuildConfig.AVATAR_2, BuildConfig.AVATAR_3,
            BuildConfig.AVATAR_4, BuildConfig.AVATAR_5, BuildConfig.AVATAR_6,
            BuildConfig.AVATAR_7, BuildConfig.AVATAR_8, BuildConfig.AVATAR_9,
            BuildConfig.AVATAR_10
        ).filter { it.isNotBlank() }

        _avatarUrls.value = staticAvatars

        // 2. Fetch additional avatars from Supabase if configured
        if (BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()) {
            fetchAvatarsFromSupabase("avatars")
        }
    }

    /**
     * Fetches a list of public URLs for images in the specified Supabase bucket.
     */
    fun fetchAvatarsFromSupabase(bucketName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val urls = withContext(Dispatchers.IO) {
                    val storageApi = supabase.storage
                    val bucket = storageApi.from(bucketName)
                    
                    // List files and filter for supported image types
                    bucket.list().filter { file ->
                        val ext = file.name.substringAfterLast('.', "").lowercase()
                        ext in listOf("png", "jpg", "jpeg", "webp")
                    }.map { file ->
                        bucket.publicUrl(file.name)
                    }
                }

                if (urls.isNotEmpty()) {
                    // Update list while preserving static ones and avoiding duplicates
                    _avatarUrls.value = (_avatarUrls.value + urls).distinct()
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to fetch avatars from Supabase", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
