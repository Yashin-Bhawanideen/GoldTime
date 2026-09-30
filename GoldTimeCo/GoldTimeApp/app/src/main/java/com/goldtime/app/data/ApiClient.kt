package com.goldtime.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Talks to the ASP.NET Core API. Every call sends the Firebase ID token as a Bearer token. */
object ApiClient {


    // Emulator -> your PC's localhost:5000. On a physical phone use your PC's LAN IP,
    // e.g. "http://192.168.1.20:5000/". For the hosted API use "https://YOUR_PROJECT_ID.web.app/".
    private const val API_BASE_URL = "https://goldtime-hnh4bpa5hshrgsa6.southafricanorth-01.azurewebsites.net" //azure api hosted

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private val orders = OrderApi(client, API_BASE_URL)

    suspend fun createOrder(idToken: String, draft: OrderDraft, requestId: String): SavedOrder =
        orders.create(idToken, draft, requestId)

    suspend fun getOrder(idToken: String, orderId: String): SavedOrder = orders.get(idToken, orderId)

    private fun url(path: String): String =
        API_BASE_URL.trimEnd('/') + "/" + path.trimStart('/')

    suspend fun getHome(idToken: String): HomeData = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url("api/home"))
            .header("Authorization", "Bearer $idToken")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException("Server error (${response.code})")
            parseHome(JSONObject(body))
        }
    }

    suspend fun registerProfile(
        idToken: String,
        firstName: String,
        surname: String,
        email: String,
        phone: String
    ) = withContext(Dispatchers.IO) {
        val json = JSONObject()
            .put("firstName", firstName)
            .put("surname", surname)
            .put("email", email)
            .put("phone", phone)

        val request = Request.Builder()
            .url(url("api/auth/register"))
            .header("Authorization", "Bearer $idToken")
            .post(json.toString().toRequestBody(jsonType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ApiException("Could not save your profile (${response.code}). Please try again.")
            }
        }
    }

    private fun parseHome(root: JSONObject): HomeData {
        val hero = root.getJSONObject("hero")
        val list = root.getJSONArray("featured")
        val featured = (0 until list.length()).map { i ->
            val o = list.getJSONObject(i)
            FeaturedAsset(
                id = o.getString("id"),
                name = o.getString("name"),
                cta = o.optString("cta", "Get Quote"),
                imageUrl = o.optImage("imageUrl")
            )
        }
        return HomeData(
            hero = HeroContent(
                badge = hero.getString("badge"),
                title = hero.getString("title"),
                subtitle = hero.getString("subtitle"),
                imageUrl = hero.optImage("imageUrl")
            ),
            featured = featured
        )
    }

    private fun JSONObject.optImage(key: String): String? =
        if (isNull(key)) null else getString(key).takeIf { it.isNotBlank() }
}
