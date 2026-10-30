package com.kitec.calculatorkidkid

import android.app.Application
import android.content.Context
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.room.Room
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28,35])
class AndroidWorkflowTest {
    private val context:Context get()=RuntimeEnvironment.getApplication()
    @Test fun persistentSettingsAndIndependentMemory() {
        val p=Preferences(context)
        assertTrue(p.memory(1,"0.123456789012345678901234567890123456789"))
        assertTrue(p.memory(2,"-3"))
        assertTrue(p.save(Settings(language="en",places=20,days=90,maximum=500)))
        val fresh=Preferences(context)
        assertEquals(20,fresh.settings().places); assertEquals("-3",fresh.memory(2))
        assertTrue(fresh.save(Settings()))
        assertEquals("0.123456789012345678901234567890123456789",fresh.memory(1))
        assertTrue(fresh.memory(1,null)); assertNull(fresh.memory(1)); assertEquals("-3",fresh.memory(2))
    }
    private fun <T> databaseWork(block:()->T):T {
        val worker=java.util.concurrent.Executors.newSingleThreadExecutor()
        return try { worker.submit(java.util.concurrent.Callable { block() }).get(30,TimeUnit.SECONDS) } finally { worker.shutdown() }
    }
    @Test fun roomSortPruneDeleteRestoreAndReopen() { databaseWork {
        val db=HistoryDatabase.open(context); val dao=db.history()
        val now=System.currentTimeMillis()
        val old=dao.insert(HistoryEntry(expression="old",value="1",created=0))
        val first=dao.insert(HistoryEntry(expression="first",value="2",created=now))
        val second=dao.insert(HistoryEntry(expression="second",value="3",created=now))
        dao.prune(30,300)
        assertEquals(listOf(second,first),dao.all().map { it.id })
        val deleted=dao.all().first(); dao.delete(second); assertEquals(1,dao.all().size)
        dao.insert(deleted); assertEquals(2,dao.all().size)
        dao.cap(1); assertEquals(second,dao.all().single().id)
        db.close()
        val reopened=HistoryDatabase.open(context); assertEquals("3",reopened.history().all().single().value)
        reopened.history().clear(); assertTrue(reopened.history().all().isEmpty()); reopened.close()
    } }
    @Test fun corruptHistoryPreservesOriginalFileAndCalculator() {
        val file=context.getDatabasePath("history.db"); file.parentFile!!.mkdirs()
        val original="deliberately corrupt history; preserve for recovery".toByteArray()
        file.writeBytes(original)
        val db=HistoryDatabase.open(context)
        val failure=assertThrows(java.util.concurrent.ExecutionException::class.java) { databaseWork { db.history().all() } }
        assertTrue(failure.cause.toString(), failure.cause is android.database.sqlite.SQLiteException)
        db.close()
        assertTrue(file.exists()); assertArrayEquals(original,file.readBytes())
        assertEquals("4",CalculatorEngine().evaluate("2+2").toPlainString())
    }
    private fun all(view:View):List<View> = listOf(view)+(if(view is ViewGroup) (0 until view.childCount).flatMap { all(view.getChildAt(it)) } else emptyList())
    private fun flush(a:MainActivity) {
        val f=MainActivity::class.java.getDeclaredField("work").apply { isAccessible=true }
        val executor=f.get(a) as ExecutorService
        repeat(4) { executor.submit {}.get(30,TimeUnit.SECONDS); shadowOf(Looper.getMainLooper()).idle() }
    }
    @Test fun actualButtonsAutoCloseUndoAndDeduplicateHistory() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        val a=controller.get()
        fun click(text:String) { all(a.window.decorView).filterIsInstance<Button>().first { it.text.toString()==text }.performClick() }
        for(key in listOf("2","×","(","3","+","4","=")) click(key)
        flush(a)
        assertEquals("2*(3+4)",all(a.window.decorView).filterIsInstance<EditText>().single().text.toString())
        click("="); flush(a)
        val db=HistoryDatabase.open(context); databaseWork { assertEquals(1,db.history().all().size); assertEquals("14",db.history().all().single().value) }
        click(a.getString(R.string.undo)); flush(a)
        assertEquals("2*(3+4",all(a.window.decorView).filterIsInstance<EditText>().single().text.toString())
        db.close(); controller.pause().stop().destroy()
    }
    private fun click(a:MainActivity,text:String) {
        all(a.window.decorView).filterIsInstance<Button>().first { it.text.toString()==text }.performClick()
    }
    private fun input(a:MainActivity)=all(a.window.decorView).filterIsInstance<EditText>().single()
    private fun session(a:MainActivity):Any=MainActivity::class.java.getDeclaredField("session").apply { isAccessible=true }.get(a)!!
    private fun field(a:MainActivity,name:String):Any? { val s=session(a); return s.javaClass.getDeclaredField(name).apply { isAccessible=true }.get(s) }
    @Test fun partialPreviewCanBeStoredAndNegativeMemoryInserted() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); val a=controller.get()
        input(a).setText("12+"); flush(a)
        all(a.window.decorView).filterIsInstance<Button>().first { it.text.toString()=="M1" }.performClick(); flush(a)
        assertEquals("12",Preferences(context).memory(1))
        Preferences(context).memory(2,"-3")
        input(a).setText("2"); input(a).setSelection(1)
        all(a.window.decorView).filterIsInstance<Button>().first { it.text.toString().startsWith("M2") }.performClick(); flush(a)
        assertEquals("2(-3)",input(a).text.toString())
        controller.pause().stop().destroy()
    }
    @Test fun rotationPreservesExpressionSelectionAndUndo() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); var a=controller.get()
        click(a,"1"); click(a,"2"); input(a).setSelection(1); flush(a)
        val config=android.content.res.Configuration(a.resources.configuration)
        config.orientation=android.content.res.Configuration.ORIENTATION_LANDSCAPE
        controller.configurationChange(config); a=controller.get(); flush(a)
        assertEquals("12",input(a).text.toString()); assertEquals(1,input(a).selectionStart)
        click(a,a.getString(R.string.undo)); flush(a); assertEquals("1",input(a).text.toString())
        controller.pause().stop().destroy()
    }
    @Test fun nonRotationRecreationClearsUndoButPreservesExpression() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); var a=controller.get()
        click(a,"1"); click(a,"2"); flush(a); controller.recreate(); a=controller.get(); flush(a)
        assertEquals("12",input(a).text.toString()); click(a,a.getString(R.string.undo)); flush(a)
        assertEquals("12",input(a).text.toString()); controller.pause().stop().destroy()
    }
    @Test fun newLaunchStartsNewExpressionAndSessionButKeepsMemory() {
        val p=Preferences(context); p.memory(1,"123")
        val first=Robolectric.buildActivity(MainActivity::class.java).setup(); click(first.get(),"9"); click(first.get(),"="); flush(first.get())
        first.pause().stop().destroy()
        val second=Robolectric.buildActivity(MainActivity::class.java).setup(); flush(second.get())
        assertEquals("",input(second.get()).text.toString())
        assertEquals(0,(field(second.get(),"history") as Collection<*>).size); assertEquals("123",p.memory(1))
        databaseWork { val db=HistoryDatabase.open(context); assertEquals(1,db.history().all().size); db.close() }
        second.pause().stop().destroy()
    }
    @Test fun historyDeleteUndoIsInvalidatedWhenLeavingScreen() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); val a=controller.get()
        click(a,"9"); click(a,"="); flush(a); click(a,a.getString(R.string.history)); flush(a)
        click(a,a.getString(R.string.delete)); flush(a); assertNotNull(field(a,"deleted"))
        click(a,a.getString(R.string.undo_delete)); flush(a); assertNull(field(a,"deleted"))
        click(a,a.getString(R.string.delete)); flush(a); click(a,a.getString(R.string.back)); flush(a)
        assertNull(field(a,"deleted")); assertEquals("9",input(a).text.toString())
        controller.pause().stop().destroy()
    }
    private fun failPreferenceCommits(a:MainActivity) {
        val wrapped=object:android.content.ContextWrapper(context) {
            override fun getSharedPreferences(name:String,mode:Int):android.content.SharedPreferences {
                val actual=super.getSharedPreferences(name,mode)
                return object:android.content.SharedPreferences by actual {
                    override fun edit():android.content.SharedPreferences.Editor {
                        // Preserve fluent chaining while deliberately failing durable commit.
                        return object:android.content.SharedPreferences.Editor {
                            override fun putString(k:String?,v:String?)=this
                            override fun putStringSet(k:String?,v:MutableSet<String>?)=this
                            override fun putInt(k:String?,v:Int)=this
                            override fun putLong(k:String?,v:Long)=this
                            override fun putFloat(k:String?,v:Float)=this
                            override fun putBoolean(k:String?,v:Boolean)=this
                            override fun remove(k:String?)=this
                            override fun clear()=this
                            override fun commit()=false
                            override fun apply() {}
                        }
                    }
                }
            }
        }
        MainActivity::class.java.getDeclaredField("prefs").apply { isAccessible=true }.set(a,Preferences(wrapped))
    }
    @Test fun memoryPersistenceFailureIsReportedAndNotShownAsStored() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); val a=controller.get()
        failPreferenceCommits(a); input(a).setText("12+"); flush(a); click(a,"M1"); flush(a)
        assertNull(Preferences(context).memory(1))
        assertTrue(all(a.window.decorView).filterIsInstance<android.widget.TextView>().any { it.text.toString()==a.getString(R.string.save_error) })
        controller.pause().stop().destroy()
    }
    @Test fun clearHistoryRequiresConfirmationAndCannotBeUndone() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); val a=controller.get()
        click(a,"8"); click(a,"="); flush(a); click(a,a.getString(R.string.history)); flush(a)
        click(a,a.getString(R.string.clear_all))
        databaseWork { val db=HistoryDatabase.open(context); assertEquals(1,db.history().all().size); db.close() }
        org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick(); flush(a)
        databaseWork { val db=HistoryDatabase.open(context); assertTrue(db.history().all().isEmpty()); db.close() }
        assertNull(field(a,"deleted")); controller.pause().stop().destroy()
    }
    @Test fun settingsFailureDoesNotClaimDurableSuccess() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); val a=controller.get()
        failPreferenceCommits(a); click(a,a.getString(R.string.settings)); flush(a)
        val places=all(a.window.decorView).filterIsInstance<android.widget.Spinner>()[2]
        places.setSelection(5); shadowOf(Looper.getMainLooper()).idle(); flush(a)
        assertEquals(10,Preferences(context).settings().places)
        assertTrue(all(a.window.decorView).filterIsInstance<android.widget.TextView>().any { it.text.toString()==a.getString(R.string.save_error) })
        controller.pause().stop().destroy()
    }
    @Test fun rapidSettingsChangesAreBothDurablySaved() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); val a=controller.get()
        click(a,a.getString(R.string.settings)); flush(a)
        val spinners=all(a.window.decorView).filterIsInstance<android.widget.Spinner>()
        spinners[2].setSelection(5); spinners[3].setSelection(1)
        shadowOf(Looper.getMainLooper()).idle(); flush(a)
        assertEquals(5,Preferences(context).settings().places)
        assertEquals(java.math.RoundingMode.UP,Preferences(context).settings().rounding)
        controller.pause().stop().destroy()
    }
    @Test fun rapidInputAcrossEqualsPreservesAutoCloseOrderAndEveryHistoryItem() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); val a=controller.get()
        for(key in listOf("2","×","(","3","+","4","=","+","2","=")) click(a,key)
        flush(a)
        assertEquals("2*(3+4)+2",input(a).text.toString())
        assertEquals("16",field(a,"value")?.toString())
        databaseWork { val db=HistoryDatabase.open(context); assertEquals(listOf("16","14"),db.history().all().map { it.value }); db.close() }
        controller.pause().stop().destroy()
    }
    @Test fun rotationDuringEqualsPreservesQueuedInput() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); var a=controller.get(); flush(a)
        val oldWorker=MainActivity::class.java.getDeclaredField("work").apply { isAccessible=true }.get(a) as ExecutorService
        val gate=java.util.concurrent.CountDownLatch(1)
        oldWorker.submit { gate.await(30,TimeUnit.SECONDS) }
        for(key in listOf("2","×","(","3","+","4","=","+","2","=")) click(a,key)
        val finished=oldWorker.submit {}
        val config=android.content.res.Configuration(a.resources.configuration); config.orientation=android.content.res.Configuration.ORIENTATION_LANDSCAPE
        controller.configurationChange(config); a=controller.get(); gate.countDown(); finished.get(30,TimeUnit.SECONDS); flush(a)
        assertEquals("2*(3+4)+2",input(a).text.toString()); assertEquals("16",field(a,"value")?.toString())
        databaseWork { val db=HistoryDatabase.open(context); assertEquals(listOf("16","14"),db.history().all().map { it.value }); db.close() }
        controller.pause().stop().destroy()
    }
    @Test fun pendingSettingsSaveSurvivesRotation() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); var a=controller.get()
        click(a,a.getString(R.string.settings)); flush(a)
        val oldWorker=MainActivity::class.java.getDeclaredField("work").apply { isAccessible=true }.get(a) as ExecutorService
        val gate=java.util.concurrent.CountDownLatch(1); oldWorker.submit { gate.await(30,TimeUnit.SECONDS) }
        all(a.window.decorView).filterIsInstance<android.widget.Spinner>()[2].setSelection(5)
        shadowOf(Looper.getMainLooper()).idle(); val finished=oldWorker.submit {}
        val config=android.content.res.Configuration(a.resources.configuration); config.orientation=android.content.res.Configuration.ORIENTATION_LANDSCAPE
        controller.configurationChange(config); a=controller.get(); gate.countDown(); finished.get(30,TimeUnit.SECONDS); flush(a)
        assertEquals(5,Preferences(context).settings().places)
        assertEquals(5,all(a.window.decorView).filterIsInstance<android.widget.Spinner>()[2].selectedItemPosition)
        controller.pause().stop().destroy()
    }
    @Test fun stalePreviewCannotOverwriteNewInput() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); val a=controller.get()
        val input=all(a.window.decorView).filterIsInstance<EditText>().single()
        input.setText("1/3"); input.setText("9+9"); flush(a)
        val field=MainActivity::class.java.getDeclaredField("session").apply { isAccessible=true }; val s=field.get(a)!!
        val value=s.javaClass.getDeclaredField("value").apply { isAccessible=true }.get(s)
        assertEquals("18",value?.toString()); controller.pause().stop().destroy()
    }
}
