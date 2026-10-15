package com.rajdeepkonwar.bareplayer.core.billing

import com.rajdeepkonwar.bareplayer.core.domain.tips.TipProduct

/**
 * One-time products that must exist in Play Console (Monetize → Products → In-app products).
 * They are consumed after purchase, so people can tip more than once.
 */
object TipCatalog {
    val PRODUCT_IDS = listOf("tip_small", "tip_medium", "tip_large")

    /** Store data for one product, independent of the billing library's types. */
    data class Offer(val id: String, val title: String, val formattedPrice: String, val priceMicros: Long)

    /** Keeps only known tips, cheapest first. */
    fun toProducts(offers: List<Offer>): List<TipProduct> =
        offers.filter { it.id in PRODUCT_IDS }
            .sortedWith(compareBy({ it.priceMicros }, { PRODUCT_IDS.indexOf(it.id) }))
            .map { TipProduct(id = it.id, title = it.title, formattedPrice = it.formattedPrice) }
}
