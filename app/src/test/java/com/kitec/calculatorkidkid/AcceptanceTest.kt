package com.kitec.calculatorkidkid

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId

class AcceptanceTest {
    private val e=CalculatorEngine()
    private fun number(expected:String,expression:String) = assertEquals(0,BigDecimal(expected).compareTo(e.evaluate(expression)))
    @Test fun clarifiedPercentExamples() {
        for ((input,result) in mapOf("10%" to "0.1","200+10%" to "220","200-10%" to "180",
            "200*10%" to "20","200/10%" to "2000","200+10%+10%" to "242",
            "200-10%+5%" to "189","200+10%-10%" to "198","100-20%+5%" to "84")) number(result,input)
    }
    @Test fun unresolvedPercentIsNotGuessed() {
        for(input in listOf("200+(10%)","200+10%*2","10%%","(200+10)%","200*(10%)"))
            assertThrows(CalculationException::class.java) { e.evaluate(input) }
    }
    @Test fun partialCopyRetainsTerminatingValue() { val c=e.complete("12.345+")!!; assertEquals("12.345",e.copyValue("12.345+",c.value,0,RoundingMode.HALF_UP)) }
    @Test fun approvedPartialEquals() { val c=e.complete("12+")!!; assertEquals("12+",c.expression); assertEquals("12",c.value.toPlainString()); assertFalse(c.saveable) }
    @Test fun approvedAutoClose() { val c=e.complete("2×(3+4")!!; assertEquals("2×(3+4)",c.expression); assertEquals("14",c.value.toPlainString()); assertTrue(c.saveable) }
    @Test fun ambiguousPreviewSuspended() { assertNull(e.complete("2×(3+")); assertNull(e.complete("1e-")); assertNull(e.complete("(")) }
    @Test fun autoCloseUndo() { val editor=Editor(); editor.change(EditState("2×(3+4",6)); val c=e.complete(editor.state.text)!!; editor.change(EditState(c.expression,c.expression.length)); editor.undo(); assertEquals("2×(3+4",editor.state.text) }
    @Test fun exponentBounds() { number("1e1000","1E1000"); number("1e-1000","1e-1000"); assertThrows(CalculationException::class.java) { e.evaluate("1e1001") }; assertThrows(CalculationException::class.java) { e.evaluate("1e-1000/10") }; assertThrows(CalculationException::class.java) { e.evaluate("1e1000*10") } }
    @Test fun exactTerminatingAndRecurringCopy() { val text="1/8"; assertEquals("0.125",e.copyValue(text,e.evaluate(text),2,RoundingMode.HALF_UP)); assertEquals("0.33",e.copyValue("1/3",e.evaluate("1/3"),2,RoundingMode.HALF_UP)); assertEquals("1",e.copyValue("1/3*3",e.evaluate("1/3*3"),10,RoundingMode.DOWN)) }
    @Test fun preservesSmallInternalValue() { val v=e.evaluate("1e-20"); assertEquals("0",e.format(v)); assertTrue(v.signum()!=0); number("1","1e-20*1e20") }
    @Test fun roundingModes() { val v=BigDecimal("-1.235"); assertEquals("-1.24",e.format(v,2,RoundingMode.HALF_UP)); assertEquals("-1.24",e.format(v,2,RoundingMode.UP)); assertEquals("-1.23",e.format(v,2,RoundingMode.DOWN)) }
    @Test fun noNegativeZeroOrTrailingZeros() { assertEquals("0",e.format(BigDecimal("-0.0001"),2)); assertEquals("1.2",e.format(BigDecimal("1.20000"))) }
    @Test fun scientificAfterRounding() { assertEquals("1E+16",e.format(BigDecimal("10000000000000000"))); assertEquals("1E-7",e.format(BigDecimal("0.0000001"))); assertEquals("0",e.format(BigDecimal("0.0000001"),6)) }
    @Test fun retainsLongTerminatingValue() { val s="1234567890".repeat(15); assertEquals(s,e.evaluate(s).toPlainString()) }
    @Test fun malformedAndZeroDoNotCrash() { for(s in listOf("1/0","1/(2-2)","2..3","2abc","()","1e+")) assertThrows(s,CalculationException::class.java) { e.evaluate(s) } }
    @Test fun limits() { assertThrows(CalculationException::class.java) { e.evaluate("1".repeat(501)) }; assertThrows(CalculationException::class.java) { e.evaluate("1".repeat(201)) }; number("1","(".repeat(50)+"1"+")".repeat(50)); assertThrows(CalculationException::class.java) { e.evaluate("(".repeat(51)+"1"+")".repeat(51)) } }
    @Test fun localeFormattingDoesNotAffectCanonicalValue() {
        assertEquals("1,234.5",localizeNumber("1234.5",Locale.US))
        assertEquals("-1.234,5",localizeNumber("-1234.5",Locale.GERMANY))
        assertEquals("1,23E+20",localizeNumber("1.23E+20",Locale.GERMANY))
    }
    @Test fun normalization() { assertEquals("1234.5+2*3",Editor.normalize(" ๑,๒๓๔.๕ + ๒ × ๓ ",Locale.US)); assertEquals("1234.5",Editor.normalize("1.234,5",Locale.GERMANY)) }
    @Test fun rejectPasteAtomically() { val editor=Editor(); editor.insert("12"); assertThrows(CalculationException::class.java) { editor.insert("3abc") }; assertEquals("12",editor.state.text); assertThrows(CalculationException::class.java) { Editor.normalize("12,34",Locale.US) } }
    @Test fun memoryExamples() {
        for((input,value,expected) in listOf(Triple("2","3","23"),Triple("2","-3","2(-3)"),Triple("12.","3","12.3"),Triple("0","5","5"))) {
            val editor=Editor(); editor.insert(input); editor.insert(value,memory=true); assertEquals(expected,editor.state.text); editor.undo(); assertEquals(input,editor.state.text)
        }
    }
    @Test fun selectionReplacementAndCursorUndo() { val ed=Editor(); ed.insert("1234"); ed.select(1,3); ed.insert("9"); assertEquals(EditState("194",2),ed.state); ed.undo(); assertEquals(EditState("1234",1,3),ed.state); ed.redo(); assertEquals("194",ed.state.text) }
    @Test fun pasteOneUndo() { val ed=Editor(); ed.insert("1"); ed.insert("+๒๓"); ed.undo(); assertEquals("1",ed.state.text) }
    @Test fun undoLimit() { val ed=Editor(); repeat(60) { ed.insert("1") }; repeat(50) { ed.undo() }; assertEquals(10,ed.state.text.length); ed.undo(); assertEquals(10,ed.state.text.length) }
    @Test fun noSplitScientificPaste() { val ed=Editor(); ed.change(EditState("1+".repeat(248),496)); val inserted=ed.insert("12e-34"); assertTrue(inserted.truncated); assertEquals(496,ed.state.text.length) }
    @Test fun truncationUsesCombinedNormalizedLength() { val ed=Editor(); ed.change(EditState("1+".repeat(249),498)); val inserted=ed.insert(" ๒๓๔๕ "); assertTrue(inserted.truncated); assertEquals(500,ed.state.text.length); assertTrue(ed.state.text.endsWith("23")) }
    @Test fun retentionUsesCalendarDaysAcrossDst() { val zone=ZoneId.of("America/New_York"); val today=LocalDate.of(2026,3,10); val cutoff=retentionCutoff(7,today,zone); assertEquals(today.minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli(),cutoff) }
}
