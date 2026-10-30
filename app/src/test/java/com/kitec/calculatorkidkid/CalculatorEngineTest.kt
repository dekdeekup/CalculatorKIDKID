package com.kitec.calculatorkidkid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalculatorEngineTest {
    private val engine = CalculatorEngine()
    @Test fun precedence() = assertEquals("14", engine.evaluate("2+3×4").toPlainString())
    @Test fun parentheses() = assertEquals("14", engine.evaluate("2×(3+4)").toPlainString())
    @Test fun implicitMultiplication() = assertEquals("14", engine.evaluate("2(3+4)").toPlainString())
    @Test fun decimalPrecision() = assertEquals("0.3", engine.evaluate("0.1+0.2").toPlainString())
    @Test fun percent() = assertEquals("0.25", engine.evaluate("25%").toPlainString())
    @Test fun divisionByZero() { assertThrows(CalculationException::class.java) { engine.evaluate("1/0") } }
    @Test fun missingParenthesis() { assertThrows(CalculationException::class.java) { engine.evaluate("2×(3+4") } }
    @Test fun invalidCharacter() { assertThrows(CalculationException::class.java) { engine.evaluate("2abc") } }
}
