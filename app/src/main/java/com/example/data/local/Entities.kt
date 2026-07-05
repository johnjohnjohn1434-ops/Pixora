package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String, // unique handler
    val displayName: String,
    val photoUrl: String,
    val bio: String,
    val website: String,
    val isPrivate: Boolean = false,
    val isVerified: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val isFollowing: Boolean = false,
    val isBlocked: Boolean = false,
    val isMuted: Boolean = false,
    val isRestricted: Boolean = false,
    val role: String = "user" // user, admin, superadmin
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorId: String, // username of creator
    val authorName: String,
    val authorAvatar: String,
    val mediaUrl: String,
    val caption: String,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val location: String? = null,
    val hashtags: String = "", // comma separated
    val isArchived: Boolean = false,
    val isSensitive: Boolean = false
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val mediaUrl: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isViewed: Boolean = false
)

@Entity(tableName = "reels")
data class ReelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val caption: String,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatPartnerId: String, // the other user in 1-to-1 chat
    val senderId: String,
    val text: String,
    val mediaUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSeen: Boolean = false
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "LIKE", "COMMENT", "FOLLOW", "MESSAGE", "VERIFY"
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val postId: Int? = null,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reportedType: String, // "POST", "USER", "REEL"
    val targetId: String, // postId, username, or reelId
    val reason: String,
    val description: String,
    val evidenceUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val reporterId: String,
    val status: String = "PENDING" // PENDING, UNDER_REVIEW, RESOLVED, REJECTED
)
