package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PixoraDao {
    // Users
    @Query("SELECT * FROM users WHERE username = :username")
    fun getUserFlow(username: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUser(username: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    // Posts
    @Query("SELECT * FROM posts WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE isArchived = 0 AND id = :id")
    fun getPostFlow(id: Int): Flow<PostEntity?>

    @Query("SELECT * FROM posts WHERE isArchived = 0 AND authorId = :authorId ORDER BY createdAt DESC")
    fun getPostsByAuthorFlow(authorId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE isSaved = 1 AND isArchived = 0 ORDER BY createdAt DESC")
    fun getSavedPostsFlow(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity): Long

    @Update
    suspend fun updatePost(post: PostEntity)

    @Delete
    suspend fun deletePost(post: PostEntity)

    // Comments
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun getCommentsForPostFlow(postId: Int): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Delete
    suspend fun deleteComment(comment: CommentEntity)

    // Stories
    @Query("SELECT * FROM stories ORDER BY createdAt DESC")
    fun getAllStoriesFlow(): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Delete
    suspend fun deleteStory(story: StoryEntity)

    // Reels
    @Query("SELECT * FROM reels ORDER BY createdAt DESC")
    fun getAllReelsFlow(): Flow<List<ReelEntity>>

    @Query("SELECT * FROM reels WHERE id = :id")
    fun getReelFlow(id: Int): Flow<ReelEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReel(reel: ReelEntity)

    @Update
    suspend fun updateReel(reel: ReelEntity)

    // Messages (Chat)
    @Query("SELECT * FROM messages WHERE chatPartnerId = :partnerId ORDER BY createdAt ASC")
    fun getMessagesForPartnerFlow(partnerId: String): Flow<List<MessageEntity>>

    @Query("SELECT DISTINCT chatPartnerId FROM messages ORDER BY createdAt DESC")
    fun getChatPartnersFlow(): Flow<List<String>>

    @Query("SELECT * FROM messages WHERE chatPartnerId = :partnerId ORDER BY createdAt DESC LIMIT 1")
    fun getLatestMessageForPartnerFlow(partnerId: String): Flow<MessageEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsAsRead()

    // Reports
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun getAllReportsFlow(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Update
    suspend fun updateReport(report: ReportEntity)
}

@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        CommentEntity::class,
        StoryEntity::class,
        ReelEntity::class,
        MessageEntity::class,
        NotificationEntity::class,
        ReportEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pixoraDao(): PixoraDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pixora_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
