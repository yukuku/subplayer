package yuku.subplayer.parser

import java.util.regex.Pattern

class SrtInput(val source: String) {
    companion object {
        private val eventPattern = Pattern.compile("""(\d+)\s*\n\s*(\d+):(\d+):(\d+),(\d+)\s*-->\s*(\d+):(\d+):(\d+),(\d+)\s*\n\s*(.*?)\n\n""", Pattern.MULTILINE or Pattern.DOTALL)
    }

    data class Event(
        val counter: Int,
        val startTime: Long,
        val endTime: Long,
        val text: String
    )

    @JvmField val events = mutableListOf<Event>()

    fun parse() {
        val m = eventPattern.matcher(source)
        while (m.find()) {
            try {
                val counter = m.group(1).toInt()
                val startTime = m.group(2).toLong() * 60 * 60 * 1000 +
                    m.group(3).toLong() * 60 * 1000 +
                    m.group(4).toLong() * 1000 +
                    m.group(5).toLong()
                val endTime = m.group(6).toLong() * 60 * 60 * 1000 +
                    m.group(7).toLong() * 60 * 1000 +
                    m.group(8).toLong() * 1000 +
                    m.group(9).toLong()
                val text = m.group(10)
                events.add(Event(counter, startTime, endTime, text))
            } catch (e: NumberFormatException) {
                continue
            }
        }

        events.sortWith(Comparator { a, b -> a.startTime.compareTo(b.startTime) })
    }

    fun findIndex(time: Long): Int {
        if (events.isEmpty()) return -1

        val first = events.first()
        if (time <= first.startTime) {
            return 0
        }

        val last = events.last()
        if (time >= last.endTime) {
            return events.size - 1
        }

        for ((index, event) in events.withIndex()) {
            if (event.startTime > time) {
                return index - 1
            }
        }
        return events.size - 1
    }
}
