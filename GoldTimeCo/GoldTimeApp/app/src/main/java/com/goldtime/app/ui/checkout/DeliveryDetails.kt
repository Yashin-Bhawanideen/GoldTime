package com.goldtime.app.ui.checkout

data class DeliveryDetails(
    val fullName: String = "",
    val phone: String = "",
    val streetAddress: String = "",
    val city: String = "",
    val province: String = "",
    val postalCode: String = ""
) {
    fun trimmed() = copy(
        fullName = fullName.trim(),
        phone = phone.trim(),
        streetAddress = streetAddress.trim(),
        city = city.trim(),
        province = province.trim(),
        postalCode = postalCode.trim()
    )
}

val southAfricanProvinces = listOf(
    "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal", "Limpopo",
    "Mpumalanga", "North West", "Northern Cape", "Western Cape"
)

// Client-side checks provide feedback; the order API must validate again.
fun validateDelivery(details: DeliveryDetails): Map<String, String> {
    val data = details.trimmed()
    val errors = mutableMapOf<String, String>()
    if (data.fullName.isBlank() || data.fullName.length > 100) {
        errors["fullName"] = "Enter a full name (up to 100 characters)."
    }
    val compactPhone = data.phone.filterNot { it == ' ' || it == '-' || it == '(' || it == ')' }
    if (!Regex("""\+?[0-9]{7,15}""").matches(compactPhone)) {
        errors["phone"] = "Enter a phone number with 7–15 digits and an optional leading +."
    }
    if (data.streetAddress.isBlank() || data.streetAddress.length > 200) {
        errors["streetAddress"] = "Enter a street address (up to 200 characters)."
    }
    if (data.city.isBlank() || data.city.length > 100) {
        errors["city"] = "Enter a city (up to 100 characters)."
    }
    if (southAfricanProvinces.none { it.equals(data.province, ignoreCase = true) }) {
        errors["province"] = "Enter one of South Africa's nine provinces."
    }
    if (!Regex("[0-9]{4}").matches(data.postalCode)) {
        errors["postalCode"] = "Enter a four-digit South African postal code."
    }
    return errors
}
