package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PixoraRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.pixoraDao()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    init {
        // Run database population and default session initialization on an IO thread
        CoroutineScope(Dispatchers.IO).launch {
            populateInitialDataIfEmpty()
            // Default current user to "alice" initially
            val defaultUser = dao.getUser("alice")
            _currentUser.value = defaultUser
        }
    }

    // --- Authentication & Session ---
    suspend fun login(username: String, name: String = "", profilePhoto: String = ""): Boolean = withContext(Dispatchers.IO) {
        val existing = dao.getUser(username)
        val user = if (existing != null) {
            existing
        } else {
            val newUser = UserEntity(
                username = username,
                displayName = name.ifEmpty { username.replaceFirstChar { it.uppercase() } },
                photoUrl = profilePhoto.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                bio = "Welcome to my Pixora profile!",
                website = "https://pixora.com",
                role = if (username.lowercase().contains("admin")) "superadmin" else "user"
            )
            dao.insertUser(newUser)
            newUser
        }
        _currentUser.value = user
        return@withContext true
    }

    suspend fun logout() {
        _currentUser.value = null
    }

    suspend fun register(username: String, displayName: String, bio: String): Boolean = withContext(Dispatchers.IO) {
        val existing = dao.getUser(username)
        if (existing != null) return@withContext false

        val newUser = UserEntity(
            username = username,
            displayName = displayName,
            photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            bio = bio,
            website = "",
            role = if (username.lowercase().contains("admin")) "superadmin" else "user"
        )
        dao.insertUser(newUser)
        _currentUser.value = newUser
        return@withContext true
    }

    suspend fun updateProfile(displayName: String, bio: String, website: String, photoUrl: String) = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext
        val updated = current.copy(
            displayName = displayName,
            bio = bio,
            website = website,
            photoUrl = photoUrl
        )
        dao.insertUser(updated)
        _currentUser.value = updated
    }

    // --- General Flows ---
    fun getAllUsersFlow(): Flow<List<UserEntity>> = dao.getAllUsersFlow()
    fun getUserFlow(username: String): Flow<UserEntity?> = dao.getUserFlow(username)
    fun getAllPostsFlow(): Flow<List<PostEntity>> = dao.getAllPostsFlow()
    fun getPostFlow(postId: Int): Flow<PostEntity?> = dao.getPostFlow(postId)
    fun getSavedPostsFlow(): Flow<List<PostEntity>> = dao.getSavedPostsFlow()
    fun getPostsByAuthorFlow(username: String): Flow<List<PostEntity>> = dao.getPostsByAuthorFlow(username)
    fun getCommentsForPostFlow(postId: Int): Flow<List<CommentEntity>> = dao.getCommentsForPostFlow(postId)
    fun getAllStoriesFlow(): Flow<List<StoryEntity>> = dao.getAllStoriesFlow()
    fun getAllReelsFlow(): Flow<List<ReelEntity>> = dao.getAllReelsFlow()
    fun getMessagesForPartnerFlow(partnerId: String): Flow<List<MessageEntity>> = dao.getMessagesForPartnerFlow(partnerId)
    fun getChatPartnersFlow(): Flow<List<String>> = dao.getChatPartnersFlow()
    fun getLatestMessageForPartnerFlow(partnerId: String): Flow<MessageEntity?> = dao.getLatestMessageForPartnerFlow(partnerId)
    fun getAllNotificationsFlow(): Flow<List<NotificationEntity>> = dao.getAllNotificationsFlow()
    fun getAllReportsFlow(): Flow<List<ReportEntity>> = dao.getAllReportsFlow()

    // --- Interaction Logic ---
    suspend fun toggleLikePost(postId: Int) = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext
        val post = dao.getAllPostsFlow().first().find { it.id == postId } ?: return@withContext
        val isLiking = !post.isLiked
        val updatedPost = post.copy(
            isLiked = isLiking,
            likesCount = if (isLiking) post.likesCount + 1 else maxOf(0, post.likesCount - 1)
        )
        dao.insertPost(updatedPost)

        if (isLiking && post.authorId != current.username) {
            // Trigger follow/like notification
            val notif = NotificationEntity(
                type = "LIKE",
                senderId = current.username,
                senderName = current.displayName,
                senderAvatar = current.photoUrl,
                postId = postId,
                text = "liked your post: \"${post.caption.take(20)}...\""
            )
            dao.insertNotification(notif)
        }
    }

    suspend fun toggleSavePost(postId: Int) = withContext(Dispatchers.IO) {
        val post = dao.getAllPostsFlow().first().find { it.id == postId } ?: return@withContext
        val isSaving = !post.isSaved
        val updatedPost = post.copy(isSaved = isSaving)
        dao.insertPost(updatedPost)
    }

    suspend fun toggleLikeReel(reelId: Int) = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext
        val reel = dao.getAllReelsFlow().first().find { it.id == reelId } ?: return@withContext
        val isLiking = !reel.isLiked
        val updatedReel = reel.copy(
            isLiked = isLiking,
            likesCount = if (isLiking) reel.likesCount + 1 else maxOf(0, reel.likesCount - 1)
        )
        dao.insertReel(updatedReel)

        if (isLiking && reel.authorId != current.username) {
            val notif = NotificationEntity(
                type = "LIKE",
                senderId = current.username,
                senderName = current.displayName,
                senderAvatar = current.photoUrl,
                text = "liked your reel: \"${reel.caption.take(20)}...\""
            )
            dao.insertNotification(notif)
        }
    }

    suspend fun followUser(targetUsername: String) = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext
        if (current.username == targetUsername) return@withContext

        val target = dao.getUser(targetUsername) ?: return@withContext
        val isFollowingNow = !target.isFollowing

        // Update target follower count
        val updatedTarget = target.copy(
            isFollowing = isFollowingNow,
            followersCount = if (isFollowingNow) target.followersCount + 1 else maxOf(0, target.followersCount - 1)
        )
        dao.insertUser(updatedTarget)

        // Update current following count
        val updatedCurrent = current.copy(
            followingCount = if (isFollowingNow) current.followingCount + 1 else maxOf(0, current.followingCount - 1)
        )
        dao.insertUser(updatedCurrent)
        _currentUser.value = updatedCurrent

        if (isFollowingNow) {
            val notif = NotificationEntity(
                type = "FOLLOW",
                senderId = current.username,
                senderName = current.displayName,
                senderAvatar = current.photoUrl,
                text = "started following you"
            )
            dao.insertNotification(notif)
        }
    }

    suspend fun addComment(postId: Int, text: String) = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext
        val post = dao.getAllPostsFlow().first().find { it.id == postId } ?: return@withContext

        val comment = CommentEntity(
            postId = postId,
            authorId = current.username,
            authorName = current.displayName,
            authorAvatar = current.photoUrl,
            text = text
        )
        dao.insertComment(comment)

        // Increment commentsCount
        val updatedPost = post.copy(commentsCount = post.commentsCount + 1)
        dao.insertPost(updatedPost)

        if (post.authorId != current.username) {
            val notif = NotificationEntity(
                type = "COMMENT",
                senderId = current.username,
                senderName = current.displayName,
                senderAvatar = current.photoUrl,
                postId = postId,
                text = "commented: \"${text.take(30)}...\""
            )
            dao.insertNotification(notif)
        }
    }

    suspend fun deleteComment(comment: CommentEntity) = withContext(Dispatchers.IO) {
        val post = dao.getAllPostsFlow().first().find { it.id == comment.postId }
        dao.deleteComment(comment)

        if (post != null) {
            val updatedPost = post.copy(commentsCount = maxOf(0, post.commentsCount - 1))
            dao.insertPost(updatedPost)
        }
    }

    suspend fun createPost(caption: String, mediaUrl: String, location: String? = null, hashtags: String = "", isSensitive: Boolean = false): Long = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext -1L
        val post = PostEntity(
            authorId = current.username,
            authorName = current.displayName,
            authorAvatar = current.photoUrl,
            mediaUrl = mediaUrl,
            caption = caption,
            location = location,
            hashtags = hashtags,
            isSensitive = isSensitive
        )
        val id = dao.insertPost(post)

        // Update postsCount
        val updatedUser = current.copy(postsCount = current.postsCount + 1)
        dao.insertUser(updatedUser)
        _currentUser.value = updatedUser

        return@withContext id
    }

    suspend fun deletePost(post: PostEntity) = withContext(Dispatchers.IO) {
        dao.deletePost(post)
        val current = _currentUser.value
        if (current != null && current.username == post.authorId) {
            val updatedUser = current.copy(postsCount = maxOf(0, current.postsCount - 1))
            dao.insertUser(updatedUser)
            _currentUser.value = updatedUser
        }
    }

    suspend fun archivePost(postId: Int, archive: Boolean) = withContext(Dispatchers.IO) {
        val post = dao.getAllPostsFlow().first().find { it.id == postId } ?: return@withContext
        val updated = post.copy(isArchived = archive)
        dao.insertPost(updated)
    }

    suspend fun createStory(mediaUrl: String) = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext
        val story = StoryEntity(
            authorId = current.username,
            authorName = current.displayName,
            authorAvatar = current.photoUrl,
            mediaUrl = mediaUrl
        )
        dao.insertStory(story)
    }

    suspend fun createReel(caption: String, videoUrl: String, thumbnailUrl: String) = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext
        val reel = ReelEntity(
            authorId = current.username,
            authorName = current.displayName,
            authorAvatar = current.photoUrl,
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            caption = caption
        )
        dao.insertReel(reel)
    }

    suspend fun sendMessage(partnerId: String, text: String, mediaUrl: String? = null) = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext
        val message = MessageEntity(
            chatPartnerId = partnerId,
            senderId = current.username,
            text = text,
            mediaUrl = mediaUrl
        )
        dao.insertMessage(message)

        // Trigger dynamic message notification
        val partner = dao.getUser(partnerId)
        if (partner != null) {
            val notif = NotificationEntity(
                type = "MESSAGE",
                senderId = current.username,
                senderName = current.displayName,
                senderAvatar = current.photoUrl,
                text = "sent you a message: \"${text.take(20)}...\""
            )
            dao.insertNotification(notif)
        }
    }

    suspend fun createReport(type: String, targetId: String, reason: String, description: String, evidenceUrl: String?) = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext
        val report = ReportEntity(
            reportedType = type,
            targetId = targetId,
            reason = reason,
            description = description,
            evidenceUrl = evidenceUrl,
            reporterId = current.username
        )
        dao.insertReport(report)

        // Notify Admin of Report
        val adminNotif = NotificationEntity(
            type = "REPORT",
            senderId = "system",
            senderName = "Pixora Safety",
            senderAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            text = "New $type report submitted for \"$targetId\" (Reason: $reason)"
        )
        dao.insertNotification(adminNotif)
    }

    // --- Safety and Blocking ---
    suspend fun toggleBlockUser(username: String) = withContext(Dispatchers.IO) {
        val target = dao.getUser(username) ?: return@withContext
        val updated = target.copy(isBlocked = !target.isBlocked)
        dao.insertUser(updated)
    }

    suspend fun toggleMuteUser(username: String) = withContext(Dispatchers.IO) {
        val target = dao.getUser(username) ?: return@withContext
        val updated = target.copy(isMuted = !target.isMuted)
        dao.insertUser(updated)
    }

    suspend fun toggleRestrictUser(username: String) = withContext(Dispatchers.IO) {
        val target = dao.getUser(username) ?: return@withContext
        val updated = target.copy(isRestricted = !target.isRestricted)
        dao.insertUser(updated)
    }

    // --- Admin Operations ---
    suspend fun updateReportStatus(reportId: Int, status: String) = withContext(Dispatchers.IO) {
        val reports = dao.getAllReportsFlow().first()
        val report = reports.find { it.id == reportId } ?: return@withContext
        val updated = report.copy(status = status)
        dao.insertReport(updated)
    }

    suspend fun setUserVerification(username: String, verified: Boolean) = withContext(Dispatchers.IO) {
        val user = dao.getUser(username) ?: return@withContext
        val updated = user.copy(isVerified = verified)
        dao.insertUser(updated)

        val notif = NotificationEntity(
            type = "VERIFY",
            senderId = "system",
            senderName = "Pixora Team",
            senderAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            text = if (verified) "Congratulations! Your account has been verified." else "Your verification status has been updated."
        )
        dao.insertNotification(notif)
    }

    suspend fun setUserRole(username: String, role: String) = withContext(Dispatchers.IO) {
        val user = dao.getUser(username) ?: return@withContext
        val updated = user.copy(role = role)
        dao.insertUser(updated)
    }

    suspend fun deleteUser(username: String) = withContext(Dispatchers.IO) {
        // Find user & delete posts/comments, etc
        val user = dao.getUser(username) ?: return@withContext
        // Remove from db (just delete the user entity, and remove user posts)
        val posts = dao.getPostsByAuthorFlow(username).first()
        for (post in posts) {
            dao.deletePost(post)
        }
        // Instead of strict cascades, we block/delete user entity
        // To prevent crashes, we can delete the user row
        // But for local safety we can set role to 'banned' or delete
        // We'll set role = "banned" or delete
        // Let's delete user
        val userList = dao.getAllUsersFlow().first()
        val userToDelete = userList.find { it.username == username }
        if (userToDelete != null) {
            // Room does not have native delete user unless we create a @Delete or query delete
            // We can just set their displayName to "Banned User" or similar, or we can update role = "Banned"
            val bannedUser = userToDelete.copy(role = "banned", bio = "[This account has been terminated for violating Pixora Terms.]")
            dao.insertUser(bannedUser)
        }
    }

    // --- POPULATE INITIAL ECOSYSTEM ---
    private suspend fun populateInitialDataIfEmpty() {
        val existingUsers = dao.getAllUsersFlow().first()
        if (existingUsers.isNotEmpty()) return

        // Create standard realistic profiles
        val initialUsers = listOf(
            UserEntity(
                username = "alice",
                displayName = "Alice Smith",
                photoUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                bio = "Cozy photographer based in Seattle 🌲. Coffee enthusiast and cat owner.",
                website = "https://alicephoto.me",
                isPrivate = false,
                isVerified = false,
                followersCount = 124,
                followingCount = 89,
                postsCount = 3,
                role = "user"
            ),
            UserEntity(
                username = "sarah_travels",
                displayName = "Sarah Jenkins",
                photoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                bio = "Vagabond & Storyteller ✈️. Capturing wild moments and beautiful landscapes globally.",
                website = "https://sarahtravels.blog",
                isPrivate = false,
                isVerified = true,
                followersCount = 14205,
                followingCount = 421,
                postsCount = 4,
                role = "user"
            ),
            UserEntity(
                username = "chef_marcus",
                displayName = "Chef Marcus",
                photoUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
                bio = "Michelin star dreamer 🧑‍🍳. Baker of golden crust sourdoughs. Food is visual art.",
                website = "https://marcusbakes.com",
                isPrivate = false,
                isVerified = true,
                followersCount = 8950,
                followingCount = 302,
                postsCount = 3,
                role = "user"
            ),
            UserEntity(
                username = "flora_design",
                displayName = "Clara Rivers",
                photoUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150",
                bio = "Interior Designer 🌿. Plant lover & minimalist advocate. Creating breathing homes.",
                website = "https://riversdesign.co",
                isPrivate = false,
                isVerified = false,
                followersCount = 4120,
                followingCount = 550,
                postsCount = 2,
                role = "user"
            ),
            UserEntity(
                username = "neon_dreams",
                displayName = "Leo Zhang",
                photoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                bio = "Cyberpunk digital artist 🌃. Exploring Tokyo neon alleyways and digital architecture.",
                website = "https://leozhang.artstation",
                isPrivate = false,
                isVerified = true,
                followersCount = 92100,
                followingCount = 12,
                postsCount = 2,
                role = "user"
            ),
            UserEntity(
                username = "admin",
                displayName = "Pixora Moderator",
                photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                bio = "Official Pixora Moderation Profile. Building a friendly, safe, and collaborative space.",
                website = "https://pixora.com/guidelines",
                isPrivate = false,
                isVerified = true,
                followersCount = 1,
                followingCount = 0,
                postsCount = 0,
                role = "superadmin"
            )
        )
        dao.insertUsers(initialUsers)

        // Create standard realistic Posts with high-quality landscape URLs
        val initialPosts = listOf(
            PostEntity(
                id = 1,
                authorId = "sarah_travels",
                authorName = "Sarah Jenkins",
                authorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                mediaUrl = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800",
                caption = "Wandering around Kyoto under this magical golden sky. Truly felt like stepping back in time ⛩️✨.",
                location = "Kyoto, Japan",
                hashtags = "#travel #kyoto #japan #wanderlust #sunset",
                likesCount = 2405,
                commentsCount = 3,
                createdAt = System.currentTimeMillis() - 86400000 * 2 // 2 days ago
            ),
            PostEntity(
                id = 2,
                authorId = "chef_marcus",
                authorName = "Chef Marcus",
                authorAvatar = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
                mediaUrl = "https://images.unsplash.com/photo-1549931319-a545dcf3bc73?w=800",
                caption = "Freshly pulled out from the wood oven! The hydration on this rustic sourdough is perfect. Listen to that golden crackle 🥖😋.",
                location = "Marcus Sourdough Workshop",
                hashtags = "#baking #sourdough #bread #culinary #chef",
                likesCount = 852,
                commentsCount = 2,
                createdAt = System.currentTimeMillis() - 86400000 // 1 day ago
            ),
            PostEntity(
                id = 3,
                authorId = "flora_design",
                authorName = "Clara Rivers",
                authorAvatar = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150",
                mediaUrl = "https://images.unsplash.com/photo-1512428559087-560fa5ceab42?w=800",
                caption = "Adding some green vibes to the desk. Snake plants are the ultimate resilient buddies for minimalist workspaces 🌿💻.",
                location = "Seattle Design Lab",
                hashtags = "#plants #minimalism #office #interiordesign",
                likesCount = 1040,
                commentsCount = 2,
                createdAt = System.currentTimeMillis() - 3600000 * 6 // 6 hours ago
            ),
            PostEntity(
                id = 4,
                authorId = "neon_dreams",
                authorName = "Leo Zhang",
                authorAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                mediaUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800",
                caption = "Shibuya streets reflecting neon cyber dreams. Tokyo truly lives in the year 3000 at night 🌃🤖.",
                location = "Shibuya Crossing, Tokyo",
                hashtags = "#cyberpunk #tokyo #neon #digitalart",
                likesCount = 15890,
                commentsCount = 2,
                createdAt = System.currentTimeMillis() - 3600000 * 2 // 2 hours ago
            )
        )
        for (p in initialPosts) {
            dao.insertPost(p)
        }

        // Create standard comments
        val initialComments = listOf(
            CommentEntity(
                postId = 1,
                authorId = "alice",
                authorName = "Alice Smith",
                authorAvatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                text = "Omg Sarah, this is absolutely breathtaking! Adding Kyoto to my list right now! 😍"
            ),
            CommentEntity(
                postId = 1,
                authorId = "chef_marcus",
                authorName = "Chef Marcus",
                authorAvatar = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
                text = "What a stunning sunset! Perfect visual balance."
            ),
            CommentEntity(
                postId = 1,
                authorId = "flora_design",
                authorName = "Clara Rivers",
                authorAvatar = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150",
                text = "The symmetry of the pagoda is beautiful."
            ),
            CommentEntity(
                postId = 2,
                authorId = "alice",
                authorName = "Alice Smith",
                authorAvatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                text = "Can almost smell it through the screen! Please share the recipe! 🤤"
            ),
            CommentEntity(
                postId = 2,
                authorId = "sarah_travels",
                authorName = "Sarah Jenkins",
                authorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                text = "Marcus, this makes me want to fly back and get a slice!"
            ),
            CommentEntity(
                postId = 3,
                authorId = "alice",
                authorName = "Alice Smith",
                authorAvatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                text = "I have a snake plant too! They really are indestructible haha."
            ),
            CommentEntity(
                postId = 3,
                authorId = "neon_dreams",
                authorName = "Leo Zhang",
                authorAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                text = "So clean. Essential setup setup!"
            ),
            CommentEntity(
                postId = 4,
                authorId = "sarah_travels",
                authorName = "Sarah Jenkins",
                authorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                text = "This lighting is insane Leo! Teach me your edit workflow please!"
            ),
            CommentEntity(
                postId = 4,
                authorId = "flora_design",
                authorName = "Clara Rivers",
                authorAvatar = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150",
                text = "Brilliant contrasts. It's like a scene from Blade Runner."
            )
        )
        for (c in initialComments) {
            dao.insertComment(c)
        }

        // Create standard active stories
        val initialStories = listOf(
            StoryEntity(
                authorId = "sarah_travels",
                authorName = "Sarah Jenkins",
                authorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                mediaUrl = "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=500", // travel backpack
                createdAt = System.currentTimeMillis() - 3600000 // 1 hour ago
            ),
            StoryEntity(
                authorId = "chef_marcus",
                authorName = "Chef Marcus",
                authorAvatar = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
                mediaUrl = "https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=500", // prepping kitchen
                createdAt = System.currentTimeMillis() - 7200000 // 2 hours ago
            ),
            StoryEntity(
                authorId = "flora_design",
                authorName = "Clara Rivers",
                authorAvatar = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150",
                mediaUrl = "https://images.unsplash.com/photo-1545241047-6083a3684587?w=500", // zen garden
                createdAt = System.currentTimeMillis() - 10800000 // 3 hours ago
            )
        )
        for (s in initialStories) {
            dao.insertStory(s)
        }

        // Create standard Reels with simulated video assets (or striking image visualizers with music captions)
        val initialReels = listOf(
            ReelEntity(
                id = 1,
                authorId = "sarah_travels",
                authorName = "Sarah Jenkins",
                authorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-highway-in-the-forest-from-above-44445-large.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1473448912268-2022ce9509d8?w=800",
                caption = "The open road is calling! Scenic views of the Olympic Highway from above 🌲🚗.",
                likesCount = 5904,
                commentsCount = 14
            ),
            ReelEntity(
                id = 2,
                authorId = "chef_marcus",
                authorName = "Chef Marcus",
                authorAvatar = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
                videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-chef-preparing-a-fresh-vegetable-salad-34351-large.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1543083115-638c32cd3d58?w=800",
                caption = "Assembling the signature Garden Salad. Fresh dressing, crispy greens, zero compromises 🥗✨.",
                likesCount = 2810,
                commentsCount = 9
            ),
            ReelEntity(
                id = 3,
                authorId = "neon_dreams",
                authorName = "Leo Zhang",
                authorAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-cyberpunk-neon-city-streets-at-night-42284-large.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=800",
                caption = "Late night Tokyo walk. Immersive lights, digital echoes, rain reflections 🌆🌧️.",
                likesCount = 12400,
                commentsCount = 42
            )
        )
        for (r in initialReels) {
            dao.insertReel(r)
        }

        // Create initial chat messages (so chat list is populated)
        val initialMessages = listOf(
            MessageEntity(
                chatPartnerId = "sarah_travels",
                senderId = "sarah_travels",
                text = "Hey Alice! Are you free for coffee in Seattle next week? I'm visiting for a travel panel!",
                createdAt = System.currentTimeMillis() - 3600000 * 5,
                isSeen = true
            ),
            MessageEntity(
                chatPartnerId = "sarah_travels",
                senderId = "alice",
                text = "Oh my gosh, absolutely Sarah! I'd love to show you my favorite coffee spots! ☕️",
                createdAt = System.currentTimeMillis() - 3600000 * 4,
                isSeen = true
            ),
            MessageEntity(
                chatPartnerId = "sarah_travels",
                senderId = "sarah_travels",
                text = "Amazing! Let's meet at Capitol Hill. I will text you when I land!",
                createdAt = System.currentTimeMillis() - 3600000 * 3,
                isSeen = false
            ),
            MessageEntity(
                chatPartnerId = "chef_marcus",
                senderId = "chef_marcus",
                text = "Hi Alice! The sourdough baking class starts at 10 AM tomorrow. Don't forget your apron!",
                createdAt = System.currentTimeMillis() - 3600000 * 2,
                isSeen = false
            )
        )
        for (m in initialMessages) {
            dao.insertMessage(m)
        }

        // Create default notifications
        val initialNotifs = listOf(
            NotificationEntity(
                type = "FOLLOW",
                senderId = "sarah_travels",
                senderName = "Sarah Jenkins",
                senderAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                text = "started following you."
            ),
            NotificationEntity(
                type = "LIKE",
                senderId = "chef_marcus",
                senderName = "Chef Marcus",
                senderAvatar = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
                text = "liked your photo."
            ),
            NotificationEntity(
                type = "VERIFY",
                senderId = "system",
                senderName = "Pixora Team",
                senderAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                text = "Welcome to Pixora! Tap to read our community guidelines."
            )
        )
        for (n in initialNotifs) {
            dao.insertNotification(n)
        }

        // Create standard initial report to populate the reports tab for the moderator admin
        val initialReport = ReportEntity(
            reportedType = "POST",
            targetId = "4", // Leo's Shibuya post
            reason = "Spam",
            description = "This post appears multiple times on my feed.",
            evidenceUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=150",
            reporterId = "sarah_travels",
            status = "PENDING"
        )
        dao.insertReport(initialReport)
    }
}
