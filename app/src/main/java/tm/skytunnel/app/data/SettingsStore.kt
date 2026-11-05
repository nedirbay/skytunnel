package tm.skytunnel.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tm.skytunnel.app.ui.theme.ThemeMode

data class AppSettings(
    val autoConnect: Boolean = true,
    val notifications: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

private val Context.dataStore by preferencesDataStore("settings")

class SettingsStore(context: Context) {

    private val store = context.applicationContext.dataStore

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            autoConnect = p[AUTO_CONNECT] ?: true,
            notifications = p[NOTIFICATIONS] ?: true,
            themeMode = p[THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: ThemeMode.SYSTEM
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setAutoConnect(value: Boolean) = store.edit { it[AUTO_CONNECT] = value }.let { }
    suspend fun setNotifications(value: Boolean) = store.edit { it[NOTIFICATIONS] = value }.let { }
    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[THEME] = mode.name }.let { }

    private companion object {
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val THEME = stringPreferencesKey("theme")
    }
}
