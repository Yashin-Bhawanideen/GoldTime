package com.goldtime.app.data

import org.json.JSONObject
import java.math.BigDecimal
import java.net.URI
import java.net.URLEncoder

enum class PaymentNavigation { PAYFAST, RETURN, CANCEL, BLOCK }

data class PayFastForm(val actionUrl: String, val fields: List<Pair<String, String>>) {
    fun postData(): ByteArray = fields.joinToString("&") { (key, value) ->
        "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
    }.toByteArray(Charsets.UTF_8)

    //checks the complete HTTPS address, not whether the host merely contains a name (Android Developers, n.d.)
    fun navigation(url: String): PaymentNavigation = runCatching {
        val uri = URI(url)
        if (uri.scheme != "https" || uri.userInfo != null || uri.port !in listOf(-1, 443)) return@runCatching PaymentNavigation.BLOCK
        fun matches(key: String): Boolean {
            val expected = URI(fields.first { it.first == key }.second)
            return uri.host == expected.host && uri.path == expected.path
        }
        when {
            matches("return_url") -> PaymentNavigation.RETURN
            matches("cancel_url") -> PaymentNavigation.CANCEL
            uri.host == "sandbox.payfast.co.za" -> PaymentNavigation.PAYFAST
            else -> PaymentNavigation.BLOCK
        }
    }.getOrDefault(PaymentNavigation.BLOCK)

    companion object {
        fun parse(json: JSONObject, order: SavedOrder, callbackBaseUrl: String): PayFastForm {
            require(json.getString("environment") == "sandbox")
            require(json.getString("actionUrl") == "https://sandbox.payfast.co.za/eng/process")
            val array = json.getJSONArray("fields")
            val fields = (0 until array.length()).map {
                array.getJSONObject(it).let { field -> field.getString("key") to field.getString("value") }
            }
            require(fields.map { it.first }.distinct().size == fields.size)
            val values = fields.toMap()
            require(values.keys == setOf("merchant_id", "merchant_key", "return_url", "cancel_url", "notify_url",
                "m_payment_id", "amount", "item_name", "signature"))
            require(values["m_payment_id"] == order.id && BigDecimal(values.getValue("amount")).compareTo(order.order.total) == 0)
            require(Regex("[0-9]{8}").matches(values.getValue("merchant_id")))
            require(values.getValue("merchant_key").isNotBlank() && Regex("[a-f0-9]{32}").matches(values.getValue("signature")))
            for (path in listOf("return", "cancel", "notify")) {
                require(values["${path}_url"] == "${callbackBaseUrl.trimEnd('/')}/api/payments/payfast/$path")
                require(URI(values.getValue("${path}_url")).scheme == "https")
            }
            return PayFastForm(json.getString("actionUrl"), fields)
        }
    }
}

/* REFERENCE LIST
Android Developers. n.d. Webviews - Unsafe URI Loading. [Online]. Available at:
https://developer.android.com/privacy-and-security/risks/unsafe-uri-loading [Accessed 1 October 2026].
*/
