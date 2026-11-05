package tm.skytunnel.app.vpn

import tm.skytunnel.app.data.proxy.LocalSocks
import java.io.DataInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Asks a public service which IP it sees. This app is excluded from its own VPN, so
 * the request goes through Xray's password-protected loopback SOCKS inbound.
 */
object ExitIpChecker {

    private const val HOST = "api.ipify.org"
    private const val TIMEOUT_MS = 10_000

    /** Returns the exit IP, or null if it could not be determined. */
    fun check(socks: LocalSocks): String? = try {
        Socket().use { raw ->
            raw.soTimeout = TIMEOUT_MS
            raw.connect(InetSocketAddress("127.0.0.1", socks.port), TIMEOUT_MS)
            socksHandshake(raw, socks)
            val tls = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                .createSocket(raw, HOST, 443, true) as SSLSocket
            tls.use {
                it.startHandshake()
                if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(HOST, it.session)) {
                    return null
                }
                it.outputStream.write(
                    "GET / HTTP/1.1\r\nHost: $HOST\r\nConnection: close\r\n\r\n".toByteArray()
                )
                it.outputStream.flush()
                parseBody(it.inputStream.readBytes().toString(Charsets.US_ASCII))
            }
        }
    } catch (_: IOException) {
        null
    }

    internal fun parseBody(response: String): String? {
        if (!response.startsWith("HTTP/1.1 200")) return null
        val body = response.substringAfter("\r\n\r\n", "").trim()
        return body.takeIf { it.length in 3..45 && it.all { c -> c.isLetterOrDigit() || c == '.' || c == ':' } }
    }

    private fun socksHandshake(socket: Socket, socks: LocalSocks) {
        val output = socket.getOutputStream()
        val input = DataInputStream(socket.getInputStream())

        output.write(byteArrayOf(5, 1, 2)) // version 5, one method: username/password
        output.flush()
        val greeting = ByteArray(2).also { input.readFully(it) }
        if (greeting[1].toInt() != 2) throw IOException("SOCKS method refused")

        val user = socks.user.toByteArray()
        val password = socks.password.toByteArray()
        output.write(
            byteArrayOf(1, user.size.toByte()) + user + byteArrayOf(password.size.toByte()) + password
        )
        output.flush()
        val auth = ByteArray(2).also { input.readFully(it) }
        if (auth[1].toInt() != 0) throw IOException("SOCKS login refused")

        val host = HOST.toByteArray()
        output.write(
            byteArrayOf(5, 1, 0, 3, host.size.toByte()) + host + byteArrayOf(0x01, 0xBB.toByte())
        )
        output.flush()
        val reply = ByteArray(4).also { input.readFully(it) }
        if (reply[1].toInt() != 0) throw IOException("SOCKS connect failed")
        val addressLength = when (reply[3].toInt()) {
            1 -> 4
            4 -> 16
            3 -> input.readUnsignedByte()
            else -> throw IOException("Bad SOCKS reply")
        }
        input.readFully(ByteArray(addressLength + 2))
    }
}
