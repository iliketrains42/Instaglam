package com.example.instalgam.room

import android.content.Context
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.instalgam.apiClient.LikeReelBody
import com.example.instalgam.apiClient.RetrofitApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Entity
data class DatabaseReel(
    @PrimaryKey val reelId: String,
    @ColumnInfo(name = "user_name") val userName: String,
    @ColumnInfo(name = "profile_picture") val profilePicture: String,
    @ColumnInfo(name = "reel_video") val reelVideo: String,
    @ColumnInfo(name = "like_count") var likeCount: Int,
    @ColumnInfo(name = "liked_by_user") var likedByUser: Boolean,
)

@Entity
data class PendingReelLike(
    @PrimaryKey val reelId: String,
    @ColumnInfo val liked: Boolean,
)

@Dao
interface PendingReelLikesDao {
    @Query("SELECT * FROM PendingReelLike")
    suspend fun fetchAll(): List<PendingReelLike>

    @Query("SELECT * FROM PendingReelLike WHERE reelId = :reelId")
    suspend fun getByReelId(reelId: String): PendingReelLike?

    @Query("""DELETE FROM PendingReelLike WHERE reelId = :reelID""")
    suspend fun removeLike(reelID: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addLike(reel: PendingReelLike)

    @Query("DELETE FROM PendingReelLike")
    suspend fun flush()
}

@Dao
interface ReelDao {
    @Query("SELECT * FROM DatabaseReel")
    suspend fun fetchAll(): List<DatabaseReel>

    @Query("""UPDATE DatabaseReel SET like_count = like_count + 1, liked_by_user = 1 WHERE reelId = :reelID""")
    suspend fun like(reelID: String)

    @Query("""UPDATE DatabaseReel SET like_count = like_count - 1, liked_by_user = 0 WHERE reelId = :reelID""")
    suspend fun dislike(reelID: String)

    @Insert
    suspend fun insertAll(reels: List<DatabaseReel>)

    @Query("DELETE FROM DatabaseReel")
    suspend fun deleteAll()
}

@Database(entities = [DatabaseReel::class], version = 1)
abstract class ReelDatabase : RoomDatabase() {
    abstract fun reelDao(): ReelDao

    companion object {
        @Volatile // reads and writes are atomic
        private var instance: ReelDatabase? = null

        fun getInstance(context: Context): ReelDatabase =
            instance ?: synchronized(this) {
                // only one thread can enter this critical section
                instance ?: Room
                    .databaseBuilder(
                        context,
                        ReelDatabase::class.java,
                        "reels-database",
                    ).build()
                    .also { instance = it }
            }
    }
}

@Database(entities = [PendingReelLike::class], version = 1)
abstract class PendingReelLikeDatabase : RoomDatabase() {
    abstract fun pendingReelLikesDao(): PendingReelLikesDao

    companion object {
        @Volatile
        private var instance: PendingReelLikeDatabase? = null

        fun getInstance(context: Context): PendingReelLikeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(
                        context,
                        PendingReelLikeDatabase::class.java,
                        "pending-reel-likes-database",
                    ).build()
                    .also { instance = it }
            }
    }
}

class ReelDatabaseHelper(
    private val reelDao: ReelDao,
) {
    suspend fun getReels(): List<DatabaseReel> = reelDao.fetchAll()

    suspend fun saveReels(reels: List<DatabaseReel>) {
        reelDao.deleteAll()
        reelDao.insertAll(reels)
    }

    suspend fun likeReel(reelID: String): Boolean =
        withContext(Dispatchers.IO) {
            reelDao.like(reelID)

            Log.d("dbStatus", "$reelID liked")
            try {
                val response =
                    RetrofitApiClient.reelsApiService
                        .likeReel(LikeReelBody(true, reelID))

                if (response.isSuccessful) {
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }

    suspend fun dislikeReel(reelID: String): Boolean =
        withContext(Dispatchers.IO) {
            reelDao.dislike(reelID)
            Log.d("dbStatus", "$reelID disliked")
            try {
                val response = RetrofitApiClient.reelsApiService.dislikeReel()

                if (response.isSuccessful) {
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
}

class PendingReelLikeDatabaseHelper(
    private val pendingReelLikesDao: PendingReelLikesDao,
) {
    suspend fun getAllPendingLikes(): List<PendingReelLike> =
        withContext(Dispatchers.IO) {
            pendingReelLikesDao.fetchAll()
        }

    suspend fun getPendingLike(reelId: String): PendingReelLike? =
        withContext(Dispatchers.IO) {
            pendingReelLikesDao.getByReelId(reelId)
        }

    suspend fun addPendingLike(
        reelId: String,
        liked: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            pendingReelLikesDao.addLike(PendingReelLike(reelId, liked))
        }
    }

    suspend fun removePendingLike(reelId: String) {
        withContext(Dispatchers.IO) {
            pendingReelLikesDao.removeLike(reelId)
        }
    }

    suspend fun flushAllPendingLikes() {
        withContext(Dispatchers.IO) {
            pendingReelLikesDao.flush()
        }
    }
}
