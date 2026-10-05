package com.goldtime.app.data

import com.goldtime.app.ui.checkout.CheckoutItem
import com.goldtime.app.ui.checkout.DeliveryDetails
import com.goldtime.app.ui.checkout.OrderReview
import com.goldtime.app.ui.checkout.PaymentMethod
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OrderDraft(
    val order: OrderReview,
    val delivery: DeliveryDetails,
    val method: PaymentMethod
) {

    // Only sends product IDs and quantities.
    // Prices and payment status belong to the server.
    fun toJson(requestId: String): JSONObject =
        JSONObject()
            .put("requestId", requestId)
            .put(
                "items",
                JSONArray().apply {
                    order.items
                        .sortedBy { it.productId }
                        .forEach { item ->
                            put(
                                JSONObject()
                                    .put("productId", item.productId)
                                    .put("quantity", item.quantity)
                            )
                        }
                }
            )
            .put(
                "delivery",
                delivery.trimmed().let {
                    JSONObject()
                        .put("fullName", it.fullName)
                        .put("phone", it.phone)
                        .put("streetAddress", it.streetAddress)
                        .put("city", it.city)
                        .put("province", it.province)
                        .put("postalCode", it.postalCode)
                }
            )
            .put("paymentMethod", method.name)

            //creates a SHA-256 hash of the order contents (without the request ID)
    //the app can use it to tell whether the order details have changed since the last attempt
    fun fingerprint(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(
                toJson("")
                    .toString()
                    .toByteArray(Charsets.UTF_8)
            )
            .joinToString("") {
                "%02x".format(it)
            }
}
//an order as saved and returned by the server
data class SavedOrder(
    val id: String,
    val order: OrderReview,
    val delivery: DeliveryDetails,
    val method: PaymentMethod,
    val status: String,
    val paymentEnvironment: String = "",
    val paymentId: String = "",
    val paidAtUtc: String = "",

    // NEW:
    // The API sends CreatedAtUtc for every order.
    val createdAtUtc: String = ""
) {
//true only when the app can confirm the payment is genuinely complete:
    //status is "sandbox_paid", the environment is "sandbox", the PayFast payment ID is 1 to 30 digits,
    //and the paid time is a valid UTC timestamp
    //this is a check on the data from the server; the server itself decides whether an order is paid
   
    val sandboxPaymentConfirmed: Boolean
        get() =
            status == "sandbox_paid" &&
                    paymentEnvironment == "sandbox" &&
                    Regex("[0-9]{1,30}").matches(paymentId) &&
                    runCatching {
                        java.time.Instant.parse(paidAtUtc)
                    }.isSuccess
}

class OrderApiException(
    val statusCode: Int,
    message: String
) : IOException(message)

class OrderApi(
    private val client: OkHttpClient,
    private val baseUrl: String,
    private val callbackBaseUrl: String = baseUrl
) {

    suspend fun create(
        token: String,
        draft: OrderDraft,
        requestId: String
    ): SavedOrder =
        send(
            Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/orders")
                .header("Authorization", "Bearer $token")
                .post(
                    draft
                        .toJson(requestId)
                        .toString()
                        .toRequestBody(
                            "application/json; charset=utf-8".toMediaType()
                        )
                )
                .build(),
            ::parseOrder
        )

    suspend fun history(
        token: String
    ): List<SavedOrder> =
        sendList(
            Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/orders/history")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
        )

    private suspend fun sendList(
        request: Request
    ): List<SavedOrder> =
        suspendCancellableCoroutine { continuation ->

            val call = client.newCall(request)

            continuation.invokeOnCancellation {
                call.cancel()
            }

            call.enqueue(
                object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {

                        val result = runCatching {

                            response.use {

                                if (!it.isSuccessful) {
                                    throw OrderApiException(
                                        it.code,
                                        "Could not load order history."
                                    )
                                }

                                val array = JSONArray(
                                    it.body?.string().orEmpty()
                                )

                                (0 until array.length())
                                    .map { index ->
                                        parseOrder(
                                            array.getJSONObject(index)
                                        )
                                    }
                            }
                        }

                        result.fold(
                            continuation::resume,
                            continuation::resumeWithException
                        )
                    }
                }
            )
        }

    suspend fun get(
        token: String,
        orderId: String
    ): SavedOrder {

        require(
            Regex("[a-f0-9]{64}").matches(orderId)
        ) {
            "Invalid order ID."
        }

        return send(
            Request.Builder()
                .url(
                    "${baseUrl.trimEnd('/')}/api/orders/$orderId"
                )
                .header(
                    "Authorization",
                    "Bearer $token"
                )
                .get()
                .build(),
            ::parseOrder
        )
    }

    suspend fun paymentForm(
        token: String,
        order: SavedOrder
    ): PayFastForm {

        require(
            Regex("[a-f0-9]{64}").matches(order.id) &&
                    order.status == "pending_payment"
        )

        return send(
            Request.Builder()
                .url(
                    "${baseUrl.trimEnd('/')}/api/orders/${order.id}/payment"
                )
                .header(
                    "Authorization",
                    "Bearer $token"
                )
                .post(
                    "".toRequestBody(
                        "application/json".toMediaType()
                    )
                )
                .build()
        ) { json ->
            PayFastForm.parse(
                json,
                order,
                callbackBaseUrl
            )
        }
    }

    private suspend fun <T> send(
        request: Request,
        parse: (JSONObject) -> T
    ): T =
        suspendCancellableCoroutine { continuation ->

            val call = client.newCall(request)

            continuation.invokeOnCancellation {
                call.cancel()
            }

            call.enqueue(
                object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {

                        val result = runCatching {

                            response.use {

                                if (!it.isSuccessful) {

                                    val detail =
                                        runCatching {
                                            JSONObject(
                                                it.peekBody(4096)
                                                    .string()
                                            ).optString("detail")
                                        }.getOrDefault("")

                                    val message = when {

                                        it.code == 503 &&
                                                detail ==
                                                "Delivery pricing is not configured. Please try again later." ->
                                            "Delivery pricing is not configured on the server."

                                        it.code == 503 &&
                                                detail ==
                                                "Could not save your order. Retry with the same request ID." ->
                                            "The server could not save your order. Please retry."

                                        else ->
                                            when (it.code) {

                                                400 ->
                                                    "Check your delivery details and cart quantities."

                                                401, 403 ->
                                                    "Please sign in again before continuing."

                                                404 ->
                                                    "The order service or order could not be found."

                                                409 ->
                                                    "The order may have expired, changed or already been paid. Check its status before trying again."

                                                else ->
                                                    "The order service is unavailable. Please retry."
                                            }
                                    }

                                    throw OrderApiException(
                                        it.code,
                                        "$message (HTTP ${it.code})"
                                    )
                                }

                                try {
                                    parse(
                                        JSONObject(
                                            it.body?.string().orEmpty()
                                        )
                                    )
                                } catch (e: Exception) {
                                    throw IOException(
                                        "The server returned incomplete order details. Please retry.",
                                        e
                                    )
                                }
                            }
                        }

                        result.fold(
                            continuation::resume,
                            continuation::resumeWithException
                        )
                    }
                }
            )
        }
    private fun parseOrder(
        json: JSONObject
    ): SavedOrder {

        require(
            json.getString("currency") == "ZAR"
        )

        val id = json.getString("id")

        require(
            Regex("[a-f0-9]{64}").matches(id)
        )

        val lines = json.getJSONArray("items")

        require(
            lines.length() in 1..50
        )

        val items =
            (0 until lines.length()).map { index ->

                val item =
                    lines.getJSONObject(index)

                val price =
                    item.getLong("unitPriceCents")

                val quantity =
                    item.getInt("quantity")

                require(
                    price > 0 &&
                            quantity in 1..99
                )

                require(
                    Math.multiplyExact(
                        price,
                        quantity.toLong()
                    ) ==
                            item.getLong(
                                "lineTotalCents"
                            )
                )

                CheckoutItem(item.getString("productId"),
                    item.getString("name"),
                    quantity,
                    BigDecimal.valueOf(price, 2))
            }

        require(
            items.map {
                it.productId
            }.distinct().size == items.size
        )

        val order =
            OrderReview(
                items,
                BigDecimal.valueOf(json.getLong("deliveryFeeCents"), 2))

        require(
            order.subtotal ==
                    BigDecimal.valueOf(json.getLong("subtotalCents"), 2)
        )

        require(
            order.total ==
                    BigDecimal.valueOf(json.getLong("totalCents"), 2)
        )

        val address = json.getJSONObject("delivery")

        val createdAtUtc = json.optString("createdAtUtc", "")

        return SavedOrder(
            id = id,
            order = order,
            delivery = DeliveryDetails(
                address.getString("fullName"),
                address.getString("phone"),
                address.getString("streetAddress"),
                address.getString("city"),
                address.getString("province"),
                address.getString("postalCode")
            ),
            method = PaymentMethod.valueOf(
                json.getString("paymentMethod")
            ),
            status = json.getString("status"),
            paymentEnvironment = json.optString(
                "paymentEnvironment",
                ""
            ),
            paymentId = json.optString(
                "payFastPaymentId",
                ""
            ),
            paidAtUtc =
                if (json.isNull("paidAtUtc")) {
                    ""
                } else {
                    json.optString(
                        "paidAtUtc",
                        ""
                    )
                },

            // NEW:
            createdAtUtc = createdAtUtc
        )
    }
}

/* REFERENCE LIST
Square. n.d. Call (OkHttp 4.12.0). [Online]. Available at:
https://github.com/square/okhttp/blob/parent-4.12.0/okhttp/src/main/kotlin/okhttp3/Call.kt
[Accessed 1 October 2026].
JetBrains. n.d. suspendCancellableCoroutine. [Online]. Available at:
https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/suspend-cancellable-coroutine.html
[Accessed 1 October 2026].
// References
// Android, 2026. Android API client-side caching guidelines. [Online] 
// Available at: https://source.android.com/docs/setup/contribute/api-guidelines/caching
// stackoverflow, 2019. What is apiclient in Android?. [Online] 
// Available at: https://stackoverflow.com/questions/59057686/what-is-apiclient-in-android
*/
