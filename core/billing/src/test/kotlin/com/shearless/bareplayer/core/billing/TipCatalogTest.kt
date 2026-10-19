package com.shearless.bareplayer.core.billing

import org.junit.Assert.assertEquals
import org.junit.Test

class TipCatalogTest {
    private fun offer(id: String, micros: Long) = TipCatalog.Offer(id, id, "$" + micros / 1_000_000.0, micros)

    @Test
    fun toProducts_splitsMainAndCustomAndDropsUnknown() {
        val products = TipCatalog.toProducts(
            listOf(
                offer("tip_feature", 9_990_000),
                offer("tip_custom_10", 10_000_000),
                offer("legacy_item", 9_990_000),
                offer("tip_coffee", 1_990_000),
                offer("tip_custom_1", 1_000_000),
                offer("tip_lunch", 4_990_000),
            ),
        )
        assertEquals(listOf("tip_coffee", "tip_lunch", "tip_feature"), products.main.map { it.id })
        assertEquals(listOf("tip_custom_1", "tip_custom_10"), products.custom.map { it.id })
    }

    @Test
    fun toProducts_breaksPriceTiesByCatalogOrder() {
        val products = TipCatalog.toProducts(listOf(offer("tip_lunch", 1_000_000), offer("tip_coffee", 1_000_000)))
        assertEquals(listOf("tip_coffee", "tip_lunch"), products.main.map { it.id })
    }

    @Test
    fun catalog_hasTenCustomAmounts() {
        assertEquals(10, TipCatalog.CUSTOM_IDS.size)
        assertEquals("tip_custom_100", TipCatalog.CUSTOM_IDS.last())
    }
}
