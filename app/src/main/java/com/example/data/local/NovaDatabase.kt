package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VideoEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        SearchHistoryEntity::class,
        FolderPreferenceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: NovaDatabase? = null

        fun getDatabase(context: Context): NovaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NovaDatabase::class.java,
                    "nova_player_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
