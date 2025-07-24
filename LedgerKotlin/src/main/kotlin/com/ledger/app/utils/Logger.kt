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
        val RED_BRIGHT = RGB(255, 0, 0)
        val RED_DARK = RGB(139, 0, 0)
        val RED_SOFT = RGB(255, 99, 71)

        val GREEN_LIME = RGB(50, 205, 50)
        val GREEN_NEON = RGB(57, 255, 20)
        val GREEN_DARK = RGB(80, 156, 80)

        val BLUE_ELECTRIC = RGB(0, 120, 255)
        val BLUE_SKY = RGB(135, 206, 235)
        val BLUE_DARK = RGB(80, 80, 156)

        val YELLOW_BRIGHT = RGB(255, 255, 0)
        val YELLOW_GOLD = RGB(255, 215, 0)
        val YELLOW_SOFT = RGB(255, 239, 184)

        val ORANGE_BRIGHT = RGB(255, 165, 0)
        val ORANGE_DARK = RGB(255, 140, 0)
        val ORANGE_SOFT = RGB(255, 200, 130)

        val PURPLE_VIVID = RGB(148, 0, 211)
        val PURPLE_SOFT = RGB(180, 140, 255)
        val PURPLE_DARK = RGB(75, 0, 130)

        val PINK_HOT = RGB(255, 20, 147)
        val PINK_SOFT = RGB(255, 182, 193)
        val PINK_SALMON = RGB(255, 160, 122)

        val CYAN_BRIGHT = RGB(0, 255, 255)
        val CYAN_SOFT = RGB(175, 238, 238)
        val CYAN_DARK = RGB(0, 139, 139)

        val GRAY_LIGHT = RGB(211, 211, 211)
        val GRAY_MEDIUM = RGB(128, 128, 128)
        val GRAY_DARK = RGB(64, 64, 64)
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
