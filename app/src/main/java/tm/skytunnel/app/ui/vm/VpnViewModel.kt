package tm.skytunnel.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tm.skytunnel.app.data.Server
import tm.skytunnel.app.data.VpnStatus
import tm.skytunnel.app.data.VpnUiState
import tm.skytunnel.app.ui.theme.ThemeMode
import kotlin.random.Random

/**
 * Baglanyşyk ýagdaýyny dolandyrýan ViewModel.
 *
 * Bu ýerde hakyky VPN tuneli ýok — baglanyşyk simulirlenýär, şonuň üçin
 * dizaýnlardaky ähli ýagdaýlary (disconnected / connecting / connected)
 * programmany işledip görmek bolýar.
 */
class VpnViewModel : ViewModel() {

    private val _state = MutableStateFlow(VpnUiState())
    val state: StateFlow<VpnUiState> = _state.asStateFlow()

    private var tickerJob: Job? = null

    /** Güýç düwmesi: baglananda üz, üzülende bagla. */
    fun toggleConnection() {
        if (_state.value.isBusy) return
        if (_state.value.isConnected) disconnect() else connect()
    }

    fun connect() {
        if (_state.value.isConnected || _state.value.isBusy) return
        tickerJob?.cancel()
        _state.update { it.copy(status = VpnStatus.CONNECTING) }

        tickerJob = viewModelScope.launch {
            delay(1500)                       // "handshake" simulýasiýasy
            _state.update {
                it.copy(
                    status = VpnStatus.CONNECTED,
                    elapsedSeconds = 0L,
                    ipAddress = randomIp(),
                    dataUsedMb = 0.0,
                    downloadMbps = 24.5,
                    uploadMbps = 18.7
                )
            }
            while (true) {                    // sekuntlaýyn hasaplaýjy
                delay(1000)
                _state.update { s ->
                    s.copy(
                        elapsedSeconds = s.elapsedSeconds + 1,
                        dataUsedMb = s.dataUsedMb + Random.nextDouble(0.2, 0.9),
                        downloadMbps = jitter(24.5),
                        uploadMbps = jitter(18.7)
                    )
                }
            }
        }
    }

    fun disconnect() {
        tickerJob?.cancel()
        tickerJob = null
        _state.update {
            it.copy(
                status = VpnStatus.DISCONNECTED,
                elapsedSeconds = 0L,
                downloadMbps = 0.0,
                uploadMbps = 0.0,
                ipAddress = "—"
            )
        }
    }

    /** Serwer çalşylanda, eger bagly bolsa, täze serwere gaýtadan baglanýar. */
    fun selectServer(server: Server) {
        val wasActive = _state.value.isConnected || _state.value.isBusy
        _state.update { it.copy(selectedServer = server) }
        if (wasActive) {
            disconnect()
            connect()
        }
    }

    fun setAutoConnect(value: Boolean) = _state.update { it.copy(autoConnect = value) }
    fun setNotifications(value: Boolean) = _state.update { it.copy(notifications = value) }
    fun setKillSwitch(value: Boolean) = _state.update { it.copy(killSwitch = value) }
    fun setThemeMode(mode: ThemeMode) = _state.update { it.copy(themeMode = mode) }
    fun setPremium(value: Boolean) = _state.update { it.copy(isPremium = value) }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }

    private fun jitter(base: Double) =
        (base + Random.nextDouble(-2.5, 2.5)).coerceAtLeast(1.0)

    private fun randomIp() =
        "185.199.${Random.nextInt(100, 200)}.${Random.nextInt(2, 250)}"
}
