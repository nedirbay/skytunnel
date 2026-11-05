package tm.skytunnel.app.vpn

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tm.skytunnel.app.data.VpnStatus

/** What the running tunnel reports to the UI. */
data class VpnRuntime(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val serverId: String? = null,
    val elapsedSeconds: Long = 0,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val sessionBytes: Long = 0,
    val exitIp: String? = null,
    val error: String? = null
)

/** The UI's way to start and stop [SkyVpnService] and to observe it. */
object VpnController {

    internal val mutableRuntime = MutableStateFlow(VpnRuntime())
    val runtime: StateFlow<VpnRuntime> = mutableRuntime.asStateFlow()

    /** Connects to the selected server, or switches to it if already connected. */
    fun start(context: Context) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, SkyVpnService::class.java).setAction(SkyVpnService.ACTION_START)
        )
    }

    fun stop(context: Context) {
        if (runtime.value.status == VpnStatus.DISCONNECTED || runtime.value.status == VpnStatus.ERROR) {
            return
        }
        // startService is safe: the service is already in the foreground at this point.
        context.startService(
            Intent(context, SkyVpnService::class.java).setAction(SkyVpnService.ACTION_STOP)
        )
    }
}
