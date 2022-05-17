import kotlinx.datetime.*

fun isLeapYear(year: String): Boolean {
   try {
       val instant1 = Instant.parse("$year-02-29T22:10:01.324Z")
       return true
   } catch (e: Exception) {
       return false
   }
}

fun main() {
    val year = readLine()!!
    println(isLeapYear(year))
}