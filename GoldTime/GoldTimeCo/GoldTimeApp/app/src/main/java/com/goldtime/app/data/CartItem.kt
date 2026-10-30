package com.goldtime.app.data

import java.text.NumberFormat
import java.util.Locale
import java.util.Currency

//data class: Kotlin automatically generates equals, hashCode, toString and copy for classes that only hold data
//one line in the shopping cart
data class CartItem(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String?,
    val quantity: Int = 1
) {
    val totalAmount: Double get() = price * quantity
}
//extension function that formats a number as South African rand, so any Double can call .toZarFormat()
fun Double.toZarFormat(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))
    formatter.currency = Currency.getInstance("ZAR")
    return formatter.format(this)
}
