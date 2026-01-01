package com.example.instalgam.room

import android.content.Context
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.instalgam.apiClient.LikeReelBody
import com.example.instalgam.apiClient.RetrofitApiClient
import com.example.instalgam.model.LikeResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

@Entity
data class DatabaseReel(
    @PrimaryKey val reelId: String,
    @ColumnInfo(name = "user_name") val userName: String,
    @ColumnInfo(name = "profile_picture") val profilePicture: String,
    @ColumnInfo(name = "reel_video") val reelVideo: String,
    @ColumnInfo(name = "like_count") var likeCount: Int,
    @ColumnInfo(name = "liked_by_user") var likedByUser: Boolean,
)

@Dao
interface ReelDao {
    @Query("SELECT * FROM DatabaseReel")
    fun fetchAll(): Flow<List<DatabaseReel>>

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

class ReelDatabaseHelper(
    private val reelDao: ReelDao,
) {
    fun getReels(): Flow<List<DatabaseReel>> = reelDao.fetchAll()

    suspend fun saveReels(reels: List<DatabaseReel>) {
        reelDao.deleteAll()
        reelDao.insertAll(reels)
    }

    fun likeReel(
        reelID: String,
        onSuccess: () -> Unit,
        onFailing: () -> Unit,
    ) {
        val call = RetrofitApiClient.reelsApiService.likeReel(LikeReelBody(true, reelID))
        call.enqueue(
            object : retrofit2.Callback<LikeResponse> {
                override fun onResponse(
                    call: Call<LikeResponse>,
                    response: Response<LikeResponse>,
                ) {
                    // UI is reset if the call is unsuccessful
                    if (response.isSuccessful) {
                        CoroutineScope(Dispatchers.IO).launch {
                            reelDao.like(reelID)
                            Log.d("dbStatus", "$reelID is successfully liked")
                        }
                        onSuccess()
                    } else {
                        onFailing()
                    }
                }

                override fun onFailure(
                    call: Call<LikeResponse>,
                    t: Throwable,
                ) {
                    onFailing()
                }
            },
        )
    }

    fun dislikeReel(
        reelID: String,
        onSuccess: () -> Unit,
        onFailing: () -> Unit,
    ) {
        val call = RetrofitApiClient.reelsApiService.dislikeReel()
        call.enqueue(
            object : retrofit2.Callback<LikeResponse> {
                override fun onResponse(
                    call: Call<LikeResponse>,
                    response: Response<LikeResponse>,
                ) {
                    // UI is reset if the call is unsuccessful
                    if (response.isSuccessful) {
                        CoroutineScope(Dispatchers.IO).launch {
                            reelDao.dislike(reelID)
                            Log.d("dbStatus", "$reelID is successfully disliked")
                        }
                        onSuccess()
                    } else {
                        onFailing()
                    }
                }

                override fun onFailure(
                    call: Call<LikeResponse>,
                    t: Throwable,
                ) {
                    onFailing()
                }
            },
        )
    }
}
