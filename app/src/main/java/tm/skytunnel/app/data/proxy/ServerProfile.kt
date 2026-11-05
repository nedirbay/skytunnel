package tm.skytunnel.app.data.proxy

import org.json.JSONObject
import java.security.MessageDigest

enum class ProxyProtocol(val label: String) {
    VLESS("VLESS"),
    VMESS("VMess"),
    TROJAN("Trojan"),
    SHADOWSOCKS("Shadowsocks"),
    UNSUPPORTED("Unsupported")
}

/**
 * One server from a subscription or share link. [credential] is the VLESS/VMess
 * UUID or the Trojan/Shadowsocks password, so never log a profile.
 */
data class ServerProfile(
    val name: String,
    val address: String,
    val port: Int,
    val protocol: ProxyProtocol,
    val credential: String = "",
    /** VLESS encryption, VMess security, or Shadowsocks method. */
    val method: String = "",
    val flow: String = "",
    val network: String = "tcp",
    val security: String = "none",
    val sni: String = "",
    val fingerprint: String = "",
    val alpn: String = "",
    val publicKey: String = "",
    val shortId: String = "",
    val spiderX: String = "",
    val path: String = "",
    val host: String = "",
    val serviceName: String = "",
    val mode: String = "",
    val headerType: String = "",
    /** Original scheme of an unsupported link, for example "hysteria2". */
    val unsupportedScheme: String = ""
) {
    val isSupported: Boolean get() = protocol != ProxyProtocol.UNSUPPORTED

    val protocolLabel: String
        get() = when {
            !isSupported -> unsupportedScheme.ifEmpty { protocol.label }
            security == "reality" -> "${protocol.label} Reality"
            else -> protocol.label
        }

    /** Stays the same when the panel renames a server, so a selection survives a refresh. */
    val id: String
        get() {
            val key = listOf(
                protocol, address, port, credential, method, flow, network, security,
                sni, publicKey, shortId, path, host, serviceName, unsupportedScheme
            ).joinToString("|")
            return MessageDigest.getInstance("SHA-256")
                .digest(key.toByteArray())
                .take(12)
                .joinToString("") { "%02x".format(it) }
        }

    fun toJson(): JSONObject = JSONObject()
        .put("name", name).put("address", address).put("port", port)
        .put("protocol", protocol.name).put("credential", credential)
        .put("method", method).put("flow", flow).put("network", network)
        .put("security", security).put("sni", sni).put("fingerprint", fingerprint)
        .put("alpn", alpn).put("publicKey", publicKey).put("shortId", shortId)
        .put("spiderX", spiderX).put("path", path).put("host", host)
        .put("serviceName", serviceName).put("mode", mode)
        .put("headerType", headerType).put("unsupportedScheme", unsupportedScheme)

    companion object {
        fun fromJson(o: JSONObject) = ServerProfile(
            name = o.optString("name"),
            address = o.optString("address"),
            port = o.optInt("port"),
            protocol = ProxyProtocol.valueOf(o.optString("protocol")),
            credential = o.optString("credential"),
            method = o.optString("method"),
            flow = o.optString("flow"),
            network = o.optString("network"),
            security = o.optString("security"),
            sni = o.optString("sni"),
            fingerprint = o.optString("fingerprint"),
            alpn = o.optString("alpn"),
            publicKey = o.optString("publicKey"),
            shortId = o.optString("shortId"),
            spiderX = o.optString("spiderX"),
            path = o.optString("path"),
            host = o.optString("host"),
            serviceName = o.optString("serviceName"),
            mode = o.optString("mode"),
            headerType = o.optString("headerType"),
            unsupportedScheme = o.optString("unsupportedScheme")
        )
    }
}
