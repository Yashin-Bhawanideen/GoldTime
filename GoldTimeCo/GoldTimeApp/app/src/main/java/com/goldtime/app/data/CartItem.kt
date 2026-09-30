package com.goldtime.app.data

import java.text.NumberFormat
import java.util.Locale

data class CartItem(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String?,
    val quantity: Int = 1
) {
    val totalAmount: Double get() = price * quantity
}

fun Double.toZarFormat(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "ZAR"))
    return formatter.format(this).replace("ZAR", "R").trim()
}