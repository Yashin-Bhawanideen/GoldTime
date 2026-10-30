package com.goldtime.app.ui.checkout

import com.goldtime.app.data.OrderApi
import com.goldtime.app.data.OrderApiException
import com.goldtime.app.data.OrderDraft
import com.goldtime.app.data.SavedOrder
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class OrderConnectionTest {
    private fun draft() = OrderDraft(OrderReview(listOf(CheckoutItem("gold-bar", "Gold bar", 2,
        BigDecimal("10.00"))), BigDecimal("1.00")),
        DeliveryDetails("Test Customer", "0821234567", "10 Test Road", "Johannesburg", "Gauteng", "2000"),
        PaymentMethod.CARD)

    private fun response() = """{
        "id":"${"a".repeat(64)}","currency":"ZAR","status":"pending_payment","paymentMethod":"CARD",
        "items":[{"productId":"gold-bar","name":"Gold bar","quantity":2,"unitPriceCents":1234,"lineTotalCents":2468}],
        "subtotalCents":2468,"deliveryFeeCents":15000,"totalCents":17468,
        "delivery":{"fullName":"Test Customer","phone":"0821234567","streetAddress":"10 Test Road",
        "city":"Johannesburg","province":"Gauteng","postalCode":"2000"}
    }"""

    private suspend fun withServer(block: suspend (MockWebServer, OrderApi) -> Unit) {
        val server = MockWebServer()
        val client = OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS).build()
        server.start()
        try { block(server, OrderApi(client, server.url("/").toString())) }
        finally { server.shutdown(); client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll() }
    }

    @Test fun sendsTokenAndPriceFreeRequestAndUsesServerTotal() = runBlocking {
        withServer { server, api ->
            server.enqueue(MockResponse().setBody(response()))
            val result = api.create("test-token", draft(), "test-request-id")
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/orders", request.path)
            assertEquals("Bearer test-token", request.getHeader("Authorization"))
            val json = JSONObject(request.body.readUtf8())
            assertEquals("test-request-id", json.getString("requestId"))
            assertFalse(json.has("totalCents"))
            assertFalse(json.has("userId"))
            assertFalse(json.has("status"))
            assertEquals(setOf("productId", "quantity"), json.getJSONArray("items").getJSONObject(0).keySet())
            assertEquals(BigDecimal("174.68"), result.order.total)
            assertEquals("pending_payment", result.status)
        }
    }

    @Test fun retrievesOrderUsingAuthenticatedGet() = runBlocking {
        withServer { server, api ->
            server.enqueue(MockResponse().setBody(response()))
            api.get("test-token", "a".repeat(64))
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/orders/${"a".repeat(64)}", request.path)
            assertEquals("Bearer test-token", request.getHeader("Authorization"))
        }
    }

    @Test fun handlesServerErrorsWithoutShowingRawResponse() = runBlocking {
        withServer { server, api ->
            for (code in listOf(400, 401, 403, 404, 409, 500, 503)) {
                server.enqueue(MockResponse().setResponseCode(code).setBody("private server exception"))
                try { api.create("token", draft(), "id"); fail("Expected an API error") }
                catch (ex: OrderApiException) {
                    assertEquals(code, ex.statusCode)
                    assertFalse(ex.message.orEmpty().contains("private"))
                }
            }
        }
    }

    @Test fun rejectsIncompleteOrInconsistentResponses() = runBlocking {
        withServer { server, api ->
            for (body in listOf("{}", "not JSON", response().replace("17468", "1"),
                response().replace("ZAR", "USD"), response().replace("2468", "2469"))) {
                server.enqueue(MockResponse().setBody(body))
                try { api.create("token", draft(), "id"); fail("Expected a response error") }
                catch (ex: IOException) { assertTrue(ex.message.orEmpty().contains("incomplete order")) }
            }
        }
    }

    @Test fun fingerprintIgnoresDisplayPricesButDetectsChangedDetails() {
        val original = draft()
        val changedPrice = original.copy(order = original.order.copy(items = original.order.items.map {
            it.copy(unitPrice = BigDecimal("999.00"))
        }))
        assertEquals(original.fingerprint(), changedPrice.fingerprint())
        assertNotEquals(original.fingerprint(), original.copy(method = PaymentMethod.INSTANT_EFT).fingerprint())
        assertNotEquals(original.fingerprint(), original.copy(delivery = original.delivery.copy(city = "Cape Town")).fingerprint())
    }

    @Test fun cancellingRequestStopsWaitingForTheServer() = runBlocking {
        withServer { server, api ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val job = launch { api.create("token", draft(), "id") }
            withContext(Dispatchers.IO) {
                assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            }
            withTimeout(2000) { job.cancelAndJoin() }
            assertTrue(job.isCancelled)
        }
    }

    private fun saved(draft: OrderDraft) = SavedOrder("a".repeat(64), draft.order, draft.delivery, draft.method, "pending_payment")

    @Test fun retryAndRestoredSubmissionReuseRequestId() = runBlocking {
        var stored: List<String>? = null
        val ids = mutableListOf<String>()
        val first = OrderSubmission({ _, id -> ids += id; throw IOException("lost response") }, { stored }, { stored = it })
        first.submit(draft())
        assertNotNull(first.state.value.error)
        assertFalse(first.state.value.loading)
        val restored = OrderSubmission({ d, id -> ids += id; saved(d) }, { stored }, { stored = it })
        restored.submit(draft())
        assertEquals(ids[0], ids[1])
        assertNotNull(restored.state.value.savedOrder)
        restored.edit()
        restored.submit(draft().copy(method = PaymentMethod.INSTANT_EFT))
        assertNotEquals(ids[1], ids[2])
    }

    @Test fun doubleTapOnlySubmitsOnce() = runBlocking {
        var count = 0
        val gate = CompletableDeferred<Unit>()
        val submission = OrderSubmission({ d, _ -> count++; gate.await(); saved(d) }, { null }, {})
        val first = launch(start = CoroutineStart.UNDISPATCHED) { submission.submit(draft()) }
        assertTrue(submission.state.value.loading)
        submission.submit(draft())
        assertEquals(1, count)
        gate.complete(Unit); first.join()
        assertFalse(submission.state.value.loading)
    }

    @Test fun invalidCheckoutNeverCallsApi() = runBlocking {
        var called = false
        val submission = OrderSubmission({ d, _ -> called = true; saved(d) }, { null }, {})
        submission.submit(draft().copy(delivery = DeliveryDetails()))
        assertFalse(called)
        assertNotNull(submission.state.value.error)
    }

    @Test fun cancellationReleasesSubmissionAndKeepsRetryIdentity() = runBlocking {
        var stored: List<String>? = null
        val submission = OrderSubmission({ _, _ -> awaitCancellation() }, { stored }, { stored = it })
        val job = launch(start = CoroutineStart.UNDISPATCHED) { submission.submit(draft()) }
        job.cancelAndJoin()
        assertFalse(submission.state.value.loading)
        assertNull(submission.state.value.error)
        assertNotNull(stored)
    }

    @Test fun rejectsOrderForDifferentItems() = runBlocking {
        val submission = OrderSubmission({ d, _ -> saved(d).copy(order = OrderReview(listOf(
            CheckoutItem("different", "Other product", 1, BigDecimal.ONE)), BigDecimal.ZERO)) }, { null }, {})
        submission.submit(draft())
        assertNull(submission.state.value.savedOrder)
        assertNotNull(submission.state.value.error)
    }
}
