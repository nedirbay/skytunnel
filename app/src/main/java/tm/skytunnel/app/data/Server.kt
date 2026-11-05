package tm.skytunnel.app.data

/** Serweriň ýerleşýän yklymy — "Servers" sahypasyndaky filtr çipleri üçin. */
enum class Region(val label: String) {
    EUROPE("Europe"),
    ASIA("Asia"),
    AMERICA("America")
}

/**
 * Bir VPN serweri.
 *
 * @param signal 0..4 aralygynda signal güýji (sütünjikleriň sany).
 * @param pingMs tizlik barlagy üçin şertli jogap wagty.
 */
data class Server(
    val id: String,
    val country: String,
    val city: String,
    val countryCode: String,
    val region: Region,
    val isOnline: Boolean,
    val signal: Int,
    val pingMs: Int,
    val isPremium: Boolean = false
)
