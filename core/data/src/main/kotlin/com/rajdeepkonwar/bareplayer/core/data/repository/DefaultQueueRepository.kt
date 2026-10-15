package com.rajdeepkonwar.bareplayer.core.data.repository

import androidx.room.withTransaction
import com.rajdeepkonwar.bareplayer.core.data.db.BareDatabase
import com.rajdeepkonwar.bareplayer.core.data.db.PlayCountEntity
import com.rajdeepkonwar.bareplayer.core.data.db.toEntity
import com.rajdeepkonwar.bareplayer.core.data.db.toModel
import com.rajdeepkonwar.bareplayer.core.domain.repository.PlayHistoryRepository
import com.rajdeepkonwar.bareplayer.core.domain.repository.QueueRepository
import com.rajdeepkonwar.bareplayer.core.model.QueueSnapshot

class DefaultQueueRepository(database: BareDatabase) : QueueRepository {
    private val dao = database.queueDao()

    override suspend fun save(snapshot: QueueSnapshot) {
        if (snapshot.trackIds.isEmpty()) dao.clear() else dao.save(snapshot.toEntity())
    }

    override suspend fun load(): QueueSnapshot? = dao.load()?.toModel()?.takeIf { it.trackIds.isNotEmpty() }

    override suspend fun clear() = dao.clear()
}

class DefaultPlayHistoryRepository(
    private val database: BareDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : PlayHistoryRepository {
    private val dao = database.playCountDao()

    // Two statements instead of an UPSERT: SQLite's ON CONFLICT DO UPDATE needs API 30+.
    override suspend fun recordPlay(trackId: Long) {
        val now = clock()
        database.withTransaction {
            if (dao.increment(trackId, now) == 0) {
                dao.insert(PlayCountEntity(trackId = trackId, count = 1, lastPlayed = now))
            }
        }
    }

    override suspend fun playCount(trackId: Long): Int = dao.count(trackId) ?: 0
}
