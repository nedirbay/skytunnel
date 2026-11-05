package tm.skytunnel.app.data

import tm.skytunnel.app.ui.theme.ThemeMode

enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

/** Period picker on the Statistics screen. */
enum class StatsRange(val label: String) {
    TODAY("Today"),
    WEEK("7 Days"),
    MONTH("30 Days")
}

data class VpnUiState(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val servers: List<Server> = emptyList(),
    val selectedServer: Server? = null,
    val subscriptionTitle: String = "",
    val hasSubscriptionUrl: Boolean = false,
    val usage: SubscriptionUsage? = null,
    val isRefreshing: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val sessionBytes: Long = 0L,
    val ipAddress: String = "—",
    val settingsLoaded: Boolean = false,
    val autoConnect: Boolean = true,
    val notifications: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val isConnected: Boolean get() = status == VpnStatus.CONNECTED
    val isBusy: Boolean get() = status == VpnStatus.CONNECTING

    /** Session length as 00:12:34. */
    val timerText: String
        get() {
            val h = elapsedSeconds / 3600
            val m = (elapsedSeconds % 3600) / 60
            val s = elapsedSeconds % 60
            return "%02d:%02d:%02d".format(h, m, s)
        }

    val statusLabel: String
        get() = when (status) {
            VpnStatus.CONNECTED -> "Connected"
            VpnStatus.CONNECTING -> "Connecting..."
            VpnStatus.DISCONNECTED -> "Disconnected"
            VpnStatus.ERROR -> "Failed"
        }
}
