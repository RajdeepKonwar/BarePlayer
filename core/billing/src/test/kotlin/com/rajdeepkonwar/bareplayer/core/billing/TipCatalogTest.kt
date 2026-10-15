package com.rajdeepkonwar.bareplayer.core.billing

import org.junit.Assert.assertEquals
import org.junit.Test

class TipCatalogTest {
    @Test
    fun toProducts_ordersByPriceAndDropsUnknownProducts() {
        val products = TipCatalog.toProducts(
            listOf(
                TipCatalog.Offer("tip_large", "Large tip", "$4.99", 4_990_000),
                TipCatalog.Offer("legacy_item", "Old item", "$9.99", 9_990_000),
                TipCatalog.Offer("tip_small", "Small tip", "$0.99", 990_000),
                TipCatalog.Offer("tip_medium", "Medium tip", "$2.99", 2_990_000),
            ),
        )
        assertEquals(listOf("tip_small", "tip_medium", "tip_large"), products.map { it.id })
        assertEquals("$0.99", products.first().formattedPrice)
    }

    @Test
    fun toProducts_breaksPriceTiesByCatalogOrder() {
        val products = TipCatalog.toProducts(
            listOf(
                TipCatalog.Offer("tip_medium", "Medium", "€1", 1_000_000),
                TipCatalog.Offer("tip_small", "Small", "€1", 1_000_000),
            ),
        )
        assertEquals(listOf("tip_small", "tip_medium"), products.map { it.id })
    }
}
