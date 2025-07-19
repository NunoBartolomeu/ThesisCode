package com.ledger.app.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Simple RGB holder
data class Rgb(val r: Int, val g: Int, val b: Int) {
    init {
        require(r in 0..255 && g in 0..255 && b in 0..255) {
            "RGB values must be between 0 and 255"
        }
    }
}

enum class LogLevel(val priority: Int) {
    DEBUG(10),
    INFO(20),
    WARN(30),
    ERROR(40)
}

class ColorLogger(
    private val componentName: String,
    private val color: Rgb,
    private var minLevel: LogLevel = LogLevel.INFO
) {
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss")

    /** Change the minimum level at runtime */
    fun setLevel(level: LogLevel) {
        minLevel = level
    }

    private fun ansiColor(): String =
        "\u001B[38;2;${color.r};${color.g};${color.b}m"

    private val reset = "\u001B[0m"

    private fun log(level: LogLevel, message: String) {
        if (level.priority < minLevel.priority) return

        val timestamp = LocalDateTime.now().format(timeFmt)
        println("${ansiColor()}[$timestamp] [${level.name}]\t$componentName – $message$reset")
    }

    fun debug(message: String) = log(LogLevel.DEBUG, message)
    fun info(message: String)  = log(LogLevel.INFO,  message)
    fun warn(message: String)  = log(LogLevel.WARN,  message)
    fun error(message: String) = log(LogLevel.ERROR, message)
}
