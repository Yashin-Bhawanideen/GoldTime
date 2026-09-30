package com.goldtime.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object CartRepository {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    fun addItem(asset: FeaturedAsset, price: Double = 58995.00) {
        _items.update { currentList ->
            val existing = currentList.find { it.id == asset.id }
            if (existing != null) {
                currentList.map {
                    if (it.id == asset.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                currentList + CartItem(
                    id = asset.id,
                    name = asset.name,
                    price = price,
                    imageUrl = asset.imageUrl
                )
            }
        }
    }

    fun incrementQuantity(id: String) {
        _items.update { list ->
            list.map { if (it.id == id) it.copy(quantity = it.quantity + 1) else it }
        }
    }

    fun decrementQuantity(id: String) {
        _items.update { list ->
            list.mapNotNull { item ->
                if (item.id == id) {
                    if (item.quantity > 1) item.copy(quantity = item.quantity -1) else null
                } else item
            }
        }
    }

    fun removeItem(id: String) {
        _items.update { list -> list.filterNot {it.id == id}}
    }

    fun clearCart() {
        _items.value = emptyList()
    }
}