package com.kitec.calculatorkidkid

import java.math.BigInteger
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Pure calculation core. UI must catch CalculationException and keep the input editable. */
class CalculationException(message: String) : IllegalArgumentException(message)

class CalculatorEngine(
    private val precision: Int = 60,
    private val rounding: RoundingMode = RoundingMode.HALF_UP
) {
    private val context = MathContext(precision.coerceAtLeast(50), rounding)

    fun evaluate(expression: String): BigDecimal {
        if (expression.length > 500) throw CalculationException("Expression exceeds 500 characters")
        val normalized = expression.replace('×', '*').replace('÷', '/').replace('−', '-')
            .replace('\u0E50', '0').replace('\u0E51', '1').replace('\u0E52', '2').replace('\u0E53', '3')
            .replace('\u0E54', '4').replace('\u0E55', '5').replace('\u0E56', '6').replace('\u0E57', '7')
            .replace('\u0E58', '8').replace('\u0E59', '9')
        if ('%' in normalized) {
            val number = "[0-9]+(?:\\.[0-9]*)?(?:[eE][+-]?[0-9]+)?"
            val compact = normalized.filterNot(Char::isWhitespace)
            val approved = listOf("$number%", "$number(?:[+-]$number%)+", "$number[*/]$number%")
            if (approved.none { compact.matches(Regex(it)) })
                throw CalculationException("Complex percentage semantics need verification")
        }
        val parser = Parser(normalized)
        val result = parser.parse().decimal(context)
        if (result.signum() != 0 && result.precision().toLong() - result.scale() - 1 !in -1000L..1000L) {
            throw CalculationException("Result exceeds supported exponent range")
        }
        return result.stripTrailingZeros().let { if (it.signum() == 0) BigDecimal.ZERO else it }
    }

    /** Display formatting only; stored calculations must retain full precision. */
    fun format(value: BigDecimal, places: Int = 10, mode: RoundingMode = rounding): String {
        require(places in 0..20)
        val rounded = value.setScale(places, mode).stripTrailingZeros()
        if (rounded.signum() == 0) return "0"
        val plain = rounded.toPlainString()
        return if (plain.replace("-", "").replace(".", "").length > 15 ||
            (rounded.abs() < BigDecimal("0.000001") && rounded.signum() != 0)) {
            scientific(rounded)
        } else plain
    }

    private fun scientific(value: BigDecimal): String {
        val v = value.stripTrailingZeros()
        val exponent = v.precision() - v.scale() - 1
        return v.movePointLeft(exponent).toPlainString() + "E" + (if (exponent >= 0) "+" else "") + exponent
    }

    /** Presentation orchestration is separate from strict grammar parsing. */
    data class Completion(val expression: String, val value: BigDecimal, val saveable: Boolean)

    fun complete(expression: String): Completion? {
        if (expression.isBlank()) return null
        try { return Completion(expression, evaluate(expression), true) }
        catch (_: CalculationException) { }
        // The handoff explicitly approves this partial case only. Other ambiguous
        // trailing-operator cases remain suspended pending historical evidence.
        if (expression.matches(Regex("[+−-]?[0-9]+(?:\\.[0-9]+)?[+]"))) {
            return Completion(expression, evaluate(expression.dropLast(1)), false)
        }
        val balance = expression.count { it == '(' } - expression.count { it == ')' }
        if (balance in 1..50 && expression.length + balance <= 500) {
            val closed = expression + ")".repeat(balance)
            try { return Completion(closed, evaluate(closed), true) }
            catch (_: CalculationException) { }
        }
        return null
    }

    /** Exact rational intermediates distinguish terminating from recurring results. */
    private data class Fraction(val n: BigInteger, val d: BigInteger = BigInteger.ONE) {
        fun normalized(): Fraction {
            if (d.signum() == 0) throw CalculationException("Division by zero")
            val gcd = n.gcd(d)
            return Fraction(n.divide(gcd).multiply(BigInteger.valueOf(d.signum().toLong())), d.divide(gcd).abs())
        }
        fun add(v: Fraction) = Fraction(n*v.d+v.n*d,d*v.d).normalized()
        fun subtract(v: Fraction) = add(v.negate())
        fun multiply(v: Fraction) = Fraction(n*v.n,d*v.d).normalized()
        fun divide(v: Fraction) = Fraction(n*v.d,d*v.n).normalized()
        fun negate() = Fraction(-n,d)
        fun signum() = n.signum()
        fun decimal(context: MathContext): BigDecimal = try { BigDecimal(n).divide(BigDecimal(d)) }
            catch (_: ArithmeticException) { BigDecimal(n).divide(BigDecimal(d),context) }
        fun terminating(): Boolean {
            var denominator=d
            for (factor in listOf(BigInteger.valueOf(2),BigInteger.valueOf(5)))
                while(denominator.mod(factor)==BigInteger.ZERO) denominator=denominator.divide(factor)
            return denominator==BigInteger.ONE
        }
        companion object {
            fun of(v:BigDecimal): Fraction = if(v.scale()>=0) Fraction(v.unscaledValue(),BigInteger.TEN.pow(v.scale())).normalized()
                else Fraction(v.unscaledValue()*BigInteger.TEN.pow(-v.scale()))
        }
    }
    fun copyValue(expression: String, value: BigDecimal, places: Int, mode: RoundingMode): String {
        val normalized=Editor.normalize(complete(expression)?.expression?.removeSuffix("+") ?: expression, java.util.Locale.US)
        val fraction=runCatching { Parser(normalized).parse() }.getOrNull()
        val copy=if(fraction?.terminating()==true) value else value.setScale(places,mode)
        return copy.stripTrailingZeros().toPlainString().let { if(copy.signum()==0) "0" else it }
    }

    private inner class Parser(private val s: String) {
        private var pos = 0
        private var depth = 0
        fun parse(): Fraction {
            val result = sum()
            skipSpaces()
            if (pos != s.length) fail("Unexpected character")
            return result
        }
        private fun sum(): Fraction {
            var left = product()
            while (true) {
                skipSpaces()
                left = when {
                    eat('+') -> { val start=pos; val right=product(); left.add(if (simplePercent(start,pos)) left.multiply(right) else right) }
                    eat('-') -> { val start=pos; val right=product(); left.subtract(if (simplePercent(start,pos)) left.multiply(right) else right) }
                    else -> return left
                }
            }
        }
        private fun simplePercent(start:Int,end:Int):Boolean = s.substring(start,end).filterNot(Char::isWhitespace)
            .matches(Regex("[0-9]+(?:\\.[0-9]*)?(?:[eE][+-]?[0-9]+)?%"))
        private fun product(): Fraction {
            var left = signed()
            while (true) {
                skipSpaces()
                left = when {
                    eat('*') -> left.multiply(signed())
                    eat('/') -> {
                        val right = signed()
                        if (right.signum() == 0) fail("Division by zero")
                        left.divide(right)
                    }
                    // A number followed by '(' or adjacent parenthesis means multiplication.
                    pos < s.length && s[pos] == '(' -> left.multiply(signed())
                    else -> return left
                }
            }
        }
        private fun signed(): Fraction {
            skipSpaces()
            return when {
                eat('+') -> signed()
                eat('-') -> signed().negate()
                else -> percent()
            }
        }
        private fun percent(): Fraction {
            var v = atom()
            while (eat('%')) v = v.divide(Fraction(BigInteger.valueOf(100)))
            return v
        }
        private fun atom(): Fraction {
            skipSpaces()
            if (eat('(')) {
                if (++depth > 50) fail("Too many nested parentheses")
                val v = sum()
                if (!eat(')')) fail("Missing closing parenthesis")
                depth--
                return v
            }
            val start = pos
            var digits = 0
            var decimal = false
            while (pos < s.length) {
                when (s[pos]) {
                    in '0'..'9' -> { digits++; pos++ }
                    '.' -> if (!decimal) { decimal = true; pos++ } else break
                    else -> break
                }
            }
            if (digits == 0) fail("Expected a number")
            if (digits > 200) fail("Operand exceeds 200 digits")
            // Scientific notation is accepted only when its exponent is complete.
            if (pos < s.length && (s[pos] == 'E' || s[pos] == 'e')) {
                pos++
                if (pos < s.length && (s[pos] == '+' || s[pos] == '-')) pos++
                val exponentStart = pos
                while (pos < s.length && s[pos].isDigit()) pos++
                if (pos == exponentStart) fail("Incomplete exponent")
                val exponent = s.substring(exponentStart, pos).toIntOrNull() ?: fail("Exponent too large")
                if (exponent > 1000) fail("Exponent too large")
            }
            return try { Fraction.of(BigDecimal(s.substring(start, pos))) }
            catch (_: NumberFormatException) { fail("Invalid number") }
        }
        private fun eat(c: Char): Boolean {
            skipSpaces()
            if (pos < s.length && s[pos] == c) { pos++; return true }
            return false
        }
        private fun skipSpaces() { while (pos < s.length && s[pos].isWhitespace()) pos++ }
        private fun fail(message: String): Nothing = throw CalculationException("$message at position $pos")
    }
}
