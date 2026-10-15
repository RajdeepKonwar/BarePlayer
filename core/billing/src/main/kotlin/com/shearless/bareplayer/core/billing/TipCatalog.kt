package com.shearless.bareplayer.core.billing

import com.shearless.bareplayer.core.domain.tips.TipProduct

/**
 * One-time products that must exist in Play Console (Monetize → Products → In-app products).
 * They are consumed after purchase, so people can tip more than once. Play only sells fixed
 * prices, so the custom input can only accept amounts backed by products in this catalog.
 */
object TipCatalog {
    val MAIN_IDS = listOf("tip_coffee", "tip_lunch", "tip_feature")
    const val FEATURED_ID = "tip_lunch"
    val CUSTOM_IDS = listOf(1, 2, 3, 5, 10, 15, 20, 25, 50, 100).map { "tip_custom_$it" }
    val PRODUCT_IDS = MAIN_IDS + CUSTOM_IDS

    /** Store data for one product, independent of the billing library's types. */
    data class Offer(val id: String, val title: String, val formattedPrice: String, val priceMicros: Long)

    data class Products(val main: List<TipProduct>, val custom: List<TipProduct>)

    /** Splits known tips into the main buttons and custom-input amounts, each cheapest first. */
    fun toProducts(offers: List<Offer>): Products {
        fun pick(ids: List<String>) = offers.filter { it.id in ids }
            .sortedWith(compareBy({ it.priceMicros }, { ids.indexOf(it.id) }))
            .map { TipProduct(id = it.id, title = it.title, formattedPrice = it.formattedPrice, featured = it.id == FEATURED_ID) }
        return Products(main = pick(MAIN_IDS), custom = pick(CUSTOM_IDS))
    }
}
