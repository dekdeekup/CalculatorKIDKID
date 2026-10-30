package com.kitec.calculatorkidkid

import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale

data class EditState(val text: String = "", val start: Int = 0, val end: Int = start)
data class Insertion(val state: EditState, val truncated: Boolean)

/** All input sources share the same selection and undo transactions. */
class Editor {
    var state = EditState(); private set
    private val undo = ArrayDeque<EditState>()
    private val redo = ArrayDeque<EditState>()
    fun select(start: Int, end: Int) { state = state.copy(start = start, end = end) }
    fun change(next: EditState) {
        if (next.text == state.text) { state = next; return }
        if(next.text.length > 500 || !validPrefix(next.text)) throw CalculationException("Input limit exceeded")
        if (undo.size == 50) undo.removeFirst()
        undo.addLast(state); redo.clear(); state = next
    }
    fun undo() { if (undo.isNotEmpty()) { redo.addLast(state); state = undo.removeLast() } }
    fun redo() { if (redo.isNotEmpty()) { undo.addLast(state); state = redo.removeLast() } }
    fun clearStacks() { undo.clear(); redo.clear() }
    fun insert(raw: String, locale: Locale = Locale.US, memory: Boolean = false): Insertion {
        var value = normalize(raw, locale)
        var a = minOf(state.start, state.end).coerceIn(0, state.text.length)
        val b = maxOf(state.start, state.end).coerceIn(a, state.text.length)
        if (memory && a == 1 && state.text.take(a) == "0" && (b == state.text.length || state.text[b] !in '0'..'9')) a = 0
        if (memory && value.startsWith('-')) value = "($value)"
        val room = 500 - (state.text.length - (b - a))
        val truncated = value.length > room
        if (truncated) {
            var cut = room.coerceAtLeast(0)
            // A scientific literal is indivisible, including its mantissa.
            Regex("(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)[eE][+-]?[0-9]*").findAll(value).forEach {
                if (cut > it.range.first && cut <= it.range.last) cut = it.range.first
            }
            value = value.take(cut)
            // Memory is a representation, so retain balanced parentheses.
            if (memory && value.startsWith('(') && !value.endsWith(')')) {
                value = if (value.length > 2 && room >= 4) value.dropLast(1) + ")" else ""
            }
            while (value.isNotEmpty() && !validPrefix(state.text.take(a) + value + state.text.drop(b))) value = value.dropLast(1)
        }
        if (value.isNotEmpty()) change(EditState(state.text.take(a) + value + state.text.drop(b), a + value.length))
        return Insertion(state, truncated)
    }
    private fun validPrefix(s: String): Boolean {
        var depth = 0
        for (c in s) { if (c == '(' && ++depth > 50) return false; if (c == ')') depth-- }
        return Regex("[0-9.]+").findAll(s).none { it.value.count(Char::isDigit) > 200 }
    }
    companion object {
        fun normalize(raw: String, locale: Locale): String {
            val symbols = DecimalFormatSymbols.getInstance(locale)
            val s = buildString {
                raw.forEach { c -> append(when {
                    c in '๐'..'๙' -> '0' + (c - '๐')
                    c.isWhitespace() -> return@forEach
                    c == '×' -> '*'
                    c == '÷' -> '/'
                    c == '−' -> '-'
                    else -> c
                }) }
            }
            // Validate grouping per complete numeric token before removing it.
            val grouping = symbols.groupingSeparator
            val decimal = symbols.decimalSeparator
            val allowed = "0123456789.+-*/()%eE" + grouping + decimal
            if (s.any { it !in allowed }) throw CalculationException("Invalid pasted character")
            if (s.contains(grouping) && grouping != decimal) {
                val escaped = Regex.escape(grouping.toString())
                val dec = Regex.escape(decimal.toString())
                Regex("[0-9${escaped}${dec}]+").findAll(s).filter { grouping in it.value }.forEach {
                    if (!it.value.matches(Regex("[0-9]{1,3}(?:${escaped}[0-9]{3})+(?:${dec}[0-9]+)?")))
                        throw CalculationException("Invalid numeric grouping")
                }
            }
            return s.replace(grouping.toString(), "").replace(decimal, '.')
        }
    }
}

/** Group presentation only; expressions, persisted values and clipboard remain canonical. */
fun localizeNumber(value:String,locale:Locale):String {
    val symbols=DecimalFormatSymbols.getInstance(locale)
    if('E' in value) return value.replace('.',symbols.decimalSeparator)
    val sign=if(value.startsWith('-')) "-" else ""
    val parts=value.removePrefix("-").split('.')
    val integer=parts[0].reversed().chunked(3).joinToString(symbols.groupingSeparator.toString()).reversed()
    return sign+integer+(if(parts.size>1) symbols.decimalSeparator+parts[1] else "")
}
