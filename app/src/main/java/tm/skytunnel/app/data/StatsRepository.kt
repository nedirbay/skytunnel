package tm.skytunnel.app.data

import kotlin.math.sin
import kotlin.random.Random

/** Statistika grafigi üçin demo nokatlary we jemler. */
object StatsRepository {

    data class Summary(
        val points: List<Float>,
        val totalDownload: String,
        val totalUpload: String,
        val peakMbps: Float,
        val peakIndex: Int
    )

    fun summaryFor(range: StatsRange): Summary {
        val n = when (range) {
            StatsRange.TODAY -> 24
            StatsRange.WEEK -> 7
            StatsRange.MONTH -> 30
        }
        val seed = range.ordinal * 977 + 13
        val rnd = Random(seed)
        val points = (0 until n).map { i ->
            val wave = sin(i / 3.1).toFloat() * 5f
            (14f + wave + rnd.nextFloat() * 8f).coerceIn(4f, 30f)
        }
        val peakIndex = points.indices.maxByOrNull { points[it] } ?: 0
        val totals = when (range) {
            StatsRange.TODAY -> "1.2 GB" to "842 MB"
            StatsRange.WEEK -> "9.4 GB" to "5.8 GB"
            StatsRange.MONTH -> "38.1 GB" to "22.6 GB"
        }
        return Summary(
            points = points,
            totalDownload = totals.first,
            totalUpload = totals.second,
            peakMbps = points[peakIndex],
            peakIndex = peakIndex
        )
    }
}
