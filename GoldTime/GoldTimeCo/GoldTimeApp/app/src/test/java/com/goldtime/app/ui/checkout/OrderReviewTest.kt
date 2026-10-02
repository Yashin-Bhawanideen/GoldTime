package com.goldtime.app.ui.checkout

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class OrderReviewTest {
    @Test fun calculatesMultipleItemsAndChargesDeliveryOnce() {
        val order = OrderReview(listOf(
            CheckoutItem("gold", "Gold", 2, BigDecimal("58995.00")),
            CheckoutItem("silver", "Silver", 1, BigDecimal("21450.00"))
        ), BigDecimal("150.00"))
        assertEquals(BigDecimal("139440.00"), order.subtotal)
        assertEquals(BigDecimal("139590.00"), order.total)
        assertTrue(order.canContinue)
    }

    @Test fun keepsDecimalTotalsExact() {
        val order = OrderReview(listOf(CheckoutItem("sample", "Sample", 3, BigDecimal("0.10"))),
            BigDecimal("0.20"))
        assertEquals(BigDecimal("0.50"), order.total)
    }

    @Test fun emptyCartHasNoDeliveryChargeAndCannotContinue() {
        val order = OrderReview(emptyList(), BigDecimal("150.00"))
        assertEquals(0, order.total.signum())
        assertFalse(order.canContinue)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroQuantity() { CheckoutItem("a", "A", 0, BigDecimal.ONE) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativePrice() { CheckoutItem("a", "A", 1, BigDecimal("-1.00")) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsFractionalCentPrice() { CheckoutItem("a", "A", 1, BigDecimal("1.001")) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeDelivery() { OrderReview(emptyList(), BigDecimal("-1.00")) }
}
