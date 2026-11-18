package razek.fuchoquiniela.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import razek.fuchoquiniela.BuildConfig
import razek.fuchoquiniela.R
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val SITE_URL = "https://www.fuchoquiniela.com"

/** Failures of the mobile API. [describe] turns them into the text shown to the user. */
sealed class ApiError : Exception() {
    class Network(override val cause: Throwable) : ApiError()

    /** 401: bad credentials on login, or an expired/revoked token elsewhere. */
    class Unauthorized(val serverMessage: String?, val duringLogin: Boolean) : ApiError()
    class Server(val status: Int, val serverMessage: String?) : ApiError()
    class Decoding(override val cause: Throwable) : ApiError()

    fun describe(context: Context): String = when (this) {
        is Network -> context.getString(R.string.error_network)
        is Decoding -> context.getString(R.string.error_decoding)
        is Unauthorized -> serverMessage
            ?: context.getString(if (duringLogin) R.string.error_bad_credentials else R.string.error_session_expired)
        is Server -> when {
            status == 429 -> context.getString(R.string.error_too_many)
            serverMessage != null -> serverMessage
            else -> context.getString(R.string.error_status, status)
        }
    }
}

/**
 * Client for the fuchoquiniela.com mobile API (/api/v1, Laravel Sanctum bearer tokens).
 * Same contract as the iOS app's FuchoApi; the server owns the rules (locking and scoring).
 */
object FuchoApi {
    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        namingStrategy = JsonNamingStrategy.SnakeCase
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    /**
     * Names the app on every request. Some hosts refuse generic clients: Wikimedia, where most
     * news images live, answers 403 to OkHttp's default "okhttp/x.y" (its User-Agent policy).
     */
    val userAgent: String =
        "Fuchoquiniela/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE}; +$SITE_URL)"

    /** Shared by the API and the image loader (see FuchoApp). */
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build())
        }
        .build()

    private val client = httpClient.newBuilder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json".toMediaType()

    // Endpoints

    suspend fun login(email: String, password: String): LoginResponse {
        val body = mapOf("email" to email, "password" to password, "device_name" to deviceName)
        return send("POST", "auth/login", body = json.encodeToString(body), authenticated = false)
    }

    suspend fun logout() {
        sendRaw("POST", "auth/logout")
    }

    suspend fun me(): UserResponse = send("GET", "me")
    suspend fun pools(): PoolsResponse = send("GET", "pools")
    suspend fun pool(uuid: String): PoolResponse = send("GET", "pools/$uuid")
    suspend fun joinPool(uuid: String): PoolResponse = send("POST", "pools/$uuid/join")

    /** `filter`: "all" (whole season) or "open" (only matches that can still be picked). */
    suspend fun matches(poolUuid: String, filter: String = "all"): MatchesResponse =
        send("GET", "pools/$poolUuid/matches", query = mapOf("filter" to filter))

    suspend fun savePicks(poolUuid: String, picks: List<PickInput>): SavePicksResponse =
        send("PUT", "pools/$poolUuid/picks", body = json.encodeToString(SavePicksRequest(picks)))

    suspend fun positions(poolUuid: String): PositionsResponse = send("GET", "pools/$poolUuid/positions")
    suspend fun participants(poolUuid: String): ParticipantsResponse = send("GET", "pools/$poolUuid/participants")
    suspend fun rounds(poolUuid: String): RoundsResponse = send("GET", "pools/$poolUuid/rounds")

    /** Everyone's picks for a round; other participants' picks only for matches that already locked. */
    suspend fun roundDetail(poolUuid: String, roundId: Int): RoundDetailResponse =
        send("GET", "pools/$poolUuid/rounds/$roundId")

    // Public sections: the current Liga MX table, the Liguilla simulator, matches and news.

    suspend fun standings(): StandingsResponse = send("GET", "standings")
    suspend fun simulator(): SimulatorResponse = send("GET", "simulator")

    /** `status`: upcoming (soonest first), results (latest first) or all. 30 per page. */
    suspend fun leagueMatches(status: String, page: Int): LeagueMatchesResponse =
        send("GET", "matches", query = mapOf("status" to status, "page" to page.toString()))

    suspend fun posts(page: Int): PostsResponse = send("GET", "posts", query = mapOf("page" to page.toString()))
    suspend fun post(id: Int): PostResponse = send("GET", "posts/$id")

    // Transport

    private val deviceName: String
        get() = "Android ${Build.VERSION.RELEASE} - ${Build.MANUFACTURER} ${Build.MODEL}"

    private suspend inline fun <reified T> send(
        method: String,
        path: String,
        query: Map<String, String> = emptyMap(),
        body: String? = null,
        authenticated: Boolean = true,
    ): T {
        val text = sendRaw(method, path, query, body, authenticated)
        return try {
            json.decodeFromString<T>(text)
        } catch (e: Exception) {
            throw ApiError.Decoding(e)
        }
    }

    private suspend fun sendRaw(
        method: String,
        path: String,
        query: Map<String, String> = emptyMap(),
        body: String? = null,
        authenticated: Boolean = true,
    ): String {
        val url = "$baseUrl/$path".toHttpUrl().newBuilder().apply {
            query.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()

        val requestBody = when {
            body != null -> body.toRequestBody(jsonType)
            method == "GET" -> null
            else -> "{}".toRequestBody(jsonType)
        }
        val request = Request.Builder()
            .url(url)
            .method(method, requestBody)
            .header("Accept", "application/json")
            .header("Accept-Language", AppLocale.languageCode())
            .apply {
                val token = Session.token
                if (authenticated && token != null) header("Authorization", "Bearer $token")
            }
            .build()

        val (status, text) = withContext(Dispatchers.IO) {
            try {
                client.newCall(request).await().use { it.code to (it.body?.string() ?: "") }
            } catch (e: IOException) {
                throw ApiError.Network(e)
            }
        }

        val message = runCatching { json.decodeFromString<ApiErrorBody>(text).message }.getOrNull()
        return when (status) {
            in 200..299 -> text
            401 -> {
                if (authenticated) Session.expire()
                throw ApiError.Unauthorized(message, duringLogin = !authenticated)
            }
            else -> throw ApiError.Server(status, message)
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) = continuation.resume(response)
            override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
        })
        continuation.invokeOnCancellation { runCatching { cancel() } }
    }
}

private val mysqlFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/**
 * Accepts ISO 8601 with "Z" or an offset, with or without fractional seconds (Laravel's default
 * JSON dates carry microseconds), and plain MySQL "yyyy-MM-dd HH:mm:ss" in UTC.
 */
fun parseDate(text: String): Instant? =
    runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(text, mysqlFormat).toInstant(ZoneOffset.UTC) }.getOrNull()
