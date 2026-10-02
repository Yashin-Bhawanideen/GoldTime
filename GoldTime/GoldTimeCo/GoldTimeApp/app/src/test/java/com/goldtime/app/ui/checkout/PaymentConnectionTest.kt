package com.goldtime.app.ui.checkout

import com.goldtime.app.data.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class PaymentConnectionTest {
    private fun order() = SavedOrder("a".repeat(64), OrderReview(listOf(
        CheckoutItem("gold", "Gold", 1, BigDecimal("100.00"))), BigDecimal("150.00")),
        DeliveryDetails(), PaymentMethod.CARD, "pending_payment")

    private fun formJson(): JSONObject {
        val fields = listOf("merchant_id" to "12345678", "merchant_key" to "test-key",
            "return_url" to "https://api.example.com/api/payments/payfast/return",
            "cancel_url" to "https://api.example.com/api/payments/payfast/cancel",
            "notify_url" to "https://api.example.com/api/payments/payfast/notify",
            "m_payment_id" to order().id, "amount" to "250.00", "item_name" to "Gold & Silver",
            "signature" to "a".repeat(32))
        return JSONObject().put("actionUrl", "https://sandbox.payfast.co.za/eng/process").put("environment", "sandbox")
            .put("fields", JSONArray().apply { fields.forEach { put(JSONObject().put("key", it.first).put("value", it.second)) } })
    }

    private fun form() = PayFastForm.parse(formJson(), order(), "https://api.example.com")
    private fun paid() = order().copy(status = "sandbox_paid", paymentEnvironment = "sandbox", paymentId = "12345", paidAtUtc = "2026-10-01T00:00:00Z")

    @Test fun acceptsOnlyTheExpectedSandboxForm() {
        assertEquals("https://sandbox.payfast.co.za/eng/process", form().actionUrl)
        assertTrue(String(form().postData()).contains("item_name=Gold+%26+Silver"))
        for (change in listOf("host", "environment", "amount", "order", "callback", "duplicate", "passphrase")) {
            val json = formJson()
            when (change) {
                "host" -> json.put("actionUrl", "https://www.payfast.co.za/eng/process")
                "environment" -> json.put("environment", "live")
                "amount" -> json.getJSONArray("fields").getJSONObject(6).put("value", "1.00")
                "order" -> json.getJSONArray("fields").getJSONObject(5).put("value", "b".repeat(64))
                "callback" -> json.getJSONArray("fields").getJSONObject(2).put("value", "https://untrusted.example/return")
                "duplicate" -> json.getJSONArray("fields").put(json.getJSONArray("fields").getJSONObject(0))
                "passphrase" -> json.getJSONArray("fields").put(JSONObject().put("key", "passphrase").put("value", "secret"))
            }
            assertThrows(IllegalArgumentException::class.java) { PayFastForm.parse(json, order(), "https://api.example.com") }
        }
    }

    @Test fun blocksLookalikeHostsAndUnsafeNavigation() {
        for (url in listOf("http://sandbox.payfast.co.za", "https://sandbox.payfast.co.za.evil.example",
            "https://sandbox.payfast.co.za@evil.example", "javascript:alert(1)", "file:///etc/file",
            "https://sandbox.payfast.co.za:444/", "https://api.example.com.evil.example/api/payments/payfast/return")) {
            assertEquals(url, PaymentNavigation.BLOCK, form().navigation(url))
        }
        assertEquals(PaymentNavigation.PAYFAST, form().navigation("https://sandbox.payfast.co.za/eng/process"))
        assertEquals(PaymentNavigation.RETURN, form().navigation("https://api.example.com/api/payments/payfast/return?status=paid"))
        assertEquals(PaymentNavigation.CANCEL, form().navigation("https://api.example.com/api/payments/payfast/cancel"))
    }

    @Test fun requestsFormUsingAuthenticatedPost() = runBlocking {
        val server = MockWebServer(); server.start()
        val client = OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS).build()
        try {
            server.enqueue(MockResponse().setBody(formJson().toString()))
            val api = OrderApi(client, server.url("/").toString(), "https://api.example.com")
            assertEquals(form(), api.paymentForm("test-token", order()))
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/orders/${order().id}/payment", request.path)
            assertEquals("Bearer test-token", request.getHeader("Authorization"))
            assertEquals("", request.body.readUtf8())
        } finally { server.shutdown(); client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll() }
    }

    @Test fun onlyVerifiedSandboxMetadataCanShowSuccess() {
        assertTrue(paid().sandboxPaymentConfirmed)
        assertFalse(order().sandboxPaymentConfirmed)
        assertFalse(paid().copy(paymentEnvironment = "live").sandboxPaymentConfirmed)
        assertFalse(paid().copy(paymentId = "").sandboxPaymentConfirmed)
        assertFalse(paid().copy(paidAtUtc = "invalid").sandboxPaymentConfirmed)
        assertFalse(paid().copy(status = "paid").sandboxPaymentConfirmed)
    }

    @Test fun readsVerifiedPaymentFieldsFromOrderResponse() = runBlocking {
        val server = MockWebServer(); server.start()
        val client = OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS).build()
        try {
            server.enqueue(MockResponse().setBody("""{
                "id":"${order().id}","currency":"ZAR","status":"sandbox_paid","paymentMethod":"CARD",
                "items":[{"productId":"gold","name":"Gold","quantity":1,"unitPriceCents":10000,"lineTotalCents":10000}],
                "subtotalCents":10000,"deliveryFeeCents":15000,"totalCents":25000,
                "delivery":{"fullName":"Test","phone":"0821234567","streetAddress":"1 Test Road",
                "city":"Johannesburg","province":"Gauteng","postalCode":"2000"},
                "paymentEnvironment":"sandbox","payFastPaymentId":"12345","paidAtUtc":"2026-10-01T00:00:00Z"
            }"""))
            val result = OrderApi(client, server.url("/").toString()).get("token", order().id)
            assertTrue(result.sandboxPaymentConfirmed)
            assertEquals("12345", result.paymentId)
        } finally { server.shutdown(); client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll() }
    }

    @Test fun doesNotStartAnotherPaymentForAnAlreadyPaidOrder() = runBlocking {
        var started = false
        val session = PaymentSession(null, { started = true; form() }, { paid() }, {})
        session.start(paid())
        assertFalse(started)
        assertNull(session.state.value.form)
        assertNotNull(session.state.value.error)
    }

    @Test fun closingBrowserLeavesPendingUntilServerConfirms() = runBlocking {
        var savedId: String? = null
        var result = order()
        val session = PaymentSession(null, { form() }, { result }, { savedId = it })
        session.start(order())
        assertEquals(order().id, savedId)
        assertNotNull(session.state.value.form)
        session.check()
        assertNull(session.state.value.form)
        assertFalse(session.state.value.order!!.sandboxPaymentConfirmed)
        result = paid()
        session.check()
        assertTrue(session.state.value.order!!.sandboxPaymentConfirmed)
    }

    @Test fun statusFailureDoesNotShowPreviousSuccess() = runBlocking {
        var fail = false
        val session = PaymentSession(order().id, { form() }, { if (fail) throw IOException() else paid() }, {})
        session.check()
        assertTrue(session.state.value.order!!.sandboxPaymentConfirmed)
        fail = true
        session.check()
        assertNull(session.state.value.order)
        assertNotNull(session.state.value.error)
        assertEquals(order().id, session.state.value.orderId)
    }

    @Test fun restoredSessionChecksOrderWithoutStartingAnotherPayment() = runBlocking {
        var started = false
        val session = PaymentSession(order().id, { started = true; form() }, { paid() }, {})
        session.check()
        assertFalse(started)
        assertTrue(session.state.value.order!!.sandboxPaymentConfirmed)
    }

    @Test fun ignoresRepeatedPaymentTapWhileLoading() = runBlocking {
        var count = 0
        val ready = CompletableDeferred<Unit>()
        val session = PaymentSession(null, { count++; ready.await(); form() }, { order() }, {})
        val first = launch(start = CoroutineStart.UNDISPATCHED) { session.start(order()) }
        session.start(order())
        assertEquals(1, count)
        ready.complete(Unit); first.join()
    }

    @Test fun refusesStatusForADifferentOrderOrIncompleteConfirmation() = runBlocking {
        for (result in listOf(paid().copy(id = "b".repeat(64)), paid().copy(paymentId = ""))) {
            val session = PaymentSession(order().id, { form() }, { result }, {})
            session.check()
            assertNull(session.state.value.order)
            assertNotNull(session.state.value.error)
        }
    }
}
