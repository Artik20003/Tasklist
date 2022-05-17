package tasklist
import kotlinx.datetime.*
import java.io.File
import com.squareup.moshi.*

class LocalDateTimeAdapter {
    @ToJson
    fun toJson(value: LocalDateTime): String {
        return value.toString()
    }

    @FromJson
    fun fromJson(value: String): LocalDateTime {
        return LocalDateTime.parse(value)
    }
}

class Task(){

    var name: String = ""
        set(value) {
            field = value.trim()
        }

    private fun splitBy(s: String,n: Int = 5):String {
        if(s.length <= n) return s

        val firstNIndex =  s.indexOf('\n')
        if (firstNIndex > 0 && firstNIndex <= n )
            return s.substring(0, firstNIndex + 1) + splitBy(s.substring(firstNIndex + 1), n)
        else
            return s.substring(0, n) + '\n' + splitBy(s.substring(n), n)
    }

    fun getNameList (letterCountByLine: Int = 44): MutableList<String> = splitBy(name, letterCountByLine).split('\n').toMutableList()

    enum class Priority (val string: String){
        CRITICAL("\u001B[101m \u001B[0m"),
        HIGH("\u001B[103m \u001B[0m"),
        NORMAL("\u001B[102m \u001B[0m"),
        LOW("\u001B[104m \u001B[0m")
    }
    private var deadlineDateTime: LocalDateTime? = null
    var priority: Priority = Priority.NORMAL

    fun getDedlineDateString():String {
        return deadlineDateTime.toString().split("T")[0]
    }

    fun getDedlineTimeString():String {
        return deadlineDateTime.toString().split("T")[1]
    }

    fun getDeadlineStatus ():String {
        if (deadlineDateTime == null) return ""
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val numberOfDays = currentDate.daysUntil(deadlineDateTime?.date!!)
        return when {
            numberOfDays > 0 -> "\u001B[102m \u001B[0m"
            numberOfDays < 0 -> "\u001B[101m \u001B[0m"
            else -> "\u001B[103m \u001B[0m"
        }
    }
    fun setDeadlineDate (date: String){
        val arrDate = date.split("-").toMutableList()
        if(arrDate[1].length == 1) arrDate[1] = "0"+arrDate[1]
        if(arrDate[2].length == 1) arrDate[2] = "0"+arrDate[2]
        
        deadlineDateTime = (arrDate.joinToString("-")+"T00:00:00").toLocalDateTime()
    }

    fun setDeadlineTime (time: String){
        val timeStr = time.split(":").map({if(it.length == 1) "0$it" else it}).joinToString(":")
        deadlineDateTime = (deadlineDateTime.toString().split("T")[0]+"T"+timeStr).toLocalDateTime()
    }

    fun getPriorityString ():String {
        return priority.string
    }
}


class TaskList () {
    @OptIn(ExperimentalStdlibApi::class)
    constructor (filePath: String = "tasklist.json") : this() {
        try {
            val moshiBuilder = Moshi.Builder()
            moshiBuilder.add(KotlinJsonAdapterFactory())
            moshiBuilder.add(LocalDateTimeAdapter())
            val moshi = moshiBuilder.build()
            taskListAdapter  = moshi.adapter<MutableList<Task>>()
            val file = File(filePath)
            if (file.exists()) {
                tasks = taskListAdapter.fromJson(file.readText()) ?: mutableListOf<Task>()
            }
            this.filePath = filePath

        } catch (e: Exception) {
            println(e.message)
            println("Can't work with file $filePath")
        }


    }
    private var filePath: String? = null
    private lateinit var taskListAdapter: JsonAdapter<MutableList<Task>>
    private var tasks = mutableListOf<Task>()
    fun saveToFile() {
        if(filePath != null) {
            File(filePath).writeText(taskListAdapter.toJson(tasks))
        }
    }

    fun addTask(task: Task) {
        if (task.name.isNotBlank()) {
            tasks.add(task)
        } else {
            println("The task is blank")
        }

    }


    fun getTask(n: Int): Task {
        check(n in 1..tasks.size) {"Invalid task number"}
        return tasks[n-1]
    }
    fun getTasksCount():Int = tasks.size

    fun print() {
        val separator = "+----+------------+-------+---+---+--------------------------------------------+"
        val beforeTaskString = "|    |            |       |   |   |"

        if(tasks.isEmpty()) {
            println("No tasks have been input")
            return
        }
        
        println(separator)
        println("| N  |    Date    | Time  | P | D |                   Task                     |")
        println(separator)
        for (i in tasks.indices) {
            val strNum = (i + 1).toString() + " ".repeat(2 - (i + 1).toString().length)
            print("| $strNum | ${tasks[i].getDedlineDateString()} | ${tasks[i].getDedlineTimeString()} | ${tasks[i].getPriorityString()} | ${tasks[i].getDeadlineStatus()} |")
            val taskNameLines = tasks[i].getNameList()
            for (i in taskNameLines.indices) {
                val prefix = if(i == 0) "" else beforeTaskString
                println(prefix + taskNameLines[i] + " ".repeat(44-taskNameLines[i].length) + "|")
            }
            println(separator)
        }
    }

    fun delete(n: Int) {
        tasks.remove(getTask(n))
        println("The task is deleted")
    }
}
fun readAndSetPriority (task: Task) {
    println("Input the task priority (C, H, N, L):")
    when (readln().uppercase()) {
        "C" -> task.priority = Task.Priority.CRITICAL
        "H" -> task.priority = Task.Priority.HIGH
        "N" -> task.priority = Task.Priority.NORMAL
        "L" -> task.priority = Task.Priority.LOW
        else -> readAndSetPriority(task)
    }
}

fun readAndSetDate (task: Task) {
    println("Input the date (yyyy-mm-dd):")
    try {
        task.setDeadlineDate(readln())
    } catch (e: Exception) {
        println("The input date is invalid")
        readAndSetDate(task)
    }
}
fun readAndSetTime (task: Task) {
    println("Input the time (hh:mm):")
    try {
        task.setDeadlineTime(readln())
    } catch (e: Exception) {
        println("The input time is invalid")
        readAndSetTime(task)
    }
}

fun readAndSetName (task: Task) {
    println("Input a new task (enter a blank line to end):")
    var taskName = ""
    var line: String
    do {
        line = readln()
        taskName += "$line\n"
    } while (line.isNotBlank())
    if(taskName.isBlank())
        println("The task is blank")
    task.name= taskName
}

fun readNumAndDeleteItem(taskList: TaskList, printList: Boolean = true) {
    if(printList)
        taskList.print()
    if(taskList.getTasksCount() == 0)
        return

    try {
        println("Input the task number (1-${taskList.getTasksCount()}):")
        taskList.delete(readln().toInt())
    } catch(e: Exception) {
        println("Invalid task number")
        readNumAndDeleteItem(taskList, false)
        return
    }
}

fun readNumAndEditItem(taskList: TaskList, printList:Boolean = true) {
    if(printList)
        taskList.print()
    if(taskList.getTasksCount() == 0)
        return
    var task: Task = Task()
    try {
        println("Input the task number (1-${taskList.getTasksCount()}):")
        task = taskList.getTask(readln().toInt())
    } catch(e: Exception) {
        println("Invalid task number")
        readNumAndEditItem(taskList, false)
        return
    }

    while (true) {
        println("Input a field to edit (priority, date, time, task):")
        val field = readln();
        when(field) {
            "priority" -> readAndSetPriority(task)
            "date" -> readAndSetDate(task)
            "time" -> readAndSetTime(task)
            "task" -> readAndSetName(task)
            else -> println("Invalid field")
        }
        if (field in listOf<String>("priority", "date", "time", "task")) {
            println("The task is changed")
            break
        }
    }
}


fun main() {

    val taskList = TaskList("tasklist.json")

    do {
        println("Input an action (add, print, edit, delete, end):")
        val action  = readln()
        when (action) {
            "add" -> {
                val task = Task()
                readAndSetPriority(task)
                readAndSetDate(task)
                readAndSetTime(task)
                readAndSetName(task)
                if(task.name.isNotBlank())
                    taskList.addTask(task)
            }
            "print" -> taskList.print()
            "end" -> {println("Tasklist exiting!"); taskList.saveToFile();}
            "delete" -> readNumAndDeleteItem(taskList)
            "edit" -> readNumAndEditItem(taskList)
            else -> println("The input action is invalid")
        }
    } while (action != "end")


}


