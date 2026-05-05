package ui.screens.takeout.editor

data class ParsedTakeoutFoodInput(val itemNumber: Short, val quantity: Int)

fun parseTakeoutFoodInput(input: String): List<ParsedTakeoutFoodInput> {
    val text = input.trim()
    if (text.isBlank()) return emptyList()
    return text.split(Regex("\\s+")).map { token ->
        val parts = token.split("*")
        if (parts.isEmpty() || parts.size > 2 || parts.any { it.isBlank() }) return emptyList()
        val itemNumber = parts[0].toShortOrNull() ?: return emptyList()
        val quantity = if (parts.size == 1) 1 else parts[1].toIntOrNull() ?: return emptyList()
        if (itemNumber <= 0 || quantity <= 0) return emptyList()
        ParsedTakeoutFoodInput(itemNumber, quantity)
    }
}
