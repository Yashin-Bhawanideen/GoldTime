package com.goldtime.app.ui.checkout

import com.goldtime.app.data.CartItem
import java.math.BigDecimal

//stores a fixed cart snapshot so changing screens cannot change an order being submitted
fun List<CartItem>.checkoutSnapshot(deliveryFee: Double): ArrayList<String> = arrayListOf<String>().apply {
    add(BigDecimal.valueOf(deliveryFee).toPlainString())
    this@checkoutSnapshot.forEach {
        add(it.id); add(it.name); add(it.quantity.toString()); add(BigDecimal.valueOf(it.price).toPlainString())
    }
}

fun List<String>.checkoutOrder(): OrderReview {
    require(isNotEmpty() && (size - 1) % 4 == 0)
    return OrderReview(drop(1).chunked(4).map {
        CheckoutItem(it[0], it[1], it[2].toInt(), it[3].toBigDecimal())
    }, first().toBigDecimal())
}
