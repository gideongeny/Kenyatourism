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
    val activities: String
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

@Dao
interface DestinationDao {
    @Query("SELECT * FROM destinations")
    fun getAllDestinations(): Flow<List<DestinationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(destinations: List<DestinationEntity>)

    @Query("SELECT * FROM destinations WHERE id = :id")
    suspend fun getDestinationById(id: Int): DestinationEntity?

    @Query("SELECT * FROM comments WHERE destinationId = :destinationId ORDER BY timestamp DESC")
    fun getCommentsForDestination(destinationId: Int): Flow<List<CommentEntity>>

    @Insert
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPublicComments(comments: List<CommentEntity>)
}

@Database(entities = [DestinationEntity::class, CommentEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun destinationDao(): DestinationDao
}
