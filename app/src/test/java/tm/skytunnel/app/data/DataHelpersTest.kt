package tm.skytunnel.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tm.skytunnel.app.vpn.ExitIpChecker
import tm.skytunnel.app.vpn.SkyVpnService
import java.time.LocalDateTime

class DataHelpersTest {

    @Test
    fun detectsCountryFromFlagNameAndCode() {
        assertEquals("DE", CountryInfo.detectCode("🇩🇪 Frankfurt"))
        assertEquals("NL", CountryInfo.detectCode("Netherlands #2"))
        assertEquals("GB", CountryInfo.detectCode("UK-London"))
        assertEquals("", CountryInfo.detectCode("my server"))
        assertEquals(Region.ASIA, CountryInfo.regionOf("TM"))
        assertEquals(Region.OTHER, CountryInfo.regionOf(""))
    }

    @Test
    fun parsesPanelHeaders() {
        val usage = SubscriptionRepository.parseUsage("upload=100; download=200; total=1000; expire=1700000000")!!
        assertEquals(300L, usage.usedBytes)
        assertEquals(1000L, usage.totalBytes)
        assertEquals(1700000000L, usage.expiresAtEpochSeconds)
        assertNull(SubscriptionRepository.parseUsage(null))
        assertEquals("My VPN", SubscriptionRepository.parseTitle("base64:TXkgVlBO"))
        assertEquals("Plain", SubscriptionRepository.parseTitle("Plain"))
    }

    @Test
    fun parsesOnlyProxyTraffic() {
        val (down, up) = SkyVpnService.parseProxyTraffic("proxy,downlink,500;proxy,uplink,20;direct,downlink,999;")
        assertEquals(500L, down)
        assertEquals(20L, up)
        assertEquals(0L to 0L, SkyVpnService.parseProxyTraffic(""))
    }

    @Test
    fun parsesExitIpResponse() {
        assertEquals("1.2.3.4", ExitIpChecker.parseBody("HTTP/1.1 200 OK\r\nA: b\r\n\r\n1.2.3.4"))
        assertNull(ExitIpChecker.parseBody("HTTP/1.1 500 Nope\r\n\r\n1.2.3.4"))
        assertNull(ExitIpChecker.parseBody("HTTP/1.1 200 OK\r\n\r\n<html>"))
    }

    @Test
    fun trafficSummaryAddsUpByRange() {
        var time = LocalDateTime.of(2026, 11, 5, 10, 30)
        val store = TrafficStore(null) { time }
        assertTrue(store.summary(StatsRange.TODAY).isEmpty)

        store.record(1_000_000, 500, 1, 8f)
        store.record(1_000_000, 500, 1, 8f)
        time = time.minusDays(2)
        store.record(4_000_000, 0, 1, 32f)
        time = time.plusDays(2)

        val today = store.summary(StatsRange.TODAY)
        assertEquals(24, today.points.size)
        assertEquals(2_000_000L, today.downBytes)
        assertEquals(1000L, today.upBytes)
        assertEquals(8f, today.points[10], 0.01f)
        assertEquals(10, today.peakIndex)

        val week = store.summary(StatsRange.WEEK)
        assertEquals(7, week.points.size)
        assertEquals(6_000_000L, week.downBytes)
        assertEquals(32f, week.peakMbps, 0.01f)
        assertEquals(30, store.summary(StatsRange.MONTH).points.size)
    }

    @Test
    fun subscriptionStateRoundTrips() {
        val state = SubscriptionState(
            title = "T",
            url = "https://example.com/s",
            usage = SubscriptionUsage(1, 2, 3),
            profiles = listOfNotNull(
                tm.skytunnel.app.data.proxy.ShareLinkParser.parseLink("trojan://p@example.com:443#X")
            ),
            selectedId = "abc"
        )
        assertEquals(state, SubscriptionRepository.deserialize(SubscriptionRepository.serialize(state)))
    }
}
