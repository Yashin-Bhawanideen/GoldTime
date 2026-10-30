package com.goldtime.app.ui.checkout

import com.goldtime.app.data.CartItem
import com.goldtime.app.data.CartRepository
import com.goldtime.app.data.FeaturedAsset
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CartCheckoutTest {
    @Test fun snapshotKeepsIdsQuantitiesAndExactAmounts() {
        val items = listOf(CartItem("gold", "Gold", 12.35, null, 3), CartItem("silver", "Silver", 2.10, null, 2))
        val order = items.checkoutSnapshot(150.0).checkoutOrder()
        assertEquals(listOf("gold", "silver"), order.items.map { it.productId })
        assertEquals(listOf(3, 2), order.items.map { it.quantity })
        assertEquals(0, BigDecimal("191.25").compareTo(order.total))
    }

    @Test fun snapshotDoesNotChangeWhenCartChanges() {
        CartRepository.clearCart()
        try {
            CartRepository.addItem(FeaturedAsset("gold", "Gold", "", null), 10.0)
            val snapshot = CartRepository.items.value.checkoutSnapshot(CartRepository.deliveryFee)
            CartRepository.incrementQuantity("gold")
            CartRepository.clearCart()
            assertEquals(1, snapshot.checkoutOrder().items.single().quantity)
            assertTrue(CartRepository.items.value.isEmpty())
        } finally { CartRepository.clearCart() }
    }

    @Test fun quantitiesStayWithinApiLimitAndLastDecrementRemovesItem() {
        CartRepository.clearCart()
        try {
            val asset = FeaturedAsset("gold", "Gold", "", null)
            repeat(110) { CartRepository.addItem(asset, 10.0) }
            CartRepository.incrementQuantity("gold")
            assertEquals(99, CartRepository.items.value.single().quantity)
            repeat(99) { CartRepository.decrementQuantity("gold") }
            assertTrue(CartRepository.items.value.isEmpty())
        } finally { CartRepository.clearCart() }
    }

    @Test fun emptyCartCannotProceed() {
        assertFalse(emptyList<CartItem>().checkoutSnapshot(150.0).checkoutOrder().canContinue)
    }
}
