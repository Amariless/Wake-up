package com.fritangui.wakeup.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

private val BULLET_LINE = Regex("""^(\s*)([-*])\s(.*)$""")
private val NUMBERED_LINE = Regex("""^(\s*)(\d+)\.\s(.*)$""")

/**
 * Envuelve el `onValueChange` de un campo de texto multilínea (descripción/notas de una tarea)
 * para que las listas de viñetas ("- " o "* ") y numeradas ("1.", "2."...) se sigan solas: al
 * presionar Enter justo después de una línea con ese formato, la línea nueva empieza con el mismo
 * marcador (el número siguiente, si es una lista numerada).
 *
 * Si la línea de la que viene el Enter tiene SOLO el marcador (la escribiste, no le pusiste texto
 * todavía, o borraste el texto y volviste a presionar Enter), en vez de seguir repitiendo el
 * marcador para siempre, lo QUITA — así "salir" de la lista es tan simple como presionar Enter una
 * vez más sobre una viñeta vacía, en vez de tener que borrarla a mano.
 */
fun continueListFormat(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    // Solo entra en juego cuando el cambio fue exactamente "se escribió un salto de línea" — pegar
    // texto, borrar, o cualquier otra edición pasa de largo sin tocarse.
    if (new.text.length != old.text.length + 1) return new
    val cursor = new.selection.start
    if (cursor <= 0 || cursor > new.text.length || new.text[cursor - 1] != '\n') return new

    val textBeforeNewline = new.text.substring(0, cursor - 1)
    val previousLineStart = textBeforeNewline.lastIndexOf('\n') + 1
    val previousLine = textBeforeNewline.substring(previousLineStart)

    BULLET_LINE.find(previousLine)?.let { match ->
        val (indent, marker, content) = match.destructured
        return if (content.isBlank()) {
            exitList(new, previousLineStart, cursor)
        } else {
            continueLine(new, cursor, "$indent$marker ")
        }
    }
    NUMBERED_LINE.find(previousLine)?.let { match ->
        val (indent, number, content) = match.destructured
        return if (content.isBlank()) {
            exitList(new, previousLineStart, cursor)
        } else {
            val nextNumber = (number.toIntOrNull() ?: 0) + 1
            continueLine(new, cursor, "$indent$nextNumber. ")
        }
    }
    return new
}

private fun continueLine(new: TextFieldValue, cursor: Int, prefix: String): TextFieldValue {
    val newText = new.text.substring(0, cursor) + prefix + new.text.substring(cursor)
    return TextFieldValue(newText, selection = TextRange(cursor + prefix.length))
}

/** Quita el marcador (y el salto de línea recién agregado) de la línea vacía, sin crear una línea nueva. */
private fun exitList(new: TextFieldValue, previousLineStart: Int, cursor: Int): TextFieldValue {
    val newText = new.text.removeRange(previousLineStart, cursor)
    return TextFieldValue(newText, selection = TextRange(previousLineStart))
}
