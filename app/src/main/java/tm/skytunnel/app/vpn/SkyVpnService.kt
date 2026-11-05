package tm.skytunnel.app.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import tm.skytunnel.app.BuildConfig
import tm.skytunnel.app.MainActivity
import tm.skytunnel.app.R
import tm.skytunnel.app.SkyApp
import tm.skytunnel.app.data.VpnStatus
import tm.skytunnel.app.data.proxy.LocalSocks
import tm.skytunnel.app.data.proxy.ServerProfile
import tm.skytunnel.app.data.proxy.XrayConfigBuilder
import java.net.ServerSocket
import java.security.SecureRandom

/**
 * Owns the TUN interface and the Xray core, which reads packets directly from the TUN
 * descriptor. This app is excluded from the VPN, so Xray's own sockets use the real
 * network and follow it when the phone moves between Wi-Fi and mobile data.
 */
class SkyVpnService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val app get() = application as SkyApp
    private val lock = Any()

    private var tunnelJob: Job? = null
    private var core: CoreController? = null
    private var tun: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutDown(VpnStatus.DISCONNECTED, null)
            return START_NOT_STICKY
        }
        // A null action means Android's Always-on VPN started the service.
        enterForeground(getString(R.string.vpn_connecting))
        startTunnel()
        return START_STICKY
    }

    override fun onRevoke() {
        val message = getString(R.string.vpn_revoked)
        notifyProblem(message)
        shutDown(VpnStatus.ERROR, message)
    }

    override fun onDestroy() {
        stopCore()
        scope.cancel()
        super.onDestroy()
    }

    private fun startTunnel() {
        val previous = tunnelJob
        previous?.cancel()
        stopCore()
        VpnController.mutableRuntime.value = VpnRuntime(status = VpnStatus.CONNECTING)
        tunnelJob = scope.launch {
            previous?.join()
            val profile = app.subscriptions.state.value.selected
            if (profile == null || !profile.isSupported) {
                fail(getString(R.string.vpn_no_server))
                return@launch
            }
            VpnController.mutableRuntime.update { it.copy(serverId = profile.id) }
            val socks = newLocalSocks()
            try {
                val descriptor = establishTun(profile) ?: run {
                    fail(getString(R.string.vpn_permission_needed))
                    return@launch
                }
                val controller = Libv2ray.newCoreController(CoreCallbacks)
                synchronized(lock) {
                    tun = descriptor
                    core = controller
                }
                controller.startLoop(XrayConfigBuilder.build(profile, socks, BuildConfig.DEBUG), descriptor.fd)
                ensureActive()
                if (!serverAnswers(controller)) {
                    ensureActive()
                    fail(getString(R.string.vpn_server_unreachable, profile.name))
                    return@launch
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // Core errors can quote the config, which holds credentials: do not surface them.
                ensureActive()
                fail(getString(R.string.vpn_start_failed))
                return@launch
            }
            VpnController.mutableRuntime.update { it.copy(status = VpnStatus.CONNECTED, error = null) }
            enterForeground(getString(R.string.vpn_connected_to, profile.name))
            launch { checkExitIp(socks) }
            monitor()
        }
    }

    /** The core starting says nothing about the server, so make a real request through it. */
    private fun serverAnswers(controller: CoreController): Boolean =
        PROBE_URLS.any { url ->
            try {
                controller.measureDelay(url) >= 0
            } catch (_: Exception) {
                false
            }
        }

    private fun establishTun(profile: ServerProfile): ParcelFileDescriptor? =
        Builder()
            .setSession(profile.name)
            .setMtu(1500)
            .addAddress("172.19.0.1", 30)
            .addRoute("0.0.0.0", 0)
            // Routing IPv6 here too stops it from leaking around the tunnel.
            .addAddress("fdfe:dcba:9876::1", 126)
            .addRoute("::", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
            .addDisallowedApplication(packageName)
            .apply {
                setConfigureIntent(openAppIntent())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setMetered(false)
            }
            .establish()

    private suspend fun checkExitIp(socks: LocalSocks) {
        repeat(3) {
            val ip = ExitIpChecker.check(socks)
            if (ip != null) {
                VpnController.mutableRuntime.update { it.copy(exitIp = ip) }
                return
            }
            delay(3_000)
        }
    }

    /** Reports once a second until the tunnel job is cancelled. */
    private suspend fun monitor() {
        var seconds = 0L
        var sessionBytes = 0L
        val traffic = app.traffic
        try {
            while (scope.isActive) {
                delay(1_000)
                val stats = synchronized(lock) { core?.queryAllOutboundTrafficStats() }.orEmpty()
                val (down, up) = parseProxyTraffic(stats)
                seconds++
                sessionBytes += down + up
                val downMbps = down * 8 / 1_000_000.0
                traffic.record(down, up, 1, downMbps.toFloat())
                if (seconds % FLUSH_EVERY_SECONDS == 0L) traffic.flush()
                val totalSeconds = seconds
                val totalBytes = sessionBytes
                VpnController.mutableRuntime.update {
                    it.copy(
                        elapsedSeconds = totalSeconds,
                        downloadMbps = downMbps,
                        uploadMbps = up * 8 / 1_000_000.0,
                        sessionBytes = totalBytes
                    )
                }
            }
        } finally {
            withContext(NonCancellable) { traffic.flush() }
        }
    }

    private fun fail(message: String) {
        notifyProblem(message)
        shutDown(VpnStatus.ERROR, message)
    }

    private fun shutDown(status: VpnStatus, error: String?) {
        tunnelJob?.cancel()
        tunnelJob = null
        stopCore()
        VpnController.mutableRuntime.value = VpnRuntime(status = status, error = error)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopCore() = synchronized(lock) {
        try {
            core?.stopLoop()
        } catch (_: Exception) {
            // Already stopped.
        }
        core = null
        try {
            tun?.close()
        } catch (_: java.io.IOException) {
            // Already closed.
        }
        tun = null
    }

    private fun enterForeground(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, getString(R.string.channel_status), NotificationManager.IMPORTANCE_LOW)
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, SkyVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openAppIntent())
            .addAction(0, getString(R.string.vpn_disconnect), stop)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_STATUS, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_STATUS, notification)
        }
    }

    /** Tells the user the connection ended without them asking, if they allow alerts. */
    private fun notifyProblem(message: String) {
        scope.launch {
            if (!app.settings.current().notifications) return@launch
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ALERTS, getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_DEFAULT)
            )
            manager.notify(
                NOTIFICATION_ALERT,
                NotificationCompat.Builder(this@SkyVpnService, CHANNEL_ALERTS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setContentIntent(openAppIntent())
                    .build()
            )
        }
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun newLocalSocks(): LocalSocks {
        val port = ServerSocket(0).use { it.localPort }
        return LocalSocks(port, randomToken(), randomToken())
    }

    private fun randomToken(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private object CoreCallbacks : CoreCallbackHandler {
        override fun startup(): Long = 0
        override fun shutdown(): Long = 0
        override fun onEmitStatus(p0: Long, p1: String?): Long = 0
    }

    companion object {
        const val ACTION_START = "tm.skytunnel.app.START"
        const val ACTION_STOP = "tm.skytunnel.app.STOP"

        private const val CHANNEL_STATUS = "vpn_status"
        private const val CHANNEL_ALERTS = "vpn_alerts"
        private const val NOTIFICATION_STATUS = 1
        private const val NOTIFICATION_ALERT = 2
        private const val FLUSH_EVERY_SECONDS = 30L
        private val PROBE_URLS = listOf(
            "https://www.gstatic.com/generate_204",
            "https://cp.cloudflare.com/generate_204"
        )

        /** Parses `tag,direction,value;...` into (downlink, uplink) bytes of the proxy outbound. */
        internal fun parseProxyTraffic(stats: String): Pair<Long, Long> {
            var down = 0L
            var up = 0L
            stats.split(';').forEach { entry ->
                val parts = entry.split(',')
                if (parts.size == 3 && parts[0] == XrayConfigBuilder.PROXY_TAG) {
                    val value = parts[2].toLongOrNull() ?: 0L
                    if (parts[1] == "downlink") down += value else if (parts[1] == "uplink") up += value
                }
            }
            return down to up
        }
    }
}
