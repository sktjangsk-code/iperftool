package com.example.iperftool

data class IntervalLine(
    val id: String,
    val start: Double,
    val end: Double,
    val mbps: Double,
    val omitted: Boolean,
    val isSummary: Boolean,
    val isReceiver: Boolean
)

object IperfParser {
    private val re = Regex(
        """\[\s*(\d+|SUM)]\s+([\d.]+)-([\d.]+)\s+sec\s+[\d.]+\s+\w*Bytes\s+([\d.]+)\s+(\w?)bits/sec(.*)"""
    )

    fun parse(line: String): IntervalLine? {
        val m = re.find(line) ?: return null
        val id = m.groupValues[1]
        val value = m.groupValues[4].toDoubleOrNull() ?: return null
        val mbps = when (m.groupValues[5]) {
            "K" -> value / 1000.0
            "G" -> value * 1000.0
            else -> value
        }
        val tail = m.groupValues[6]
        return IntervalLine(
            id = id,
            start = m.groupValues[2].toDoubleOrNull() ?: 0.0,
            end = m.groupValues[3].toDoubleOrNull() ?: 0.0,
            mbps = mbps,
            omitted = tail.contains("omitted"),
            isSummary = tail.contains("sender") || tail.contains("receiver"),
            isReceiver = tail.contains("receiver")
        )
    }
}

fun parseDuration(args: String): Int =
    Regex("""-t\s*(\d+)""").find(args)?.groupValues?.get(1)?.toIntOrNull() ?: 10
