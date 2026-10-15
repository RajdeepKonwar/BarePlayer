package com.rajdeepkonwar.bareplayer.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface QueueDao {
    @Upsert
    suspend fun save(state: QueueStateEntity)

    @Query("SELECT * FROM queue_state WHERE id = 0")
    suspend fun load(): QueueStateEntity?

    @Query("DELETE FROM queue_state")
    suspend fun clear()
}

@Dao
interface PlayCountDao {
    @Query("UPDATE play_counts SET count = count + 1, last_played = :now WHERE track_id = :trackId")
    suspend fun increment(trackId: Long, now: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PlayCountEntity)

    @Query("SELECT count FROM play_counts WHERE track_id = :trackId")
    suspend fun count(trackId: Long): Int?
}
