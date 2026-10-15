package com.rajdeepkonwar.bareplayer.core.billing

import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJar
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJarEvent
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJarState
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipProduct
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Debug-only stand-in for [PlayTipJar]: sample prices, and tipping just shows the thank-you flow. */
class PreviewTipJar : TipJar {
    private val _state = MutableStateFlow(
        TipJarState(
            status = TipJarState.Status.Ready,
            products = listOf(
                TipProduct("tip_small", "Small tip", "$0.99"),
                TipProduct("tip_medium", "Medium tip", "$2.99"),
                TipProduct("tip_large", "Large tip", "$4.99"),
            ),
            isPreview = true,
        ),
    )
    override val state: StateFlow<TipJarState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TipJarEvent>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val events: Flow<TipJarEvent> = _events.asSharedFlow()

    override fun connect() = Unit

    override fun tip(productId: String) {
        _state.update { it.copy(isSupporter = true) }
        _events.tryEmit(TipJarEvent.Thanks)
    }
}
