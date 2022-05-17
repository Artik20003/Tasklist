import kotlinx.datetime.*

fun nextMonth(date: String): String {
    val instant1 = Instant.parse(date)
    return instant1.plus(DateTimePeriod(months = 1), TimeZone.UTC).toString()
}

fun main() {
    val date = readLine()!!
    println(nextMonth(date))
}