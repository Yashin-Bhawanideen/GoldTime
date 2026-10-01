package com.goldtime.app.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldtime.app.data.CartItem
import com.goldtime.app.data.CartRepository
import com.goldtime.app.data.toZarFormat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val total: Double = 0.0
) {
    val subtotalFormated: String get() = subtotal.toZarFormat()
    val deliveryFeeFormatted: String get() = if (items.isEmpty()) "R 0" else deliveryFee.toZarFormat()
    val totalFormatted: String get() = if (items.isEmpty()) "R 0" else total.toZarFormat()
}

class CartViewModel : ViewModel() {
    val state: StateFlow<CartUiState>  = CartRepository.items.map {
        list ->
        val subtotal = list.sumOf { it.totalAmount }
        val delivery = if (list.isNotEmpty()) 150.0 else 0.0
        CartUiState(
            items = list,
            subtotal = subtotal,
            deliveryFee = delivery,
            total = subtotal + delivery
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CartUiState()
        )

    fun increment(id: String) = CartRepository.incrementQuantity(id)
    fun decrement(id: String) = CartRepository.decrementQuantity(id)
    fun remove(id: String) = CartRepository.removeItem(id)
}