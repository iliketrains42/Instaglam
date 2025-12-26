package com.example.instalgam.room

import android.content.Context
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

@Entity
data class DatabasePost(
    @PrimaryKey val postId: String,
    @ColumnInfo(name = "user_name") val userName: String,
    @ColumnInfo(name = "profile_picture") val profilePicture: String,
    @ColumnInfo(name = "post_image") val postImage: String,
    @ColumnInfo(name = "like_count") var likeCount: Int,
    @ColumnInfo(name = "liked_by_user") var likedByUser: Boolean,
)

@Dao
interface PostDao {
    @Query("SELECT * FROM DatabasePost")
    suspend fun fetchAll(): List<DatabasePost>

    @Query("UPDATE DatabasePost SET like_count = like_count + 1, liked_by_user = 1 WHERE postId = :postID")
    suspend fun like(postID: String)

    @Query("UPDATE DatabasePost SET like_count = like_count - 1, liked_by_user = 0 WHERE postId = :postID")
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

class PostDatabaseHelper(
    private val postDao: PostDao,
) {
    suspend fun getPosts(): List<DatabasePost> = postDao.fetchAll()

    suspend fun savePosts(posts: List<DatabasePost>) {
        postDao.deleteAll()
        postDao.insertAll(posts)
    }
}
