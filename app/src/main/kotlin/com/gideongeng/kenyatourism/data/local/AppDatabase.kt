package com.gideongeng.kenyatourism.data.local

import androidx.room.*
import com.gideongeng.kenyatourism.data.Destination
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "destinations")
data class DestinationEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val category: String,
    val description: String,
    val imageUrl: String,
    val rating: Float,
    val region: String,
    val latitude: Double?,
    val longitude: Double?,
    val activities: String,
    val imageGalleryCache: String? = null // Comma-separated URLs
)

@Entity(
    tableName = "comments",
    foreignKeys = [ForeignKey(
        entity = DestinationEntity::class,
        parentColumns = ["id"],
        childColumns = ["destinationId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val destinationId: Int,
    val userName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_media",
    foreignKeys = [ForeignKey(
        entity = DestinationEntity::class,
        parentColumns = ["id"],
        childColumns = ["destinationId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class UserMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val destinationId: Int,
    val url: String,
    val mediaType: String, // "image" or "video"
    val userName: String = "Traveler",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DestinationDao {
    @Query("SELECT * FROM destinations")
    fun getAllDestinations(): Flow<List<DestinationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(destinations: List<DestinationEntity>)

    @Query("SELECT * FROM destinations WHERE id = :id")
    suspend fun getDestinationById(id: Int): DestinationEntity?

    @Query("UPDATE destinations SET imageGalleryCache = :gallery WHERE id = :id")
    suspend fun updateGalleryCache(id: Int, gallery: String)

    @Query("SELECT * FROM comments WHERE destinationId = :destinationId ORDER BY timestamp DESC")
    fun getCommentsForDestination(destinationId: Int): Flow<List<CommentEntity>>

    @Insert
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPublicComments(comments: List<CommentEntity>)

    @Query("SELECT * FROM user_media WHERE destinationId = :destinationId ORDER BY timestamp DESC")
    fun getMediaForDestination(destinationId: Int): Flow<List<UserMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: UserMediaEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPublicMedia(media: List<UserMediaEntity>)
}

@Database(entities = [DestinationEntity::class, CommentEntity::class, UserMediaEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun destinationDao(): DestinationDao
}
