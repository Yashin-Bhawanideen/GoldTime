package com.goldtime.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
//object makes this a singleton, so every screen shares the same cart
object CartRepository {
    const val deliveryFee = 150.0
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    //adds an asset to the cart, or increases its quantity if it is already there
    //the price defaults to 58995.00 if the caller does not supply one
    fun addItem(asset: FeaturedAsset, price: Double = 58995.00) {
        _items.update { currentList ->
            val existing = currentList.find { it.id == asset.id }
            if (existing != null) {
                currentList.map {
                    if (it.id == asset.id) it.copy(quantity = (it.quantity + 1).coerceAtMost(99)) else it
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
//adds 1 to the quantity of the item with this ID, up to a maximum of 99
    fun incrementQuantity(id: String) {
        _items.update { list ->
            list.map { if (it.id == id) it.copy(quantity = (it.quantity + 1).coerceAtMost(99)) else it }
        }
    }
//removes 1 from the quantity of the item with this ID
    //mapNotNull drops an item when it returns null, so an item with quantity 1 is removed from the cart
    fun decrementQuantity(id: String) {
        _items.update { list ->
            list.mapNotNull { item ->
                if (item.id == id) {
                    if (item.quantity > 1) item.copy(quantity = item.quantity -1) else null
                } else item
            }
        }
    }
 //removes the item with this ID from the cart completely, whatever its quantity
    fun removeItem(id: String) {
        _items.update { list -> list.filterNot {it.id == id}}
    }
//empties the cart (e.g. after a successful order)
    fun clearCart() {
        _items.value = emptyList()
    }
}
//References
//Developer, A., 2024. Define work requests. [Online]
//Available at: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work
//Developer, A., 2025. Data layer. [Online]
//Available at: https://developer.android.com/topic/architecture/data-layer
//Kotlin, 2026. Kotlin libraries. [Online]
//Available at: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/
