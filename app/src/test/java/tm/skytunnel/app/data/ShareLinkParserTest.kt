package tm.skytunnel.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import tm.skytunnel.app.data.proxy.ProxyProtocol
import tm.skytunnel.app.data.proxy.ShareLinkParser
import tm.skytunnel.app.data.proxy.XrayConfigBuilder
import java.util.Base64

class ShareLinkParserTest {

    private val uuid = "11111111-2222-3333-4444-555555555555"
    private val reality = "vless://$uuid@example.com:443?type=tcp&security=reality&sni=www.example.org" +
        "&fp=chrome&pbk=PUBLICKEY&sid=abcd&spx=%2F&flow=xtls-rprx-vision#%F0%9F%87%A9%F0%9F%87%AA%20Frankfurt"

    private fun b64(text: String) = Base64.getEncoder().encodeToString(text.toByteArray())

    @Test
    fun parsesVlessReality() {
        val p = ShareLinkParser.parseLink(reality)!!
        assertEquals(ProxyProtocol.VLESS, p.protocol)
        assertEquals("example.com", p.address)
        assertEquals(443, p.port)
        assertEquals(uuid, p.credential)
        assertEquals("reality", p.security)
        assertEquals("PUBLICKEY", p.publicKey)
        assertEquals("abcd", p.shortId)
        assertEquals("/", p.spiderX)
        assertEquals("xtls-rprx-vision", p.flow)
        assertEquals("none", p.method)
        assertEquals("🇩🇪 Frankfurt", p.name)
    }

    @Test
    fun rejectsRealityWithoutPublicKey() {
        assertNull(ShareLinkParser.parseLink("vless://$uuid@example.com:443?security=reality&sni=a.org"))
    }

    @Test
    fun rejectsBadPortAndGarbage() {
        assertNull(ShareLinkParser.parseLink("vless://$uuid@example.com:99999"))
        assertNull(ShareLinkParser.parseLink("vless://$uuid@example.com"))
        assertNull(ShareLinkParser.parseLink("not a link"))
        assertNull(ShareLinkParser.parseLink("vmess://%%%"))
    }

    @Test
    fun parsesIpv6Host() {
        val p = ShareLinkParser.parseLink("vless://$uuid@[2001:db8::1]:8443?security=none#v6")!!
        assertEquals("2001:db8::1", p.address)
        assertEquals(8443, p.port)
    }

    @Test
    fun parsesVmess() {
        val json = JSONObject()
            .put("ps", "NL").put("add", "1.2.3.4").put("port", "8080").put("id", uuid)
            .put("net", "ws").put("path", "/ray").put("host", "cdn.example.com").put("tls", "tls")
            .put("sni", "cdn.example.com").put("scy", "auto")
        val p = ShareLinkParser.parseLink("vmess://" + b64(json.toString()))!!
        assertEquals(ProxyProtocol.VMESS, p.protocol)
        assertEquals(8080, p.port)
        assertEquals("ws", p.network)
        assertEquals("tls", p.security)
        assertEquals("/ray", p.path)
    }

    @Test
    fun trojanDefaultsToTls() {
        val p = ShareLinkParser.parseLink("trojan://pa%2Bss@example.com:443?sni=example.com#T")!!
        assertEquals(ProxyProtocol.TROJAN, p.protocol)
        assertEquals("tls", p.security)
        assertEquals("pa+ss", p.credential)
    }

    @Test
    fun parsesAllShadowsocksForms() {
        val sip002 = ShareLinkParser.parseLink(
            "ss://" + b64("aes-256-gcm:secret") + "@example.com:8388#One"
        )!!
        assertEquals("aes-256-gcm", sip002.method)
        assertEquals("secret", sip002.credential)
        assertEquals("One", sip002.name)

        val legacy = ShareLinkParser.parseLink(
            "ss://" + b64("aes-256-gcm:secret@example.com:8388") + "#Two"
        )!!
        assertEquals("example.com", legacy.address)
        assertEquals("secret", legacy.credential)
        assertEquals("Two", legacy.name)

        val plain = ShareLinkParser.parseLink(
            "ss://2022-blake3-aes-128-gcm:a%2Fb%3D@example.com:8388#Three"
        )!!
        assertEquals("2022-blake3-aes-128-gcm", plain.method)
        assertEquals("a/b=", plain.credential)
    }

    @Test
    fun marksHysteriaAndSsPluginUnsupported() {
        val hy = ShareLinkParser.parseLink("hysteria2://pw@example.com:443#Hy")!!
        assertFalse(hy.isSupported)
        assertEquals("Hy", hy.name)
        val plugin = ShareLinkParser.parseLink(
            "ss://" + b64("aes-256-gcm:secret") + "@example.com:8388?plugin=obfs-local#P"
        )!!
        assertFalse(plugin.isSupported)
    }

    @Test
    fun decodesBase64AndPlainSubscriptions() {
        val body = "$reality\nhysteria2://pw@example.com:443#Hy\ngarbage\n"
        assertEquals(2, ShareLinkParser.parseBody(body).size)
        assertEquals(2, ShareLinkParser.parseBody(b64(body)).size)
        val urlSafeNoPad = Base64.getUrlEncoder().withoutPadding().encodeToString(body.toByteArray())
        assertEquals(2, ShareLinkParser.parseBody(urlSafeNoPad).size)
        assertEquals(0, ShareLinkParser.parseBody("[{\"outbounds\":[]}]").size)
    }

    @Test
    fun idSurvivesRename() {
        val a = ShareLinkParser.parseLink(reality)!!
        assertEquals(a.id, a.copy(name = "Renamed").id)
    }

    @Test
    fun configHasRealityOutboundAndNoInboundsForDelayTests() {
        val root = JSONObject(XrayConfigBuilder.build(ShareLinkParser.parseLink(reality)!!))
        assertFalse(root.has("inbounds"))
        val out = root.getJSONArray("outbounds").getJSONObject(0)
        assertEquals("proxy", out.getString("tag"))
        assertEquals("vless", out.getString("protocol"))
        val stream = out.getJSONObject("streamSettings")
        assertEquals("reality", stream.getString("security"))
        val settings = stream.getJSONObject("realitySettings")
        assertEquals("PUBLICKEY", settings.getString("publicKey"))
        assertEquals("www.example.org", settings.getString("serverName"))
        val user = out.getJSONObject("settings").getJSONArray("vnext").getJSONObject(0)
            .getJSONArray("users").getJSONObject(0)
        assertEquals("xtls-rprx-vision", user.getString("flow"))
    }

    @Test
    fun localSocksIsLoopbackAndRequiresPassword() {
        val socks = tm.skytunnel.app.data.proxy.LocalSocks(12345, "u", "p")
        val root = JSONObject(XrayConfigBuilder.build(ShareLinkParser.parseLink(reality)!!, socks))
        val inbounds = root.getJSONArray("inbounds")
        assertEquals("tun", inbounds.getJSONObject(0).getString("protocol"))
        val check = inbounds.getJSONObject(1)
        assertEquals("127.0.0.1", check.getString("listen"))
        assertEquals("password", check.getJSONObject("settings").getString("auth"))
    }

    @Test
    fun neverLogsVisitedSites() {
        val profile = ShareLinkParser.parseLink(reality)!!
        val release = JSONObject(XrayConfigBuilder.build(profile)).getJSONObject("log")
        assertEquals("none", release.getString("access"))
        assertEquals("none", release.getString("loglevel"))
        val debug = JSONObject(XrayConfigBuilder.build(profile, debugLogs = true)).getJSONObject("log")
        assertEquals("none", debug.getString("access"))
    }

    @Test
    fun profileJsonRoundTrips() {
        val p = ShareLinkParser.parseLink(reality)!!
        assertEquals(p, tm.skytunnel.app.data.proxy.ServerProfile.fromJson(p.toJson()))
        assertNotNull(p.id)
    }
}
