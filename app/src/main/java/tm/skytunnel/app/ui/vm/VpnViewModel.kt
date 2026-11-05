package tm.skytunnel.app.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import libv2ray.Libv2ray
import tm.skytunnel.app.SkyApp
import tm.skytunnel.app.data.AppSettings
import tm.skytunnel.app.data.Server
import tm.skytunnel.app.data.StatsRange
import tm.skytunnel.app.data.StatsSummary
import tm.skytunnel.app.data.SubscriptionException
import tm.skytunnel.app.data.SubscriptionState
import tm.skytunnel.app.data.VpnStatus
import tm.skytunnel.app.data.VpnUiState
import tm.skytunnel.app.data.proxy.ServerProfile
import tm.skytunnel.app.data.proxy.XrayConfigBuilder
import tm.skytunnel.app.ui.theme.ThemeMode
import tm.skytunnel.app.vpn.VpnController
import tm.skytunnel.app.vpn.VpnRuntime

/** Joins the subscription, settings and the running tunnel into one screen state. */
class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SkyApp
    private val delays = MutableStateFlow<Map<String, Int?>>(emptyMap())
    private val refreshing = MutableStateFlow(false)
    private val settingsFlow = app.settings.settings.map { it as AppSettings? }

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** One-off messages for the snackbar. */
    val events: SharedFlow<String> = _events.asSharedFlow()

    val state: StateFlow<VpnUiState> = combine(
        app.subscriptions.state,
        VpnController.runtime,
        settingsFlow,
        delays,
        refreshing
    ) { subscription, runtime, settings, delayMap, isRefreshing ->
        buildState(subscription, runtime, settings, delayMap, isRefreshing)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VpnUiState())

    init {
        viewModelScope.launch {
            VpnController.runtime
                .map { it.error }
                .distinctUntilChanged()
                .collect { error -> if (error != null) _events.tryEmit(error) }
        }
    }

    private fun buildState(
        subscription: SubscriptionState,
        runtime: VpnRuntime,
        settings: AppSettings?,
        delayMap: Map<String, Int?>,
        isRefreshing: Boolean
    ): VpnUiState {
        val servers = subscription.profiles.map { profile ->
            val tested = delayMap.containsKey(profile.id)
            val latency = delayMap[profile.id]
            Server.from(profile, latency, if (tested) latency != null else null)
        }
        return VpnUiState(
            status = runtime.status,
            servers = servers,
            selectedServer = servers.firstOrNull { it.id == subscription.selectedId },
            subscriptionTitle = subscription.title,
            hasSubscriptionUrl = subscription.url.isNotEmpty(),
            usage = subscription.usage,
            isRefreshing = isRefreshing,
            elapsedSeconds = runtime.elapsedSeconds,
            downloadMbps = runtime.downloadMbps,
            uploadMbps = runtime.uploadMbps,
            sessionBytes = runtime.sessionBytes,
            ipAddress = runtime.exitIp ?: "—",
            settingsLoaded = settings != null,
            autoConnect = settings?.autoConnect ?: true,
            notifications = settings?.notifications ?: true,
            themeMode = settings?.themeMode ?: ThemeMode.SYSTEM
        )
    }

    /** Call only after the VPN permission has been granted. */
    fun connect() {
        val selected = app.subscriptions.state.value.selected
        when {
            selected == null -> message("Add a subscription and choose a server first")
            !selected.isSupported -> message("${selected.protocolLabel} is not supported yet")
            else -> VpnController.start(app)
        }
    }

    fun disconnect() = VpnController.stop(app)

    fun message(text: String) {
        _events.tryEmit(text)
    }

    /** Selects a server, and moves the tunnel to it if one is running. */
    fun selectServer(id: String): Boolean {
        val profile = app.subscriptions.state.value.profiles.firstOrNull { it.id == id } ?: return false
        if (!profile.isSupported) {
            message("${profile.protocolLabel} is not supported yet")
            return false
        }
        app.subscriptions.select(id)
        val status = VpnController.runtime.value.status
        if (status == VpnStatus.CONNECTED || status == VpnStatus.CONNECTING) VpnController.start(app)
        return true
    }

    fun addSubscription(input: String) = runRefresh { app.subscriptions.add(input) }

    /** Re-downloads the subscription if it has a link, then tests every server. */
    fun refresh() = runRefresh {
        if (app.subscriptions.state.value.url.isNotEmpty()) app.subscriptions.refresh()
    }

    private fun runRefresh(load: suspend () -> Unit) {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            try {
                load()
            } catch (e: SubscriptionException) {
                message(e.message.orEmpty())
            }
            testServers()
            refreshing.value = false
        }
    }

    fun removeSubscription() {
        if (VpnController.runtime.value.status != VpnStatus.DISCONNECTED) disconnect()
        app.subscriptions.remove()
        delays.value = emptyMap()
    }

    private suspend fun testServers() {
        val profiles = app.subscriptions.state.value.profiles.filter { it.isSupported }
        val limit = Semaphore(MAX_PARALLEL_TESTS)
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            profiles.map { profile ->
                launch {
                    limit.withPermit {
                        val result = measure(profile)
                        delays.value = delays.value + (profile.id to result)
                    }
                }
            }
        }
    }

    private fun measure(profile: ServerProfile): Int? = try {
        Libv2ray.measureOutboundDelay(XrayConfigBuilder.build(profile), PROBE_URL)
            .takeIf { it >= 0 }
            ?.toInt()
    } catch (_: Exception) {
        null
    }

    fun statsSummary(range: StatsRange): StatsSummary = app.traffic.summary(range)

    fun setAutoConnect(value: Boolean) {
        viewModelScope.launch { app.settings.setAutoConnect(value) }
    }

    fun setNotifications(value: Boolean) {
        viewModelScope.launch { app.settings.setNotifications(value) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { app.settings.setThemeMode(mode) }
    }

    private companion object {
        const val MAX_PARALLEL_TESTS = 5
        const val PROBE_URL = "https://www.gstatic.com/generate_204"
    }
}
