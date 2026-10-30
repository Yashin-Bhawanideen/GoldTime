package com.goldtime.app.ui.checkout

enum class PaymentMethod(val label: String) {
    CARD("Card"),
    INSTANT_EFT("Instant EFT")
}

//checks the checkout details again before passing them to the payment callback
fun paymentSelectionError(
    order: OrderReview,
    details: DeliveryDetails,
    method: PaymentMethod?
): String? = when {
    !order.canContinue -> "Your cart must contain an item with a payable total."
    validateDelivery(details).isNotEmpty() -> "Please return and complete your delivery details."
    method == null -> "Select Card or Instant EFT to continue."
    else -> null
}
