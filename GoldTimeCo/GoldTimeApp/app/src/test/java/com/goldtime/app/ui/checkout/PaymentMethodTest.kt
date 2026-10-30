package com.goldtime.app.ui.checkout

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class PaymentMethodTest {
    private val details = DeliveryDetails("Test Customer", "0820000000",
        "12 Example Street", "Johannesburg", "Gauteng", "2000")
    private val order = OrderReview(
        listOf(CheckoutItem("test", "Test product", 1, BigDecimal("100.00"))),
        BigDecimal("150.00"))

    @Test fun requiresAnExplicitSelection() {
        assertEquals("Select Card or Instant EFT to continue.",
            paymentSelectionError(order, details, null))
    }

    @Test fun acceptsBothMethodsWithValidCheckoutDetails() {
        PaymentMethod.entries.forEach {
            assertNull(paymentSelectionError(order, details, it))
        }
    }

    @Test fun selectedMethodDoesNotAllowAnEmptyCart() {
        val empty = OrderReview(emptyList(), BigDecimal("150.00"))
        assertNotNull(paymentSelectionError(empty, details, PaymentMethod.CARD))
    }

    @Test fun selectedMethodDoesNotAllowInvalidDelivery() {
        assertNotNull(paymentSelectionError(order, details.copy(postalCode = ""),
            PaymentMethod.INSTANT_EFT))
    }

    @Test fun blocksZeroTotalEvenWhenAnItemIsPresent() {
        val free = OrderReview(listOf(CheckoutItem("free", "Free", 1, BigDecimal.ZERO)),
            BigDecimal.ZERO)
        assertNotNull(paymentSelectionError(free, details, PaymentMethod.CARD))
    }
}
