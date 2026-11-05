package tm.skytunnel.app.data

import tm.skytunnel.app.data.proxy.ServerProfile

/** Continent filter on the Servers screen. */
enum class Region(val label: String) {
    EUROPE("Europe"),
    ASIA("Asia"),
    AMERICA("America"),
    OTHER("Other")
}

/**
 * A server row for the UI.
 *
 * @param latencyMs real delay through the server; null until tested.
 * @param reachable result of the last delay test; null until tested.
 */
data class Server(
    val id: String,
    val country: String,
    val city: String,
    val countryCode: String,
    val region: Region,
    val protocolLabel: String,
    val isSupported: Boolean,
    val latencyMs: Int? = null,
    val reachable: Boolean? = null
) {
    /** Number of filled signal bars, 0..4, from the measured delay. */
    val signal: Int
        get() = when {
            reachable != true || latencyMs == null -> 0
            latencyMs < 150 -> 4
            latencyMs < 300 -> 3
            latencyMs < 600 -> 2
            else -> 1
        }

    companion object {
        fun from(profile: ServerProfile, latencyMs: Int?, reachable: Boolean?): Server {
            val code = CountryInfo.detectCode(profile.name)
            val country = CountryInfo.displayName(code)
            val label = CountryInfo.stripFlags(profile.name).ifEmpty { profile.address }
            return Server(
                id = profile.id,
                country = country.ifEmpty { label },
                city = if (country.isEmpty() || label.equals(country, true)) profile.protocolLabel else label,
                countryCode = code,
                region = CountryInfo.regionOf(code),
                protocolLabel = profile.protocolLabel,
                isSupported = profile.isSupported,
                latencyMs = latencyMs,
                reachable = reachable
            )
        }
    }
}
