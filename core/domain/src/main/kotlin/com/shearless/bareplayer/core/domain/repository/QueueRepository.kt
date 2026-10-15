package com.shearless.bareplayer.core.domain.repository

import com.shearless.bareplayer.core.model.QueueSnapshot

interface QueueRepository {
    suspend fun save(snapshot: QueueSnapshot)
    suspend fun load(): QueueSnapshot?
    suspend fun clear()
}
