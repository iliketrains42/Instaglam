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
import com.example.instalgam.apiClient.LikeBody
import com.example.instalgam.apiClient.RetrofitApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Entity
data class DatabasePost(
    @PrimaryKey val postId: String,
    @ColumnInfo(name = "user_name") val userName: String,
    @ColumnInfo(name = "profile_picture") val profilePicture: String,
    @ColumnInfo(name = "post_image") val postImage: String,
    @ColumnInfo(name = "like_count") var likeCount: Int,
    @ColumnInfo(name = "liked_by_user") var likedByUser: Boolean,
)

@Entity
data class PendingLike(
    @PrimaryKey val postId: String,
    @ColumnInfo val liked: Boolean,
)

@Dao
interface PendingLikesDao {
    @Query("SELECT * FROM PendingLike")
    suspend fun fetchAll(): List<PendingLike>

    @Query("SELECT * FROM PendingLike WHERE postId = :postId")
    suspend fun getByPostId(postId: String): PendingLike?

    @Query("""DELETE FROM PendingLike WHERE postId = :postID""")
    suspend fun removeLike(postID: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addLike(post: PendingLike)

    @Query("DELETE FROM PendingLike")
    suspend fun flush()
}

@Dao
interface PostDao {
    @Query("SELECT * FROM DatabasePost")
    suspend fun fetchAll(): List<DatabasePost>

    @Query("""UPDATE DatabasePost SET like_count = like_count + 1, liked_by_user = 1 WHERE postId = :postID""")
    suspend fun like(postID: String)

    @Query("""UPDATE DatabasePost SET like_count = like_count - 1, liked_by_user = 0 WHERE postId = :postID""")
    suspend fun dislike(postID: String)

    @Insert
    suspend fun insertAll(posts: List<DatabasePost>)

    @Query("DELETE FROM DatabasePost")
    suspend fun deleteAll()
}

@Database(entities = [DatabasePost::class], version = 1)
abstract class PostDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao

    companion object {
        @Volatile // reads and writes are atomic
        private var instance: PostDatabase? = null

        fun getInstance(context: Context): PostDatabase =
            instance ?: synchronized(this) {
                // only one thread can enter this critical section
                instance ?: Room
                    .databaseBuilder(
                        context,
                        PostDatabase::class.java,
                        "posts-database",
                    ).build()
                    .also { instance = it }
            }
    }
}

@Database(entities = [PendingLike::class], version = 1)
abstract class PendingLikeDatabase : RoomDatabase() {
    abstract fun pendingLikesDao(): PendingLikesDao

    companion object {
        @Volatile
        private var instance: PendingLikeDatabase? = null

        fun getInstance(context: Context): PendingLikeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(
                        context,
                        PendingLikeDatabase::class.java,
                        "pending-likes-database",
                    ).build()
                    .also { instance = it }
            }
    }
}

class PostDatabaseHelper(
    private val postDao: PostDao,
) {
    suspend fun getPosts(): List<DatabasePost> = postDao.fetchAll()

    suspend fun savePosts(posts: List<DatabasePost>) {
        postDao.deleteAll()
        postDao.insertAll(posts)
    }

    suspend fun likePost(postID: String): Boolean =
        withContext(Dispatchers.IO) {
            postDao.like(postID)

            Log.d("dbStatus", "$postID liked")
            try {
                val response =
                    RetrofitApiClient.postsApiService
                        .likePost(LikeBody(true, postID))

                if (response.isSuccessful) {
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }

    suspend fun dislikePost(postID: String): Boolean =

        withContext(Dispatchers.IO) {
            postDao.dislike(postID)
            Log.d("dbStatus", "$postID disliked")
            try {
                val response = RetrofitApiClient.postsApiService.dislikePost()

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

class PendingLikeDatabaseHelper(
    private val pendingLikesDao: PendingLikesDao,
) {
    suspend fun getAllPendingLikes(): List<PendingLike> =
        withContext(Dispatchers.IO) {
            pendingLikesDao.fetchAll()
        }

    suspend fun getPendingLike(postId: String): PendingLike? =
        withContext(Dispatchers.IO) {
            pendingLikesDao.getByPostId(postId)
        }

    suspend fun addPendingLike(
        postId: String,
        liked: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            pendingLikesDao.addLike(PendingLike(postId, liked))
        }
    }

    suspend fun removePendingLike(postId: String) {
        withContext(Dispatchers.IO) {
            pendingLikesDao.removeLike(postId)
        }
    }

    suspend fun flushAllPendingLikes() {
        withContext(Dispatchers.IO) {
            pendingLikesDao.flush()
        }
    }
}
