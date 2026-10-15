package com.rajdeepkonwar.bareplayer.core.billing

import android.content.Context
import androidx.core.content.edit
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJar
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJarEvent
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJarState
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJarState.Status
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Tip jar backed by Google Play Billing. Billing talks to the Play Store app over IPC, so it works
 * without the INTERNET permission (which the app strips from its dependencies).
 */
class PlayTipJar(
    context: Context,
    private val activities: ForegroundActivityTracker,
) : TipJar {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(TipJarState(isSupporter = prefs.getBoolean(KEY_SUPPORTER, false)))
    override val state: StateFlow<TipJarState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TipJarEvent>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val events: Flow<TipJarEvent> = _events.asSharedFlow()

    private val purchasesListener = PurchasesUpdatedListener { result, purchases -> onPurchasesUpdated(result, purchases) }

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private var details: Map<String, ProductDetails> = emptyMap()
    private var connecting = false

    override fun connect() {
        if (client.isReady) {
            loadProducts()
            settleOutstandingPurchases()
            return
        }
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingResponseCode.OK) {
                    loadProducts()
                    settleOutstandingPurchases()
                } else {
                    _state.update { it.copy(status = Status.Unavailable) }
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
            }
        })
    }

    override fun tip(productId: String) {
        val product = details[productId]
        val activity = activities.current
        if (product == null || activity == null) {
            _events.tryEmit(TipJarEvent.Failed)
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()),
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingResponseCode.OK) _events.tryEmit(TipJarEvent.Failed)
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> purchases.orEmpty().forEach { handle(it, announce = true) }
            BillingResponseCode.USER_CANCELED -> Unit
            BillingResponseCode.ITEM_ALREADY_OWNED -> settleOutstandingPurchases()
            else -> _events.tryEmit(TipJarEvent.Failed)
        }
    }

    private fun loadProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                TipCatalog.PRODUCT_IDS.map {
                    QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(ProductType.INAPP).build()
                },
            )
            .build()
        client.queryProductDetailsAsync(params) { result, productResult ->
            val list = productResult.productDetailsList
            if (result.responseCode != BillingResponseCode.OK || list.isEmpty()) {
                _state.update { it.copy(status = Status.Unavailable, products = emptyList(), customAmounts = emptyList()) }
                return@queryProductDetailsAsync
            }
            details = list.associateBy { it.productId }
            val offers = list.mapNotNull { pd ->
                val offer = pd.oneTimePurchaseOfferDetails ?: return@mapNotNull null
                TipCatalog.Offer(pd.productId, pd.name, offer.formattedPrice, offer.priceAmountMicros)
            }
            val products = TipCatalog.toProducts(offers)
            _state.update { it.copy(status = Status.Ready, products = products.main, customAmounts = products.custom) }
        }
    }

    /** Consumes purchases that weren't finished (e.g. the app was killed), so they aren't refunded. */
    private fun settleOutstandingPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) purchases.forEach { handle(it, announce = false) }
        }
    }

    private fun handle(purchase: Purchase, announce: Boolean) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                markSupporter()
                client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { _, _ -> }
                if (announce) _events.tryEmit(TipJarEvent.Thanks)
            }
            Purchase.PurchaseState.PENDING -> if (announce) _events.tryEmit(TipJarEvent.Pending)
            else -> Unit
        }
    }

    private fun markSupporter() {
        if (!prefs.getBoolean(KEY_SUPPORTER, false)) prefs.edit { putBoolean(KEY_SUPPORTER, true) }
        _state.update { it.copy(isSupporter = true) }
    }

    private companion object {
        const val PREFS = "tip_jar"
        const val KEY_SUPPORTER = "supporter"
    }
}
