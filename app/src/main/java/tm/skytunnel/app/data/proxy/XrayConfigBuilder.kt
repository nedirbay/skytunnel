package tm.skytunnel.app.data.proxy

import org.json.JSONArray
import org.json.JSONObject

/** Credentials of the loopback SOCKS inbound used only for the exit IP check. */
data class LocalSocks(val port: Int, val user: String, val password: String)

/** Builds the Xray JSON config for one server. */
object XrayConfigBuilder {

    const val PROXY_TAG = "proxy"

    private val PRIVATE_RANGES = listOf(
        "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "169.254.0.0/16", "fc00::/7", "fe80::/10"
    )

    /**
     * With [socks] null the config has no inbounds, which is what delay tests need.
     * Otherwise it reads packets from the TUN descriptor passed to `startLoop`.
     *
     * The access log stays off in every build, because it would write each site the
     * user visits to the system log. [debugLogs] only enables core warnings.
     */
    fun build(profile: ServerProfile, socks: LocalSocks? = null, debugLogs: Boolean = false): String {
        require(profile.isSupported) { "Unsupported protocol" }
        val root = JSONObject()
            .put(
                "log",
                JSONObject()
                    .put("access", "none")
                    .put("loglevel", if (debugLogs) "warning" else "none")
            )
            .put("stats", JSONObject())
            .put(
                "policy",
                JSONObject().put(
                    "system",
                    JSONObject().put("statsOutboundUplink", true).put("statsOutboundDownlink", true)
                )
            )
            .put(
                "outbounds",
                JSONArray()
                    .put(outbound(profile))
                    .put(JSONObject().put("tag", "direct").put("protocol", "freedom"))
            )
            .put(
                "routing",
                JSONObject().put("domainStrategy", "AsIs").put(
                    "rules",
                    JSONArray().put(
                        JSONObject()
                            .put("type", "field")
                            .put("ip", JSONArray(PRIVATE_RANGES))
                            .put("outboundTag", "direct")
                    )
                )
            )
        if (socks != null) root.put("inbounds", inbounds(socks))
        return root.toString()
    }

    private fun inbounds(socks: LocalSocks) = JSONArray()
        .put(
            JSONObject()
                .put("tag", "tun")
                .put("protocol", "tun")
                .put("settings", JSONObject().put("name", "tun0").put("mtu", 1500))
                .put(
                    "sniffing",
                    JSONObject()
                        .put("enabled", true)
                        .put("destOverride", JSONArray(listOf("http", "tls", "quic")))
                        .put("routeOnly", true)
                )
        )
        .put(
            JSONObject()
                .put("tag", "ipcheck")
                .put("listen", "127.0.0.1")
                .put("port", socks.port)
                .put("protocol", "socks")
                .put(
                    "settings",
                    JSONObject()
                        .put("auth", "password")
                        .put("udp", false)
                        .put(
                            "accounts",
                            JSONArray().put(
                                JSONObject().put("user", socks.user).put("pass", socks.password)
                            )
                        )
                )
        )

    private fun outbound(p: ServerProfile): JSONObject = JSONObject()
        .put("tag", PROXY_TAG)
        .put(
            "protocol",
            when (p.protocol) {
                ProxyProtocol.VLESS -> "vless"
                ProxyProtocol.VMESS -> "vmess"
                ProxyProtocol.TROJAN -> "trojan"
                ProxyProtocol.SHADOWSOCKS -> "shadowsocks"
                ProxyProtocol.UNSUPPORTED -> error("Unsupported protocol")
            }
        )
        .put("settings", settings(p))
        .put("streamSettings", stream(p))

    private fun settings(p: ServerProfile): JSONObject = when (p.protocol) {
        ProxyProtocol.VLESS -> vnext(
            p,
            JSONObject().put("id", p.credential).put("encryption", p.method.ifEmpty { "none" })
                .apply { if (p.flow.isNotEmpty()) put("flow", p.flow) }
        )
        ProxyProtocol.VMESS -> vnext(
            p,
            JSONObject().put("id", p.credential).put("security", p.method.ifEmpty { "auto" })
        )
        ProxyProtocol.TROJAN -> servers(
            JSONObject().put("address", p.address).put("port", p.port).put("password", p.credential)
                .apply { if (p.flow.isNotEmpty()) put("flow", p.flow) }
        )
        ProxyProtocol.SHADOWSOCKS -> servers(
            JSONObject().put("address", p.address).put("port", p.port)
                .put("method", p.method).put("password", p.credential)
        )
        ProxyProtocol.UNSUPPORTED -> error("Unsupported protocol")
    }

    private fun vnext(p: ServerProfile, user: JSONObject) = JSONObject().put(
        "vnext",
        JSONArray().put(
            JSONObject().put("address", p.address).put("port", p.port)
                .put("users", JSONArray().put(user))
        )
    )

    private fun servers(server: JSONObject) = JSONObject().put("servers", JSONArray().put(server))

    private fun stream(p: ServerProfile): JSONObject {
        val out = JSONObject().put("network", p.network).put("security", p.security)
        when (p.security) {
            "reality" -> out.put(
                "realitySettings",
                JSONObject()
                    .put("serverName", p.sni)
                    .put("fingerprint", p.fingerprint.ifEmpty { "chrome" })
                    .put("publicKey", p.publicKey)
                    .put("shortId", p.shortId)
                    .put("spiderX", p.spiderX)
            )
            "tls" -> out.put(
                "tlsSettings",
                JSONObject()
                    .put("serverName", p.sni.ifEmpty { p.host.ifEmpty { p.address } })
                    .apply {
                        if (p.fingerprint.isNotEmpty()) put("fingerprint", p.fingerprint)
                        if (p.alpn.isNotEmpty()) put("alpn", JSONArray(p.alpn.split(',')))
                    }
            )
        }
        when (p.network) {
            "ws" -> out.put("wsSettings", pathHost(p))
            "httpupgrade" -> out.put("httpupgradeSettings", pathHost(p))
            "xhttp" -> out.put(
                "xhttpSettings",
                pathHost(p).apply { if (p.mode.isNotEmpty()) put("mode", p.mode) }
            )
            "grpc" -> out.put(
                "grpcSettings",
                JSONObject().put("serviceName", p.serviceName).put("multiMode", p.mode == "multi")
            )
            "tcp" -> if (p.headerType == "http") {
                out.put(
                    "tcpSettings",
                    JSONObject().put(
                        "header",
                        JSONObject().put("type", "http").put(
                            "request",
                            JSONObject()
                                .put("path", JSONArray(listOf(p.path.ifEmpty { "/" })))
                                .put(
                                    "headers",
                                    JSONObject().put("Host", JSONArray(p.host.split(',').filter { it.isNotEmpty() }))
                                )
                        )
                    )
                )
            }
        }
        return out
    }

    private fun pathHost(p: ServerProfile) = JSONObject()
        .put("path", p.path.ifEmpty { "/" })
        .apply { if (p.host.isNotEmpty()) put("host", p.host) }
}
