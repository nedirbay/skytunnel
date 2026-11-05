package tm.skytunnel.app.data.proxy

import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder
import java.util.Base64

/** Turns share links and subscription bodies into [ServerProfile]s. */
object ShareLinkParser {

    private val SUPPORTED_NETWORKS = setOf("tcp", "ws", "grpc", "httpupgrade", "xhttp")
    private val KNOWN_UNSUPPORTED = setOf("hysteria2", "hy2", "hysteria", "tuic", "wireguard", "wg", "socks")

    /** Parses a whole subscription body: plain links, or base64 of plain links. */
    fun parseBody(body: String): List<ServerProfile> {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return emptyList()
        val text = if ("://" in trimmed) trimmed else decodeBase64(trimmed) ?: return emptyList()
        return text.lineSequence().mapNotNull { parseLink(it) }.toList()
    }

    /** Returns null when [raw] is malformed or is not a server link. */
    fun parseLink(raw: String): ServerProfile? {
        val link = raw.trim()
        val scheme = link.substringBefore("://", "").lowercase()
        return try {
            when (scheme) {
                "vless" -> parseVless(link)
                "vmess" -> parseVmess(link)
                "trojan" -> parseTrojan(link)
                "ss" -> parseShadowsocks(link)
                in KNOWN_UNSUPPORTED -> parseUnsupported(link, scheme)
                else -> null
            }
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: JSONException) {
            null
        }
    }

    private fun parseVless(link: String): ServerProfile? {
        val url = ParsedUrl.of(link) ?: return null
        if (url.user.isEmpty()) return null
        return withTransport(
            url,
            ServerProfile(
                name = url.displayName(),
                address = url.host,
                port = url.port,
                protocol = ProxyProtocol.VLESS,
                credential = url.user,
                method = url.query["encryption"].orEmpty().ifEmpty { "none" },
                flow = url.query["flow"].orEmpty()
            ),
            defaultSecurity = "none"
        )
    }

    private fun parseTrojan(link: String): ServerProfile? {
        val url = ParsedUrl.of(link) ?: return null
        if (url.user.isEmpty()) return null
        return withTransport(
            url,
            ServerProfile(
                name = url.displayName(),
                address = url.host,
                port = url.port,
                protocol = ProxyProtocol.TROJAN,
                credential = url.user,
                flow = url.query["flow"].orEmpty()
            ),
            defaultSecurity = "tls"
        )
    }

    private fun withTransport(url: ParsedUrl, base: ServerProfile, defaultSecurity: String): ServerProfile? {
        val q = url.query
        val network = q["type"].orEmpty().ifEmpty { "tcp" }.let { if (it == "raw") "tcp" else it }
        if (network !in SUPPORTED_NETWORKS) return null
        val security = q["security"].orEmpty().ifEmpty { defaultSecurity }
        if (security !in setOf("none", "tls", "reality")) return null
        val publicKey = q["pbk"].orEmpty()
        val sni = q["sni"].orEmpty().ifEmpty { q["peer"].orEmpty() }
        if (security == "reality" && (publicKey.isEmpty() || sni.isEmpty())) return null
        return base.copy(
            network = network,
            security = security,
            sni = sni,
            fingerprint = q["fp"].orEmpty(),
            alpn = q["alpn"].orEmpty(),
            publicKey = publicKey,
            shortId = q["sid"].orEmpty(),
            spiderX = q["spx"].orEmpty(),
            path = q["path"].orEmpty(),
            host = q["host"].orEmpty(),
            serviceName = q["serviceName"].orEmpty(),
            mode = q["mode"].orEmpty(),
            headerType = q["headerType"].orEmpty()
        )
    }

    private fun parseVmess(link: String): ServerProfile? {
        val json = decodeBase64(link.substringAfter("://")) ?: return null
        val o = JSONObject(json)
        val address = o.optString("add")
        val port = o.optString("port").toIntOrNull() ?: return null
        val id = o.optString("id")
        if (address.isEmpty() || id.isEmpty() || port !in 1..65535) return null
        val network = o.optString("net").ifEmpty { "tcp" }.let { if (it == "raw") "tcp" else it }
        if (network !in SUPPORTED_NETWORKS) return null
        return ServerProfile(
            name = o.optString("ps").ifEmpty { address },
            address = address,
            port = port,
            protocol = ProxyProtocol.VMESS,
            credential = id,
            method = o.optString("scy").ifEmpty { "auto" },
            network = network,
            security = if (o.optString("tls") == "tls") "tls" else "none",
            sni = o.optString("sni"),
            fingerprint = o.optString("fp"),
            alpn = o.optString("alpn"),
            path = o.optString("path"),
            host = o.optString("host"),
            serviceName = if (network == "grpc") o.optString("path") else "",
            mode = if (network == "grpc" || network == "xhttp") o.optString("type") else "",
            headerType = if (network == "tcp") o.optString("type") else ""
        )
    }

    private fun parseShadowsocks(link: String): ServerProfile? {
        val fragment = link.substringAfter('#', "")
        val noFragment = link.substringBefore('#')
        val rest = noFragment.substringAfter("://")
        val noQuery = rest.substringBefore('?')
        // Legacy form: ss://base64(method:password@host:port)
        val decodedWhole = if ('@' !in noQuery) decodeBase64(noQuery) else null
        val full = "ss://" + (decodedWhole ?: noQuery) +
            (if ('?' in rest) "?" + rest.substringAfter('?') else "") +
            (if (fragment.isNotEmpty()) "#$fragment" else "")
        val url = ParsedUrl.of(full, decodeUser = false) ?: return null
        if (url.query.containsKey("plugin")) return parseUnsupported(full, "ss+plugin")
        val rawUser = url.rawUser
        val credentials = if (decodedWhole != null) {
            rawUser
        } else if (':' in rawUser) {
            percentDecode(rawUser)
        } else {
            decodeBase64(percentDecode(rawUser)) ?: return null
        }
        val method = credentials.substringBefore(':', "")
        val password = credentials.substringAfter(':', "")
        if (method.isEmpty() || password.isEmpty()) return null
        return ServerProfile(
            name = url.displayName(),
            address = url.host,
            port = url.port,
            protocol = ProxyProtocol.SHADOWSOCKS,
            credential = password,
            method = method
        )
    }

    private fun parseUnsupported(link: String, scheme: String): ServerProfile {
        val url = ParsedUrl.of(link, decodeUser = false)
        val name = url?.displayName()
            ?: percentDecode(link.substringAfter('#', "")).ifEmpty { scheme }
        return ServerProfile(
            name = name,
            address = url?.host.orEmpty(),
            port = url?.port ?: 0,
            protocol = ProxyProtocol.UNSUPPORTED,
            unsupportedScheme = scheme
        )
    }

    /** Accepts standard and URL-safe alphabets, with or without padding. */
    internal fun decodeBase64(text: String): String? {
        val clean = text.filterNot { it.isWhitespace() }.replace('-', '+').replace('_', '/').trimEnd('=')
        val padded = clean + "=".repeat((4 - clean.length % 4) % 4)
        return try {
            String(Base64.getDecoder().decode(padded), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Unlike a form decoder, keeps a literal '+', which is common in passwords and paths. */
    internal fun percentDecode(value: String): String = try {
        URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
    } catch (_: IllegalArgumentException) {
        value
    }

    private class ParsedUrl(
        val rawUser: String,
        val user: String,
        val host: String,
        val port: Int,
        val query: Map<String, String>,
        val fragment: String
    ) {
        fun displayName() = fragment.ifEmpty { host }

        companion object {
            fun of(link: String, decodeUser: Boolean = true): ParsedUrl? {
                val fragment = percentDecode(link.substringAfter('#', "")).trim()
                val body = link.substringBefore('#').substringAfter("://")
                val queryText = body.substringAfter('?', "")
                val authority = body.substringBefore('?').substringBefore('/')
                val rawUser = authority.substringBeforeLast('@', "")
                val hostPort = authority.substringAfterLast('@')
                val host: String
                val portText: String
                if (hostPort.startsWith("[")) {
                    host = hostPort.substringAfter('[').substringBefore(']')
                    portText = hostPort.substringAfter("]:", "")
                } else {
                    host = hostPort.substringBefore(':')
                    portText = hostPort.substringAfter(':', "")
                }
                val port = portText.toIntOrNull() ?: return null
                if (host.isEmpty() || port !in 1..65535) return null
                val query = queryText.split('&').filter { it.isNotEmpty() }.associate {
                    it.substringBefore('=') to percentDecode(it.substringAfter('=', ""))
                }
                return ParsedUrl(
                    rawUser = rawUser,
                    user = if (decodeUser) percentDecode(rawUser) else rawUser,
                    host = host,
                    port = port,
                    query = query,
                    fragment = fragment
                )
            }
        }
    }
}
