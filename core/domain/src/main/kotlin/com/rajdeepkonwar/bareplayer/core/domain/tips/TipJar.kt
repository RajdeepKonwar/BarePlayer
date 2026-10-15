package com.rajdeepkonwar.bareplayer.core.domain.tips

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** An optional tip. Tips support development and unlock nothing. */
data class TipProduct(
    val id: String,
    val title: String,
    val formattedPrice: String,
)

data class TipJarState(
    val status: Status = Status.Loading,
    val products: List<TipProduct> = emptyList(),
    val isSupporter: Boolean = false,
    /** Debug builds show sample tips that never charge anything. */
    val isPreview: Boolean = false,
) {
    enum class Status {
        Loading,
        Ready,

        /** No store billing on this install (e.g. not installed from Google Play). */
        Unavailable,
    }
}

sealed interface TipJarEvent {
    data object Thanks : TipJarEvent

    /** The payment needs extra steps (e.g. cash at a store) and will complete later. */
    data object Pending : TipJarEvent
    data object Failed : TipJarEvent
}

interface TipJar {
    val state: StateFlow<TipJarState>
    val events: Flow<TipJarEvent>

    /** Connects to the store and loads tip prices. Cheap to call repeatedly. */
    fun connect()

    fun tip(productId: String)
}
