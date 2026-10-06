package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "USER" or "RIYA"
    val text: String,
    val moodName: String = "ROMANTIC",
    val innerThought: String? = null,
    val gestureBadge: String? = null,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "relationship_state")
data class RelationshipStateEntity(
    @PrimaryKey val id: Int = 1,
    val currentMood: String = "ROMANTIC",
    val loveScore: Int = 85,
    val angerLevel: Int = 0,
    val partnerNickname: String = "जानू",
    val scriptMode: String = "AUTO",
    val avatarStyle: String = "ANIME_NEKO",
    val isVoiceEnabled: Boolean = true,
    val isLiveStageExpanded: Boolean = true,
    val currentInnerThought: String = "हाय! आज अपने जानू से ढेर सारी प्यार भरी बातें करने का मन है... 💖",
    val mananaCount: Int = 3,
    val lastUserReplyTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface RiyaDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC, id DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int = 16): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setMessageFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("SELECT * FROM relationship_state WHERE id = 1")
    fun getRelationshipState(): Flow<RelationshipStateEntity?>

    @Query("SELECT * FROM relationship_state WHERE id = 1")
    suspend fun getRelationshipStateOnce(): RelationshipStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRelationshipState(state: RelationshipStateEntity)

    @Update
    suspend fun updateRelationshipState(state: RelationshipStateEntity)
}

@Database(
    entities = [ChatMessageEntity::class, RelationshipStateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RiyaDatabase : RoomDatabase() {
    abstract fun riyaDao(): RiyaDao

    companion object {
        @Volatile
        private var INSTANCE: RiyaDatabase? = null

        fun getInstance(context: Context): RiyaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RiyaDatabase::class.java,
                    "riya_companion_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
