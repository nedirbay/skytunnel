package tm.skytunnel.app.data

/**
 * Demo maglumat çeşmesi. Hakyky programmada bu ýerde tor arkaly
 * serwer sanawyny alýan API gatlagy durmaly.
 */
object ServerRepository {

    val servers: List<Server> = listOf(
        Server("tm-1", "Turkmenistan",   "Ashgabat #1",  "TM", Region.ASIA,    true,  4, 18),
        Server("tm-2", "Turkmenistan",   "Ashgabat #2",  "TM", Region.ASIA,    false, 3, 24),
        Server("tr-1", "Turkey",         "Istanbul #1",  "TR", Region.EUROPE,  true,  4, 42),
        Server("de-1", "Germany",        "Frankfurt #1", "DE", Region.EUROPE,  false, 3, 58),
        Server("nl-1", "Netherlands",    "Amsterdam #1", "NL", Region.EUROPE,  true,  4, 61),
        Server("us-1", "United States",  "New York #1",  "US", Region.AMERICA, false, 2, 124),
        Server("gb-1", "United Kingdom", "London #1",    "GB", Region.EUROPE,  true,  4, 67),
        Server("ca-1", "Canada",         "Toronto #1",   "CA", Region.AMERICA, false, 2, 138),
        Server("fr-1", "France",         "Paris #1",     "FR", Region.EUROPE,  true,  3, 70, isPremium = true),
        Server("se-1", "Sweden",         "Stockholm #1", "SE", Region.EUROPE,  true,  3, 74, isPremium = true),
        Server("ch-1", "Switzerland",    "Zurich #1",    "CH", Region.EUROPE,  true,  4, 66, isPremium = true),
        Server("es-1", "Spain",          "Madrid #1",    "ES", Region.EUROPE,  true,  3, 82, isPremium = true),
        Server("it-1", "Italy",          "Milan #1",     "IT", Region.EUROPE,  false, 3, 72, isPremium = true),
        Server("pl-1", "Poland",         "Warsaw #1",    "PL", Region.EUROPE,  true,  3, 63, isPremium = true),
        Server("jp-1", "Japan",          "Tokyo #1",     "JP", Region.ASIA,    true,  3, 152, isPremium = true),
        Server("sg-1", "Singapore",      "Singapore #1", "SG", Region.ASIA,    true,  4, 118, isPremium = true),
        Server("in-1", "India",          "Mumbai #1",    "IN", Region.ASIA,    true,  3, 96, isPremium = true),
        Server("ae-1", "UAE",            "Dubai #1",     "AE", Region.ASIA,    true,  4, 54, isPremium = true),
        Server("id-1", "Indonesia",      "Jakarta #1",   "ID", Region.ASIA,    false, 2, 164, isPremium = true),
        Server("th-1", "Thailand",       "Bangkok #1",   "TH", Region.ASIA,    true,  3, 142, isPremium = true),
        Server("us-2", "United States",  "Los Angeles #1", "US", Region.AMERICA, true, 3, 172, isPremium = true),
        Server("ca-2", "Canada",         "Vancouver #1", "CA", Region.AMERICA, true,  3, 168, isPremium = true),
        Server("br-1", "Brazil",         "Sao Paulo #1", "BR", Region.AMERICA, true,  2, 210, isPremium = true),
        Server("mx-1", "Mexico",         "Mexico City #1", "MX", Region.AMERICA, false, 2, 186, isPremium = true)
    )

    val default: Server get() = servers.first()

    fun byRegion(region: Region?): List<Server> =
        if (region == null) servers else servers.filter { it.region == region }

    fun count(region: Region?): Int = byRegion(region).size
}
