package tm.skytunnel.app.ui.navigation

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import tm.skytunnel.app.ui.components.SkyBottomBar
import tm.skytunnel.app.ui.screens.ConnectedScreen
import tm.skytunnel.app.ui.screens.HomeScreen
import tm.skytunnel.app.ui.screens.PremiumScreen
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
        val scope = rememberCoroutineScope()

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
                        onToggleConnection = viewModel::toggleConnection,
                        onServerClick = { navController.navigateToTab(Routes.SERVERS) },
                        onDetailsClick = { navController.navigate(Routes.CONNECTED) },
                        onPremiumClick = { navController.navigate(Routes.PREMIUM) },
                        onMenuClick = { navController.navigateToTab(Routes.SETTINGS) }
                    )
                }

                composable(Routes.SERVERS) {
                    ServersScreen(
                        selectedId = state.selectedServer.id,
                        onSelect = { server ->
                            if (server.isPremium && !state.isPremium) {
                                scope.launch {
                                    snackbarHostState.showMessage(
                                        "${server.country} is a Premium server"
                                    )
                                }
                            } else if (!server.isOnline) {
                                scope.launch {
                                    snackbarHostState.showMessage(
                                        "${server.city} is offline"
                                    )
                                }
                            } else {
                                viewModel.selectServer(server)
                                navController.navigateToTab(Routes.HOME)
                            }
                        },
                        onBack = { navController.navigateToTab(Routes.HOME) }
                    )
                }

                composable(Routes.STATS) {
                    StatisticsScreen(
                        state = state,
                        onBack = { navController.navigateToTab(Routes.HOME) }
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        state = state,
                        onBack = { navController.navigateToTab(Routes.HOME) },
                        onAutoConnect = viewModel::setAutoConnect,
                        onNotifications = viewModel::setNotifications,
                        onKillSwitch = viewModel::setKillSwitch,
                        onThemeMode = viewModel::setThemeMode,
                        onAccount = { navController.navigate(Routes.PREMIUM) },
                        onSplitTunneling = {
                            scope.launch {
                                snackbarHostState.showMessage("Split Tunneling coming soon")
                            }
                        }
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
                        onPremium = { navController.navigate(Routes.PREMIUM) }
                    )
                }

                composable(Routes.PREMIUM) {
                    PremiumScreen(
                        isPremium = state.isPremium,
                        onBack = { navController.popBackStack() },
                        onUpgrade = {
                            viewModel.setPremium(true)
                            scope.launch {
                                snackbarHostState.showMessage("Premium activated")
                            }
                        }
                    )
                }
            }
        }

        // "Auto Connect": programma açylanda diňe bir gezek. Her Home-a gaýdanda
        // baglansa, "Disconnect" basylandan soň baglanyşyk derrew täzeden açylardy.
        var autoConnectDone by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(route) {
            if (route == Routes.HOME && !autoConnectDone) {
                autoConnectDone = true
                if (state.autoConnect) viewModel.connect()
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
