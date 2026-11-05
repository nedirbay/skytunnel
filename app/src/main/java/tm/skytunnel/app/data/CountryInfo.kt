package tm.skytunnel.app.data

import java.util.Locale

/** Works out a country and region from a server name chosen in the panel. */
object CountryInfo {

    private val EUROPE = setOf(
        "AL", "AT", "BA", "BE", "BG", "BY", "CH", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR",
        "GB", "GE", "GR", "HR", "HU", "IE", "IS", "IT", "LT", "LU", "LV", "MD", "ME", "MK", "MT",
        "NL", "NO", "PL", "PT", "RO", "RS", "RU", "SE", "SI", "SK", "TR", "UA"
    )
    private val ASIA = setOf(
        "AE", "AF", "AM", "AZ", "BD", "BH", "CN", "HK", "ID", "IL", "IN", "IQ", "IR", "JO", "JP",
        "KG", "KH", "KR", "KW", "KZ", "LA", "LB", "LK", "MM", "MN", "MY", "NP", "OM", "PH", "PK",
        "QA", "SA", "SG", "TH", "TJ", "TM", "TW", "UZ", "VN"
    )
    private val AMERICA = setOf(
        "AR", "BO", "BR", "CA", "CL", "CO", "CR", "CU", "DO", "EC", "GT", "MX", "PA", "PE", "PY",
        "US", "UY", "VE"
    )
    private val ISO = Locale.getISOCountries().toSet()
    private val NAMES: List<Pair<String, String>> = ISO
        .map { Locale("", it).getDisplayCountry(Locale.ENGLISH).lowercase() to it }
        .sortedByDescending { it.first.length }

    private val FLAG_EMOJI = Regex("[\\x{1F1E6}-\\x{1F1FF}]{2}")
    private val TWO_LETTERS = Regex("\\b[A-Z]{2}\\b")

    /** Returns an ISO code, or "" when the name gives no country. */
    fun detectCode(name: String): String {
        FLAG_EMOJI.find(name)?.let { match ->
            val points = match.value.codePoints().toArray()
            val code = points.map { (it - 0x1F1E6 + 'A'.code).toChar() }.joinToString("")
            return if (code == "UK") "GB" else code
        }
        val lower = name.lowercase()
        NAMES.firstOrNull { (country, _) -> Regex("\\b${Regex.escape(country)}\\b").containsMatchIn(lower) }
            ?.let { return it.second }
        TWO_LETTERS.findAll(name).map { it.value }.forEach {
            if (it == "UK") return "GB"
            if (it in ISO) return it
        }
        return ""
    }

    fun displayName(code: String): String =
        if (code.isEmpty()) "" else Locale("", code).getDisplayCountry(Locale.ENGLISH)

    fun regionOf(code: String): Region = when (code) {
        in EUROPE -> Region.EUROPE
        in ASIA -> Region.ASIA
        in AMERICA -> Region.AMERICA
        else -> Region.OTHER
    }

    fun stripFlags(name: String): String = name.replace(FLAG_EMOJI, "").trim()
}
