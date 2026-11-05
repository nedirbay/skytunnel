package tm.skytunnel.app.data

import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** One hour of real tunnel traffic. */
data class TrafficBucket(
    val downBytes: Long = 0,
    val upBytes: Long = 0,
    val activeSeconds: Long = 0,
    val peakMbps: Float = 0f
)

data class StatsSummary(
    val points: List<Float>,
    val downBytes: Long,
    val upBytes: Long,
    val peakMbps: Float,
    val peakIndex: Int
) {
    val isEmpty: Boolean get() = downBytes == 0L && upBytes == 0L
}

/** Keeps 31 days of hourly traffic totals in a local file. */
class TrafficStore(private val file: File?, private val now: () -> LocalDateTime = LocalDateTime::now) {

    private val lock = Any()
    private val buckets: MutableMap<String, TrafficBucket> = load()

    fun record(downBytes: Long, upBytes: Long, seconds: Long, downMbps: Float) = synchronized(lock) {
        val key = now().format(HOUR)
        val b = buckets[key] ?: TrafficBucket()
        buckets[key] = TrafficBucket(
            downBytes = b.downBytes + downBytes,
            upBytes = b.upBytes + upBytes,
            activeSeconds = b.activeSeconds + seconds,
            peakMbps = maxOf(b.peakMbps, downMbps)
        )
    }

    fun flush() = synchronized(lock) {
        val target = file ?: return@synchronized
        val oldest = now().minusDays(RETENTION_DAYS).format(HOUR)
        buckets.keys.removeAll { it < oldest }
        val root = JSONObject()
        buckets.forEach { (key, b) ->
            root.put(
                key,
                JSONObject().put("d", b.downBytes).put("u", b.upBytes)
                    .put("s", b.activeSeconds).put("p", b.peakMbps.toDouble())
            )
        }
        try {
            target.writeText(root.toString())
        } catch (_: IOException) {
            // Statistics are not worth interrupting the tunnel for.
        }
    }

    fun summary(range: StatsRange): StatsSummary = synchronized(lock) {
        val current = now()
        val groups: List<List<TrafficBucket>> = when (range) {
            StatsRange.TODAY -> (0 until 24).map { hour ->
                listOfNotNull(buckets[current.withHour(hour).format(HOUR)])
            }
            StatsRange.WEEK -> days(current, 7)
            StatsRange.MONTH -> days(current, 30)
        }
        val points = groups.map { group ->
            val seconds = group.sumOf { it.activeSeconds }
            if (seconds == 0L) 0f else (group.sumOf { it.downBytes } * 8f / 1_000_000f) / seconds
        }
        val all = groups.flatten()
        StatsSummary(
            points = points,
            downBytes = all.sumOf { it.downBytes },
            upBytes = all.sumOf { it.upBytes },
            peakMbps = all.maxOfOrNull { it.peakMbps } ?: 0f,
            peakIndex = points.indices.maxByOrNull { points[it] } ?: 0
        )
    }

    private fun days(current: LocalDateTime, count: Int): List<List<TrafficBucket>> =
        (count - 1 downTo 0).map { back ->
            val day = current.minusDays(back.toLong()).format(DAY)
            buckets.filterKeys { it.startsWith(day) }.values.toList()
        }

    private fun load(): MutableMap<String, TrafficBucket> {
        val out = HashMap<String, TrafficBucket>()
        val source = file?.takeIf { it.exists() } ?: return out
        try {
            val root = JSONObject(source.readText())
            root.keys().forEach { key ->
                val o = root.getJSONObject(key)
                out[key] = TrafficBucket(
                    o.optLong("d"), o.optLong("u"), o.optLong("s"), o.optDouble("p", 0.0).toFloat()
                )
            }
        } catch (_: JSONException) {
            out.clear()
        } catch (_: IOException) {
            out.clear()
        }
        return out
    }

    private companion object {
        const val RETENTION_DAYS = 31L
        val HOUR: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHH")
        val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}

/** 1536 B → "1.5 KB". */
fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024L * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}
