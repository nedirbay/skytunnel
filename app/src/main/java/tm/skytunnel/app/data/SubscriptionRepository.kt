package tm.skytunnel.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import tm.skytunnel.app.data.proxy.ServerProfile
import tm.skytunnel.app.data.proxy.ShareLinkParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Traffic limits a panel reports in the `subscription-userinfo` header. */
data class SubscriptionUsage(
    val usedBytes: Long,
    val totalBytes: Long,
    val expiresAtEpochSeconds: Long
)

data class SubscriptionState(
    val title: String = "",
    val url: String = "",
    val usage: SubscriptionUsage? = null,
    val profiles: List<ServerProfile> = emptyList(),
    val selectedId: String? = null
) {
    val selected: ServerProfile? get() = profiles.firstOrNull { it.id == selectedId }
}

class SubscriptionException(message: String) : Exception(message)

/** Holds the servers from one subscription URL or from pasted share links. */
class SubscriptionRepository(context: Context) {

    private val store = SecureStore(context, "subscription.enc")
    private val _state = MutableStateFlow(load())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    /** Accepts an `https://` subscription URL or one or more share links. */
    suspend fun add(input: String) {
        val text = input.trim()
        when {
            text.startsWith("happ://", ignoreCase = true) -> throw SubscriptionException(
                "Encrypted happ:// links are not supported. Copy the plain https:// link from your panel."
            )
            text.startsWith("http://", ignoreCase = true) -> throw SubscriptionException(
                "Use an https:// link. A plain http:// link would expose your subscription."
            )
            text.startsWith("https://", ignoreCase = true) -> fetchAndStore(text)
            else -> {
                val profiles = ShareLinkParser.parseBody(text)
                if (profiles.isEmpty()) throw SubscriptionException("No valid server link found")
                publish(SubscriptionState(title = "Manual", profiles = profiles))
            }
        }
    }

    suspend fun refresh() {
        val url = _state.value.url
        if (url.isEmpty()) throw SubscriptionException("This list has no link to refresh")
        fetchAndStore(url)
    }

    fun select(id: String) = updateAndSave { it.copy(selectedId = id) }

    fun remove() {
        store.delete()
        _state.value = SubscriptionState()
    }

    private suspend fun fetchAndStore(url: String) {
        val current = _state.value
        val fetched = withContext(Dispatchers.IO) { fetch(url) }
        publish(
            SubscriptionState(
                title = fetched.title,
                url = url,
                usage = fetched.usage,
                profiles = fetched.profiles,
                selectedId = current.selectedId
            )
        )
    }

    private fun publish(next: SubscriptionState) {
        val selected = next.selectedId?.takeIf { id -> next.profiles.any { it.id == id } }
            ?: next.profiles.firstOrNull { it.isSupported }?.id
        updateAndSave { next.copy(selectedId = selected) }
    }

    private fun updateAndSave(change: (SubscriptionState) -> SubscriptionState) {
        _state.update(change)
        store.write(serialize(_state.value))
    }

    private fun fetch(url: String): Fetched {
        // Panels choose a format from the User-Agent, so fall back to a widely served one.
        val first = request(url, "SkyTunnel/${USER_AGENT_VERSION}")
        if (first.profiles.isNotEmpty()) return first
        val second = request(url, "v2rayNG/1.10.0")
        if (second.profiles.isNotEmpty()) return second
        throw SubscriptionException("The subscription has no servers in a supported format")
    }

    private fun request(url: String, userAgent: String): Fetched {
        val connection = try {
            URL(url).openConnection() as? HttpsURLConnection
        } catch (_: IOException) {
            null
        } ?: throw SubscriptionException("Invalid subscription link")
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", userAgent)
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw SubscriptionException("The panel answered with error $code")
            }
            val body = connection.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    if (out.size() > MAX_BODY_BYTES) throw SubscriptionException("The subscription is too large")
                }
                out.toString("UTF-8")
            }
            return Fetched(
                title = parseTitle(connection.getHeaderField("profile-title"))
                    .ifEmpty { connection.url.host },
                usage = parseUsage(connection.getHeaderField("subscription-userinfo")),
                profiles = ShareLinkParser.parseBody(body)
            )
        } catch (e: IOException) {
            throw SubscriptionException("Could not reach the panel: ${e.javaClass.simpleName}")
        } finally {
            connection.disconnect()
        }
    }

    private fun load(): SubscriptionState {
        val text = store.read() ?: return SubscriptionState()
        return try {
            deserialize(text)
        } catch (_: JSONException) {
            SubscriptionState()
        }
    }

    private class Fetched(
        val title: String,
        val usage: SubscriptionUsage?,
        val profiles: List<ServerProfile>
    )

    companion object {
        private const val TIMEOUT_MS = 20_000
        private const val MAX_BODY_BYTES = 4 * 1024 * 1024
        private const val USER_AGENT_VERSION = "1.0"

        internal fun parseTitle(header: String?): String {
            val value = header?.trim().orEmpty()
            return if (value.startsWith("base64:")) {
                ShareLinkParser.decodeBase64(value.removePrefix("base64:")).orEmpty().trim()
            } else {
                value
            }
        }

        internal fun parseUsage(header: String?): SubscriptionUsage? {
            if (header.isNullOrBlank()) return null
            val values = header.split(';').associate {
                it.substringBefore('=').trim() to (it.substringAfter('=', "").trim().toLongOrNull() ?: 0L)
            }
            return SubscriptionUsage(
                usedBytes = values["upload"].orZero() + values["download"].orZero(),
                totalBytes = values["total"].orZero(),
                expiresAtEpochSeconds = values["expire"].orZero()
            )
        }

        private fun Long?.orZero() = this ?: 0L

        internal fun serialize(s: SubscriptionState): String = JSONObject()
            .put("title", s.title)
            .put("url", s.url)
            .put("selectedId", s.selectedId ?: "")
            .put("used", s.usage?.usedBytes ?: -1L)
            .put("total", s.usage?.totalBytes ?: 0L)
            .put("expire", s.usage?.expiresAtEpochSeconds ?: 0L)
            .put("profiles", JSONArray().apply { s.profiles.forEach { put(it.toJson()) } })
            .toString()

        internal fun deserialize(text: String): SubscriptionState {
            val o = JSONObject(text)
            val array = o.getJSONArray("profiles")
            val used = o.optLong("used", -1L)
            return SubscriptionState(
                title = o.optString("title"),
                url = o.optString("url"),
                usage = if (used < 0) null else SubscriptionUsage(used, o.optLong("total"), o.optLong("expire")),
                profiles = (0 until array.length()).map { ServerProfile.fromJson(array.getJSONObject(it)) },
                selectedId = o.optString("selectedId").ifEmpty { null }
            )
        }
    }
}
