package com.goldtime.app.ui.checkout

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class CheckoutItem(
    val productId: String,
    val name: String,
    val quantity: Int,
    val unitPrice: BigDecimal
) {
    init {
        require(productId.isNotBlank() && name.isNotBlank())
        require(quantity > 0)
        require(unitPrice.signum() >= 0 && unitPrice.stripTrailingZeros().scale() <= 2)
    }

    //uses decimal arithmetic so currency totals stay exact (Oracle, n.d.)
    val lineTotal: BigDecimal get() = unitPrice.multiply(BigDecimal(quantity))
}

data class OrderReview(val items: List<CheckoutItem>, val deliveryFee: BigDecimal) {
    init {
        require(deliveryFee.signum() >= 0 && deliveryFee.stripTrailingZeros().scale() <= 2)
    }

    val subtotal: BigDecimal get() = items.fold(BigDecimal.ZERO) { sum, item -> sum + item.lineTotal }
    val delivery: BigDecimal get() = if (items.isEmpty()) BigDecimal.ZERO else deliveryFee
    val total: BigDecimal get() = subtotal + delivery
    val canContinue: Boolean get() = items.isNotEmpty() && total.signum() > 0
}

fun BigDecimal.asRand(): String = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
    currency = Currency.getInstance("ZAR")
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}.format(this)

/*
REFERENCE LIST
Oracle. n.d. BigDecimal (Java SE 17). [Online].
Available at: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/BigDecimal.html
[Accessed 30 September 2026].
*/
