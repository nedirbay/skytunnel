package tm.skytunnel.app.ui.navigation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import tm.skytunnel.app.ui.components.SkyBottomBar
import tm.skytunnel.app.ui.screens.ConnectedScreen
import tm.skytunnel.app.ui.screens.HomeScreen
import tm.skytunnel.app.ui.screens.ServersScreen
import tm.skytunnel.app.ui.screens.SettingsScreen
import tm.skytunnel.app.ui.screens.SplashScreen
import tm.skytunnel.app.ui.screens.StatisticsScreen
import tm.skytunnel.app.ui.theme.SkyTunnelTheme
import tm.skytunnel.app.ui.vm.VpnViewModel

/** Programmanyň kökündäki Composable: tema + nawigasiýa. */
@Composable
fun SkyTunnelApp(viewModel: VpnViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    SkyTunnelTheme(
        themeMode = state.themeMode,
        forceLightStatusIcons = route == null || route == Routes.SPLASH
    ) {
        // 03_Servers_Screen dizaýnynda aşaky nawigasiýa ýok: aşakda "Refresh" durýar.
        val showBottomBar = route in setOf(Routes.HOME, Routes.STATS, Routes.SETTINGS)

        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.events.collect { snackbarHostState.showMessage(it) }
        }

        val vpnPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) viewModel.connect()
            else viewModel.message("VPN permission is required to connect")
        }
        val connectWithVpnPermission = {
            val request = VpnService.prepare(context)
            if (request != null) vpnPermission.launch(request) else viewModel.connect()
        }
        // The connection works without notifications, so go on whatever the answer is.
        val notificationPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { connectWithVpnPermission() }
        val requestConnect = {
            val needsNotificationPermission =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
            if (state.selectedServer == null) {
                navController.navigateToTab(Routes.SERVERS)
            } else if (needsNotificationPermission) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                connectWithVpnPermission()
            }
        }

        // Bagly bolanda, Home-dan awtomatiki Connected sahypasyna geçmeli däl —
        // ulanyjy "Connection Details" arkaly özi geçýär.
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    SkyBottomBar(
                        current = route ?: Routes.HOME,
                        onSelect = { tab -> navController.navigateToTab(tab.route) }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = if (showBottomBar) padding.calculateBottomPadding() else 0.dp
                    )
            ) {
                composable(Routes.SPLASH) {
                    SplashScreen(
                        onFinished = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.HOME) {
                    HomeScreen(
                        state = state,
                        onToggleConnection = {
                            if (state.isConnected || state.isBusy) viewModel.disconnect()
                            else requestConnect()
                        },
                        onServerClick = { navController.navigateToTab(Routes.SERVERS) },
                        onDetailsClick = { navController.navigate(Routes.CONNECTED) },
                        onMenuClick = { navController.navigateToTab(Routes.SETTINGS) }
                    )
                }

                composable(Routes.SERVERS) {
                    ServersScreen(
                        servers = state.servers,
                        selectedId = state.selectedServer?.id,
                        isRefreshing = state.isRefreshing,
                        onSelect = { server ->
                            if (viewModel.selectServer(server.id)) {
                                navController.navigateToTab(Routes.HOME)
                            }
                        },
                        onRefresh = viewModel::refresh,
                        onAddSubscription = viewModel::addSubscription,
                        onBack = { navController.navigateToTab(Routes.HOME) }
                    )
                }

                composable(Routes.STATS) {
                    StatisticsScreen(
                        state = state,
                        loadSummary = viewModel::statsSummary,
                        onBack = { navController.navigateToTab(Routes.HOME) }
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        state = state,
                        onBack = { navController.navigateToTab(Routes.HOME) },
                        onAutoConnect = viewModel::setAutoConnect,
                        onNotifications = viewModel::setNotifications,
                        onOpenVpnSettings = {
                            context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
                        },
                        onThemeMode = viewModel::setThemeMode,
                        onRefreshSubscription = viewModel::refresh,
                        onRemoveSubscription = viewModel::removeSubscription,
                        onAddSubscription = { navController.navigateToTab(Routes.SERVERS) }
                    )
                }

                composable(Routes.CONNECTED) {
                    ConnectedScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                        onDisconnect = {
                            viewModel.disconnect()
                            navController.popBackStack()
                        },
                    )
                }
            }
        }

        // Android or another VPN can end the tunnel while this screen is open.
        LaunchedEffect(route, state.status) {
            if (route == Routes.CONNECTED && !state.isConnected) navController.popBackStack()
        }

        // "Auto Connect": programma açylanda diňe bir gezek. Her Home-a gaýdanda
        // baglansa, "Disconnect" basylandan soň baglanyşyk derrew täzeden açylardy.
        var autoConnectDone by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(route, state.settingsLoaded) {
            if (route == Routes.HOME && state.settingsLoaded && !autoConnectDone) {
                autoConnectDone = true
                // Never open the permission dialog on launch: only connect if already allowed.
                if (state.autoConnect && state.selectedServer != null && VpnService.prepare(context) == null) {
                    viewModel.connect()
                }
            }
        }
    }
}

/** Aşaky nawigasiýa üçin: ýekeje nusga, stegi çişirmän. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private suspend fun SnackbarHostState.showMessage(text: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(text)
}
