package com.rajdeepkonwar.bareplayer.core.domain.repository

import com.rajdeepkonwar.bareplayer.core.model.QueueSnapshot

interface QueueRepository {
    suspend fun save(snapshot: QueueSnapshot)
    suspend fun load(): QueueSnapshot?
    suspend fun clear()
}
