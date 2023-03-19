package tasklist

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.datetime.*
import java.io.File

data class Task(var description: String, var priority: String, var date: LocalDate, var time: LocalTime, var dueTag: Char)

class LocalDateAdapter {
    @ToJson fun toJson(date: LocalDate): String = date.toString()
    @FromJson fun fromJson(dateString: String): LocalDate = LocalDate.parse(dateString)
}

class LocalTimeAdapter {
    @ToJson fun toJson(time: LocalTime): String = time.toString()
    @FromJson fun fromJson(timeString: String): LocalTime = LocalTime.parse(timeString)
}

const val SEPARATOR = "+----+------------+-------+---+---+--------------------------------------------+"
const val HEADER = "| N  |    Date    | Time  | P | D |                   Task                     |"
const val MAX_SIZE = 44

fun main() {
    val jsonFile = File("tasklist.json")
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .add(LocalDateAdapter())
        .add(LocalTimeAdapter())
        .build()
    val jsonAdapter = moshi.adapter<List<Task>>(Types.newParameterizedType(List::class.java, Task::class.java))

    val tasks = mutableListOf<Task>()
    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date

    if (jsonFile.exists()) {
        val json = jsonFile.readText()
        tasks.addAll(jsonAdapter.fromJson(json) ?: emptyList())
    }

    while (true) {
        println("Input an action (add, print, edit, delete, end):")
        val input = readlnOrNull()?.trim()?.lowercase() ?: continue

        when (input) {
            "add" -> {
                var description = ""
                var priority: String
                var date: LocalDate? = null
                var time: LocalTime? = null

                do {
                    println("Input the task priority (C, H, N, L):")
                    priority = readln().trim().uppercase()
                } while (priority !in setOf("C", "H", "N", "L"))

                do {
                    println("Input the date (yyyy-mm-dd):")
                    val dateInput = readln()

                    try {
                        val (year, month, day) = dateInput.split("-").map { it.toInt() }
                        date = LocalDate(year, month, day)
                    } catch (e: Exception) {
                        println("The input date is invalid")
                    }
                } while (date == null)

                val daysUntil = currentDate.daysUntil(date)
                val dueTag = when {
                    daysUntil < 0 -> 'O'
                    daysUntil == 0 -> 'T'
                    else -> 'I'
                }

                do {
                    println("Input the time (hh:mm):")
                    val timeInput = readln()

                    try {
                        val (hour, minute) = timeInput.split(":").map { it.toInt() }
                        time = LocalTime(hour, minute)
                    } catch (e: Exception) {
                        println("The input time is invalid")
                    }
                } while (time == null)

                println("Input a new task (enter a blank line to end):")

                while (true) {
                    val task = readlnOrNull()
                    if (task.isNullOrBlank()) {
                        if (description.isBlank()) println("The task is blank")
                        break
                    }
                    description += "$task\n"
                }

                tasks.add(Task(description.trimEnd(), priority, date, time, dueTag))
            }
            "print" -> {
                if (tasks.isEmpty()) {
                    println("No tasks have been input")
                } else {
                    printTasks(tasks)
                }
            }
            "edit" -> {
                if (tasks.isEmpty()) {
                    println("No tasks have been input")
                } else {
                    printTasks(tasks)

                    loop@while (true) {
                        println("Input the task number (1-${tasks.size}):")
                        val numInput = readln().trim()

                        try {
                            val num = numInput.toInt()
                            if (num in 1..tasks.size) {
                                val task = tasks[num - 1]

                                while (true) {
                                    println("Input a field to edit (priority, date, time, task):")

                                    when (readln().trim().lowercase()) {
                                        "priority" -> {
                                            println("Input the task priority (C, H, N, L):")
                                            val priority = readln().trim().uppercase()

                                            if (priority in listOf("C", "H", "N", "L")) {
                                                task.priority = priority
                                                println("The task is changed")
                                                break@loop
                                            }
                                        }
                                        "date" -> {
                                            println("Input the date (yyyy-mm-dd):")
                                            val dateInput = readln()

                                            try {
                                                val (year, month, day) = dateInput.split("-").map { it.toInt() }
                                                task.date = LocalDate(year, month, day)
                                                val daysUntil = currentDate.daysUntil(task.date)
                                                task.dueTag = when {
                                                    daysUntil < 0 -> 'O'
                                                    daysUntil == 0 -> 'T'
                                                    else -> 'I'
                                                }
                                                println("The task is changed")
                                                break@loop
                                            } catch (e: Exception) {
                                                println("The input date is invalid")
                                            }
                                        }
                                        "time" -> {
                                            println("Input the time (hh:mm):")
                                            val timeInput = readln()

                                            try {
                                                val (hour, minute) = timeInput.split(":").map { it.toInt() }
                                                task.time = LocalTime(hour, minute)
                                                println("The task is changed")
                                                break@loop
                                            } catch (e: Exception) {
                                                println("The input time is invalid")
                                            }
                                        }
                                        "task" -> {
                                            println("Input a new task (enter a blank line to end):")
                                            val editedTask = mutableListOf<String>()

                                            while (true) {
                                                val taskInput = readlnOrNull()
                                                if (taskInput.isNullOrBlank()) break
                                                editedTask.add(taskInput)
                                            }

                                            task.description = editedTask.joinToString(separator = "\n   ")
                                            println("The task is changed")
                                            break@loop
                                        }
                                        else -> println("Invalid field")
                                    }
                                }
                            } else {
                                println("Invalid task number")
                            }
                        } catch (e: NumberFormatException) {
                            println("Invalid task number")
                        }
                    }
                }
            }
            "delete" -> {
                if (tasks.isEmpty()) {
                    println("No tasks have been input")
                } else {
                    printTasks(tasks)

                    while (true) {
                        println("Input the task number (1-${tasks.size}):")
                        val numInput = readln().trim()

                        try {
                            val num = numInput.toInt()
                            if (num in 1..tasks.size) {
                                tasks.removeAt(num - 1)
                                println("The task is deleted")
                                break
                            } else {
                                println("Invalid task number")
                            }
                        } catch (e: NumberFormatException) {
                            println("Invalid task number")
                        }
                    }
                }
            }
            "end" -> {
                println("Tasklist exiting!")
                val json = jsonAdapter.toJson(tasks)
                jsonFile.writeText(json)
                break
            }
            else -> {
                println("The input action is invalid")
            }
        }
    }
}

fun printTasks(tasks: List<Task>) {
    println(SEPARATOR)
    println(HEADER)
    println(SEPARATOR)
    tasks.forEachIndexed { index, task ->
        val num = index + 1
        val descriptionLines = task.description.trimEnd().lines()
        val modifiedDescription = descriptionLines.joinToString(
            separator = "\n|    |            |       |   |   |"
        ) { it.chunked(MAX_SIZE).joinToString(
            "|\n|    |            |       |   |   |"
        ) { line -> line.padEnd(MAX_SIZE) } + "|" }

        val priorityCode = when (task.priority) {
            "C" -> "\u001B[101m \u001B[0m"
            "H" -> "\u001B[103m \u001B[0m"
            "N" -> "\u001B[102m \u001B[0m"
            "L" -> "\u001B[104m \u001B[0m"
            else -> " "
        }

        val dueTagCode = when (task.dueTag) {
            'O' -> "\u001B[101m \u001B[0m"
            'T' -> "\u001B[103m \u001B[0m"
            'I' -> "\u001B[102m \u001B[0m"
            else -> " "
        }

        println("| ${num.toString().padEnd(2)} | ${task.date.toString().padEnd(10)} |"
                + " ${task.time.toString().padEnd(5)} | $priorityCode | $dueTagCode |$modifiedDescription")
        println(SEPARATOR)
    }
}