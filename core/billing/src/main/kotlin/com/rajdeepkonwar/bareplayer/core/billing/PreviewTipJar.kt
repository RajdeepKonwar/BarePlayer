package com.rajdeepkonwar.bareplayer.core.billing

import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJar
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJarEvent
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJarState
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
        TipCatalog.toProducts(SAMPLE_OFFERS).let { products ->
            TipJarState(
                status = TipJarState.Status.Ready,
                products = products.main,
                customAmounts = products.custom,
                isPreview = true,
            )
        },
    )
    override val state: StateFlow<TipJarState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TipJarEvent>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val events: Flow<TipJarEvent> = _events.asSharedFlow()

    override fun connect() = Unit

    override fun tip(productId: String) {
        _state.update { it.copy(isSupporter = true) }
        _events.tryEmit(TipJarEvent.Thanks)
    }

    private companion object {
        val SAMPLE_OFFERS = listOf(
            TipCatalog.Offer("tip_coffee", "Coffee", "$1.99", 1_990_000),
            TipCatalog.Offer("tip_lunch", "Lunch", "$4.99", 4_990_000),
            TipCatalog.Offer("tip_feature", "Feature", "$9.99", 9_990_000),
        ) + TipCatalog.CUSTOM_IDS.map { id ->
            val dollars = id.removePrefix("tip_custom_").toLong()
            TipCatalog.Offer(id, "$$dollars tip", "$$dollars.00", dollars * 1_000_000)
        }
    }
}
