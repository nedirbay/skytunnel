package tm.skytunnel.app

import android.app.Application
import libv2ray.Libv2ray
import tm.skytunnel.app.data.SettingsStore
import tm.skytunnel.app.data.SubscriptionRepository
import tm.skytunnel.app.data.TrafficStore
import java.io.File

/** Owns the objects shared by the UI and the VPN service. */
class SkyApp : Application() {

    lateinit var subscriptions: SubscriptionRepository
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var traffic: TrafficStore
        private set

    override fun onCreate() {
        super.onCreate()
        // An empty key makes Xray pick a random XUDP base key on each start.
        Libv2ray.initCoreEnv(filesDir.absolutePath, "")
        subscriptions = SubscriptionRepository(this)
        settings = SettingsStore(this)
        traffic = TrafficStore(File(filesDir, "traffic.json"))
    }
}
