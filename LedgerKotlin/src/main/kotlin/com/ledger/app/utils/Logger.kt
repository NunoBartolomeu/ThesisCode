package com.ledger.app.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Simple RGB holder
data class RGB(val r: Int, val g: Int, val b: Int) {
    init {
        require(r in 0..255 && g in 0..255 && b in 0..255) {
            "RGB values must be between 0 and 255"
        }
    }

    companion object {
        val RED_DARK = RGB(150, 0, 0)
        val RED = RGB(200, 0, 0)
        val RED_LIGHT = RGB(250, 0, 0)

        val GREEN_DARK = RGB(0, 150, 0)
        val GREEN = RGB(0, 200, 0)
        val GREEN_LIGHT = RGB(0, 250, 0)

        val BLUE_DARK = RGB(0, 0, 150)
        val BLUE = RGB(0, 0, 200)
        val BLUE_LIGHT = RGB(0, 0, 250)

        val CYAN_DARK = RGB(0, 150, 150)
        val CYAN = RGB(0, 200, 200)
        val CYAN_LIGHT = RGB(0, 250, 250)

        val YELLOW_DARK = RGB(150, 150, 0)
        val YELLOW = RGB(200, 200, 0)
        val YELLOW_LIGHT = RGB(250, 250, 0)

        val ORANGE_DARK = RGB(150, 80, 0)
        val ORANGE = RGB(200, 130, 0)
        val ORANGE_LIGHT = RGB(250, 180, 0)

        val PURPLE_DARK = RGB(100, 0, 150)
        val PURPLE = RGB(150, 0, 200)
        val PURPLE_LIGHT = RGB(200, 100, 250)

        val PINK_DARK = RGB(150, 0, 100)
        val PINK = RGB(200, 0, 150)
        val PINK_LIGHT = RGB(250, 100, 200)

        val GRAY_DARK = RGB(64, 64, 64)
        val GRAY = RGB(128, 128, 128)
        val GRAY_LIGHT = RGB(192, 192, 192)
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
    private val color: RGB,
    private var minLevel: LogLevel
) {
    constructor(componentName: String, color: RGB, minLevelStr: String) :
            this(componentName, color, LogLevel.valueOf(minLevelStr.uppercase()))

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss")

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
