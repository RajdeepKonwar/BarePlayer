package com.shearless.bareplayer.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackEntity::class,
        TrackFtsEntity::class,
        PlaylistEntity::class,
        PlaylistEntryEntity::class,
        QueueStateEntity::class,
        PlayCountEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class BareDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun queueDao(): QueueDao
    abstract fun playCountDao(): PlayCountDao

    companion object {
        const val FILE_NAME = "library.db"

        fun create(context: Context): BareDatabase =
            Room.databaseBuilder(context.applicationContext, BareDatabase::class.java, FILE_NAME).build()
    }
}
