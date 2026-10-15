package com.shearless.bareplayer.core.domain.repository

interface PlayHistoryRepository {
    suspend fun recordPlay(trackId: Long)
    suspend fun playCount(trackId: Long): Int
}
