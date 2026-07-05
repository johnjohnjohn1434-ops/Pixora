package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.remote.GeminiClient
import com.example.data.repository.PixoraRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AiTaskState {
    object Idle : AiTaskState
    object Loading : AiTaskState
    data class Success(val result: String) : AiTaskState
    data class Error(val error: String) : AiTaskState
}

class PixoraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PixoraRepository(application)

    // Navigation States
    private val _currentScreen = MutableStateFlow("auth")
    val currentScreen: StateFlow<String> = _currentScreen

    private val backStack = java.util.Collections.synchronizedList(mutableListOf<String>())

    private val _navigationUser = MutableStateFlow<String?>(null)
    val navigationUser: StateFlow<String?> = _navigationUser

    private val _navigationPostId = MutableStateFlow<Int?>(null)
    val navigationPostId: StateFlow<Int?> = _navigationPostId

    fun navigateTo(screen: String, userParam: String? = null, postParam: Int? = null) {
        val current = _currentScreen.value
        if (current != screen) {
            backStack.add(current)
        }
        _currentScreen.value = screen
        if (userParam != null) {
            _navigationUser.value = userParam
        }
        if (postParam != null) {
            _navigationPostId.value = postParam
        }
    }

    fun navigateBack() {
        if (backStack.isNotEmpty()) {
            _currentScreen.value = backStack.removeAt(backStack.size - 1)
        } else {
            _currentScreen.value = "feed"
        }
    }

    // Current Session
    val currentUser: StateFlow<UserEntity?> = repository.currentUser

    // App Data Flows
    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val posts: StateFlow<List<PostEntity>> = repository.getAllPostsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stories: StateFlow<List<StoryEntity>> = repository.getAllStoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reels: StateFlow<List<ReelEntity>> = repository.getAllReelsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.getAllNotificationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<ReportEntity>> = repository.getAllReportsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatPartners: StateFlow<List<String>> = repository.getChatPartnersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feed Sort Preferences
    private val _feedTab = MutableStateFlow("RECOMMENDED") // RECOMMENDED, LATEST, TRENDING
    val feedTab: StateFlow<String> = _feedTab

    val sortedPosts: StateFlow<List<PostEntity>> = combine(posts, feedTab) { postsList, tab ->
        when (tab) {
            "LATEST" -> postsList.sortedByDescending { it.createdAt }
            "TRENDING" -> postsList.sortedByDescending { it.likesCount }
            else -> postsList // Recommended / Mixed
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat Session
    private val _activeChatPartner = MutableStateFlow<String?>(null)
    val activeChatPartner: StateFlow<String?> = _activeChatPartner

    val activeMessages: StateFlow<List<MessageEntity>> = _activeChatPartner
        .flatMapLatest { partnerId ->
            if (partnerId != null) {
                repository.getMessagesForPartnerFlow(partnerId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved/Saved Collections
    val savedPosts: StateFlow<List<PostEntity>> = repository.getSavedPostsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings States
    private val _isDarkMode = MutableStateFlow(true) // defaults to true for that gorgeous cosmic look
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _isPrivateAccount = MutableStateFlow(false)
    val isPrivateAccount: StateFlow<Boolean> = _isPrivateAccount

    private val _keywordFilter = MutableStateFlow("spam, offensive, crypto")
    val keywordFilter: StateFlow<String> = _keywordFilter

    // Gemini AI States
    private val _aiCaptionState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val aiCaptionState: StateFlow<AiTaskState> = _aiCaptionState

    private val _aiHashtagsState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val aiHashtagsState: StateFlow<AiTaskState> = _aiHashtagsState

    fun setFeedTab(tab: String) {
        _feedTab.value = tab
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setPrivateAccount(enabled: Boolean) {
        _isPrivateAccount.value = enabled
        viewModelScope.launch {
            val current = currentUser.value ?: return@launch
            val updated = current.copy(isPrivate = enabled)
            repository.updateProfile(updated.displayName, updated.bio, updated.website, updated.photoUrl)
        }
    }

    fun setKeywordFilter(filter: String) {
        _keywordFilter.value = filter
    }

    fun setActiveChatPartner(partnerId: String?) {
        _activeChatPartner.value = partnerId
    }

    // --- Interactive Operations ---
    fun registerUser(username: String, name: String, bio: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = repository.register(username, name, bio)
            if (success) {
                onSuccess()
            } else {
                onError("Username is already taken!")
            }
        }
    }

    fun loginUser(username: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.login(username)
            onSuccess()
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun updateProfile(displayName: String, bio: String, website: String, photoUrl: String) {
        viewModelScope.launch {
            repository.updateProfile(displayName, bio, website, photoUrl)
        }
    }

    fun likePost(postId: Int) {
        viewModelScope.launch {
            repository.toggleLikePost(postId)
        }
    }

    fun savePost(postId: Int) {
        viewModelScope.launch {
            repository.toggleSavePost(postId)
        }
    }

    fun likeReel(reelId: Int) {
        viewModelScope.launch {
            repository.toggleLikeReel(reelId)
        }
    }

    fun followUser(targetUsername: String) {
        viewModelScope.launch {
            repository.followUser(targetUsername)
        }
    }

    fun submitComment(postId: Int, text: String) {
        if (text.isBlank()) return
        val filteredText = filterCommentToxicity(text)
        viewModelScope.launch {
            repository.addComment(postId, filteredText)
        }
    }

    fun getLatestMessageForPartnerFlow(partnerId: String): Flow<MessageEntity?> {
        return repository.getLatestMessageForPartnerFlow(partnerId)
    }

    fun getCommentsForPostFlow(postId: Int): Flow<List<CommentEntity>> {
        return repository.getCommentsForPostFlow(postId)
    }

    fun deleteComment(comment: CommentEntity) {
        viewModelScope.launch {
            repository.deleteComment(comment)
        }
    }

    private fun filterCommentToxicity(commentText: String): String {
        val badWords = _keywordFilter.value.split(",").map { it.trim().lowercase() }
        var result = commentText
        for (word in badWords) {
            if (word.isNotEmpty() && result.lowercase().contains(word)) {
                result = result.replace(word, "*".repeat(word.length), ignoreCase = true)
            }
        }
        return result
    }

    fun submitPost(caption: String, mediaUrl: String, location: String?, hashtags: String, isSensitive: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            // Moderation check via Gemini AI
            val isSafe = GeminiClient.moderatePostCaption(caption)
            repository.createPost(
                caption = caption,
                mediaUrl = mediaUrl,
                location = location,
                hashtags = hashtags,
                isSensitive = !isSafe || isSensitive
            )
            onComplete()
        }
    }

    fun deletePost(post: PostEntity) {
        viewModelScope.launch {
            repository.deletePost(post)
        }
    }

    fun archivePost(postId: Int, archive: Boolean) {
        viewModelScope.launch {
            repository.archivePost(postId, archive)
        }
    }

    fun submitStory(mediaUrl: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.createStory(mediaUrl)
            onComplete()
        }
    }

    fun submitReel(caption: String, videoUrl: String, thumbnailUrl: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.createReel(caption, videoUrl, thumbnailUrl)
            onComplete()
        }
    }

    fun sendChatMessage(partnerId: String, text: String, mediaUrl: String? = null) {
        if (text.isBlank() && mediaUrl == null) return
        viewModelScope.launch {
            repository.sendMessage(partnerId, text, mediaUrl)
        }
    }

    fun submitReport(type: String, targetId: String, reason: String, description: String, evidenceUrl: String?) {
        viewModelScope.launch {
            repository.createReport(type, targetId, reason, description, evidenceUrl)
        }
    }

    // --- Gemini AI Dynamic Functions ---
    fun generateAiCaption(prompt: String) {
        _aiCaptionState.value = AiTaskState.Loading
        viewModelScope.launch {
            val caption = GeminiClient.generateCaption(prompt)
            if (caption.startsWith("Failed") || caption.startsWith("No Gemini API key")) {
                _aiCaptionState.value = AiTaskState.Error(caption)
            } else {
                _aiCaptionState.value = AiTaskState.Success(caption)
            }
        }
    }

    fun clearAiCaption() {
        _aiCaptionState.value = AiTaskState.Idle
    }

    fun suggestAiHashtags(prompt: String) {
        _aiHashtagsState.value = AiTaskState.Loading
        viewModelScope.launch {
            val hashtags = GeminiClient.suggestHashtags(prompt)
            _aiHashtagsState.value = AiTaskState.Success(hashtags)
        }
    }

    fun clearAiHashtags() {
        _aiHashtagsState.value = AiTaskState.Idle
    }

    // --- Admin Control Dashboard Operations ---
    fun updateReportStatus(reportId: Int, status: String) {
        viewModelScope.launch {
            repository.updateReportStatus(reportId, status)
        }
    }

    fun verifyUserStatus(username: String, verify: Boolean) {
        viewModelScope.launch {
            repository.setUserVerification(username, verify)
        }
    }

    fun changeUserRole(username: String, role: String) {
        viewModelScope.launch {
            repository.setUserRole(username, role)
        }
    }

    fun terminateUserAccount(username: String) {
        viewModelScope.launch {
            repository.deleteUser(username)
        }
    }

    // Blocking / Muting
    fun blockUser(username: String) {
        viewModelScope.launch {
            repository.toggleBlockUser(username)
        }
    }

    fun muteUser(username: String) {
        viewModelScope.launch {
            repository.toggleMuteUser(username)
        }
    }

    fun restrictUser(username: String) {
        viewModelScope.launch {
            repository.toggleRestrictUser(username)
        }
    }
}
