import com.ledger.app.utils.ColorLogger
import com.ledger.app.utils.LogLevel
import com.ledger.app.utils.RGB

fun generateSteps(step: Int): List<Int> {
    val steps = mutableListOf<Int>()
    var value = 0
    while (value < 255) {
        steps.add(value)
        value += step
    }
    return steps
}

fun testAllColors(step: Int = 50) {
    val rgbSteps = generateSteps(step)
    for (r in rgbSteps) {
        for (g in rgbSteps) {
            for (b in rgbSteps) {
                val color = RGB(r, g, b)
                val logger = ColorLogger("ColorTest", color, LogLevel.INFO)
                logger.info("Test Message - Color ($r, $g, $b)")
            }
        }
    }
}

fun testNamedColors() {
    val colors = listOf(
        // RED
        RGB.RED_DARK, RGB.RED, RGB.RED_LIGHT,
        // GREEN
        RGB.GREEN_DARK, RGB.GREEN, RGB.GREEN_LIGHT,
        // BLUE
        RGB.BLUE_DARK, RGB.BLUE, RGB.BLUE_LIGHT,
        // CYAN
        RGB.CYAN_DARK, RGB.CYAN, RGB.CYAN_LIGHT,
        // YELLOW
        RGB.YELLOW_DARK, RGB.YELLOW, RGB.YELLOW_LIGHT,
        // ORANGE
        RGB.ORANGE_DARK, RGB.ORANGE, RGB.ORANGE_LIGHT,
        // PURPLE
        RGB.PURPLE_DARK, RGB.PURPLE, RGB.PURPLE_LIGHT,
        // PINK
        RGB.PINK_DARK, RGB.PINK, RGB.PINK_LIGHT,
        // GRAY
        RGB.GRAY_DARK, RGB.GRAY, RGB.GRAY_LIGHT,
    )

    colors.forEach { color ->
        val logger = ColorLogger("NamedColorTest", color, LogLevel.INFO)
        logger.info("Testing named color: $color")
    }
}

fun main() {
    testAllColors(50) // adjust cadence here
    println("\n\n\n")
    testNamedColors()
}
