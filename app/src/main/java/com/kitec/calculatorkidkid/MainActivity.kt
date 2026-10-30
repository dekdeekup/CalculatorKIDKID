package com.kitec.calculatorkidkid

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.concurrent.Executors

/** Transient state lives in the process only; no saved expression is restored after process death. */
private class Session {
    var activity: MainActivity? = null
    var pendingSettings:Settings?=null
    var settingsGeneration=0L
    var equalsPending=false
    val pendingInput=ArrayDeque<MainActivity.()->Unit>()
    val editor = Editor()
    var orientation = 0
    var page = "calculator"
    var value: BigDecimal? = null
    var preview = ""
    var lastSaved: String? = null
    val history = ArrayDeque<HistoryEntry>()
    var historyScroll = 0
    var deleted: HistoryEntry? = null
}
class MainActivity : Activity() {
    companion object { private val sessions = mutableMapOf<Int, Session>() }
    private lateinit var session: Session
    private lateinit var prefs: Preferences
    private lateinit var settings: Settings
    private var pendingSettings:Settings
        get()=session.pendingSettings ?: settings
        set(value) { session.pendingSettings=value }
    private lateinit var db: HistoryDatabase
    private lateinit var root: LinearLayout
    private var expression: EditText? = null
    private var result: TextView? = null
    private var status: TextView? = null
    private var historyScroller: ScrollView? = null
    private val work = Executors.newSingleThreadExecutor()
    @Volatile private var generation = 0L
    private var equalsPending:Boolean
        get()=session.equalsPending
        set(value) { session.equalsPending=value }
    private val pendingInput get()=session.pendingInput
    private var binding = false
    private var historyError = false
    @Volatile private var dead = false
    private var textColor = Color.BLACK
    private var surfaceColor = Color.WHITE
    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
    private fun t(id: Int) = getString(id)
    private fun engine() = CalculatorEngine(rounding = settings.rounding)
    private fun locale() = Locale.forLanguageTag(settings.language)
    private fun formatted(v: BigDecimal): String = localizeNumber(engine().format(v,settings.places,settings.rounding),locale())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Preferences(this); settings = prefs.settings(); applyLanguage()
        session = if (savedInstanceState == null) Session().also { sessions[taskId] = it }
            else sessions.getOrPut(taskId) { Session() }
        if (savedInstanceState != null && session.orientation == resources.configuration.orientation) session.editor.clearStacks()
        if(session.pendingSettings==null) pendingSettings=settings
        session.activity=this
        session.orientation=resources.configuration.orientation
        db = HistoryDatabase.open(this)
        showPage()
        historyWork { db.history().prune(settings.days,settings.maximum) }
    }
    private fun applyLanguage() {
        val c = Configuration(resources.configuration)
        c.setLocale(locale())
        @Suppress("DEPRECATION") resources.updateConfiguration(c, resources.displayMetrics)
    }
    private fun showPage() {
        generation++
        val dark = settings.theme == "dark" || settings.theme == "system" &&
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        surfaceColor = if (dark) Color.rgb(24,25,28) else Color.rgb(250,250,250)
        textColor = if (dark) Color.WHITE else Color.BLACK
        setTheme(if(dark) android.R.style.Theme_Material_NoActionBar else android.R.style.Theme_Material_Light_NoActionBar)
        window.statusBarColor=backgroundColor(dark); window.navigationBarColor=backgroundColor(dark)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(6.dp,4.dp,6.dp,4.dp); setBackgroundColor(surfaceColor) }
        expression = null; result = null; status = null; historyScroller = null
        root.setOnApplyWindowInsetsListener { v,insets ->
            v.setPadding(6.dp+insets.systemWindowInsetLeft,4.dp+insets.systemWindowInsetTop,6.dp+insets.systemWindowInsetRight,4.dp+insets.systemWindowInsetBottom); insets
        }
        setContentView(root)
        when (session.page) { "history" -> historyPage(); "settings" -> settingsPage(); "about" -> aboutPage(); else -> calculatorPage() }
    }
    private fun backgroundColor(dark:Boolean) = if(dark) Color.rgb(24,25,28) else Color.rgb(250,250,250)
    private fun label(text: String, size: Float = 18f) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(textColor); setPadding(6.dp,4.dp,6.dp,4.dp)
    }
    private fun button(text: String, command: Boolean = true, description: String = text, action: () -> Unit) = Button(this).apply {
        this.text = text; textSize = 16f; minWidth = 0; minimumWidth = 0; minHeight = 48.dp
        setPadding(2.dp,0,2.dp,0); isAllCaps = false; setTextColor(if (command) Color.BLACK else textColor)
        contentDescription = description; isSoundEffectsEnabled = false; isHapticFeedbackEnabled = false
        background = GradientDrawable().apply {
            setColor(if (command) Color.rgb(236,193,74) else if (textColor == Color.WHITE) Color.rgb(55,57,62) else Color.rgb(228,231,236))
            cornerRadius = if (command) 8.dp.toFloat() else 25.dp.toFloat()
            if (command) setStroke(1.dp, Color.rgb(155,119,24))
        }
        setOnClickListener { action() }
    }
    private fun row(parent: LinearLayout = root, entries: List<Pair<String,()->Unit>>) {
        val r = LinearLayout(this)
        entries.forEach { (name,action) -> r.addView(button(name,action=action),LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(2.dp,2.dp,2.dp,2.dp) }) }
        parent.addView(r)
    }
    private fun calculatorPage() {
        row(entries = listOf(t(R.string.history) to { navigate("history") }, t(R.string.settings) to { navigate("settings") }, t(R.string.about) to { navigate("about") }))
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val body = LinearLayout(this).apply { orientation = if (landscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL }
        val needsScroll = !landscape && (resources.configuration.fontScale>1.2f || resources.configuration.screenHeightDp<560)
        if(needsScroll) {
            root.addView(ScrollView(this).apply { addView(body) },LinearLayout.LayoutParams(-1,0,1f))
        } else root.addView(body,LinearLayout.LayoutParams(-1,0,1f))
        val display = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if(landscape) body.addView(ScrollView(this).apply { addView(display) },LinearLayout.LayoutParams(0,-1,1f))
        else body.addView(display,LinearLayout.LayoutParams(-1,-2))
        val editor = object : EditText(this) {
            override fun onTextContextMenuItem(id: Int): Boolean {
                if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText) { paste(); return true }
                return super.onTextContextMenuItem(id)
            }
        }.apply {
            textSize = 28f; gravity = Gravity.END; setTextColor(textColor); setSingleLine(true)
            showSoftInputOnFocus = false; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            contentDescription = t(R.string.expression)
        }
        expression = editor
        display.addView(editor,LinearLayout.LayoutParams(-1,-2))
        result = label(session.preview,24f).apply {
            gravity = Gravity.END; contentDescription=t(R.string.result); isFocusable=true
            setOnLongClickListener { copyResult(); true }
        }
        val resultScroll = HorizontalScrollView(this).apply { addView(result) }
        display.addView(resultScroll)
        status = label(if (historyError) t(R.string.history_error) else "",14f).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        status?.visibility=if(historyError) View.VISIBLE else View.GONE
        display.addView(status)
        val memoryRow = LinearLayout(this)
        (1..2).forEach { index ->
            val value = prefs.memory(index)?.let { runCatching { formatted(BigDecimal(it)) }.getOrNull() }
            val b = button("M$index" + (value?.let { " $it" } ?: ""),description="M$index, " + (value ?: t(R.string.empty))) { memory(index) }
            b.setOnLongClickListener {
                work.execute { val ok=runCatching { prefs.memory(index,null) }.getOrDefault(false); runOnUiThread { if (!dead) { if(ok) showPage() else notifyError(R.string.save_error) } } }; true
            }
            memoryRow.addView(b,LinearLayout.LayoutParams(0,-2,1f))
        }
        display.addView(memoryRow)
        row(display,listOf(t(R.string.paste) to { paste() },t(R.string.copy_result) to { copyResult() }))
        val scroller = ScrollView(this).apply { isFillViewport = true }
        val keyHeight=maxOf(48,(48*resources.configuration.fontScale).toInt()).dp
        val keypad = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; minimumHeight=(if(landscape) 4 else 6)*(keyHeight+4.dp) }
        scroller.addView(keypad)
        body.addView(scroller,if(landscape) LinearLayout.LayoutParams(0,-1,1.4f) else if(needsScroll) LinearLayout.LayoutParams(-1,-2) else LinearLayout.LayoutParams(-1,0,2f))
        val keys = listOf(listOf("C","⌫","(",")"),listOf("7","8","9","÷"),listOf("4","5","6","×"),
            listOf("1","2","3","−"),listOf("±","0",".","+"),listOf("undo","redo","%","="))
        val lines=if(landscape) listOf(listOf("C","⌫","(",")","undo","redo"),listOf("7","8","9","÷","%","±"),listOf("4","5","6","×",".","="),listOf("1","2","3","−","0","+")) else keys
        lines.forEach { line ->
            val r = LinearLayout(this)
            line.forEach { key ->
                val name = when(key) { "undo" -> t(R.string.undo); "redo" -> t(R.string.redo); else -> key }
                val desc = when(key) { "⌫" -> t(R.string.backspace); "±" -> t(R.string.sign); "C" -> t(R.string.clear); "=" -> t(R.string.equals); "×" -> t(R.string.multiply); "÷" -> t(R.string.divide); "+" -> t(R.string.plus); "−" -> t(R.string.minus); "%" -> t(R.string.percent); "(" -> t(R.string.open_parenthesis); ")" -> t(R.string.close_parenthesis); "." -> t(R.string.decimal); else -> name }
                r.addView(button(name,key.singleOrNull()?.isDigit() != true,desc) { key(key) },LinearLayout.LayoutParams(0,-1,1f).apply { setMargins(2.dp,2.dp,2.dp,2.dp) })
            }
            keypad.addView(r,LinearLayout.LayoutParams(-1,0,1f).apply { minHeightCompat(r,keyHeight) })
        }
        bindEditor()
        editor.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?,start:Int,count:Int,after:Int) { if(!binding) syncSelection() }
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) {}
            override fun afterTextChanged(s:Editable?) {
                if (binding) return
                val next = runCatching { Editor.normalize(s.toString(),locale()) }.getOrElse { bindEditor(); notifyError(R.string.input_error); return }
                if(next.length>500 || runCatching { Editor.normalize(next,locale()) }.isFailure) { bindEditor(); notifyError(R.string.input_error); return }
                val previous=session.editor.state.text
                try { session.editor.change(EditState(next,editor.selectionStart.coerceAtLeast(0),editor.selectionEnd.coerceAtLeast(0))) }
                catch (_:CalculationException) { bindEditor(); notifyError(R.string.input_error); return }
                if(next!=s.toString()) bindEditor()
                if(next!=previous) edited()
            }
        })
        editor.setOnKeyListener { _, code, event ->
            if(event.action!=KeyEvent.ACTION_DOWN) false
            else if(code==KeyEvent.KEYCODE_ENTER || code==KeyEvent.KEYCODE_NUMPAD_ENTER || code==KeyEvent.KEYCODE_EQUALS && !event.isShiftPressed) { equals(); true }
            else if(event.isCtrlPressed && code==KeyEvent.KEYCODE_Z) { key(if(event.isShiftPressed) "redo" else "undo"); true }
            else if(event.isCtrlPressed && code==KeyEvent.KEYCODE_Y) { key("redo"); true }
            else if(event.isCtrlPressed && code==KeyEvent.KEYCODE_V) { paste(); true }
            else false
        }
        expression?.isEnabled=!equalsPending
        if(equalsPending) result?.text=t(R.string.busy) else preview()
    }
    private fun minHeightCompat(view: View, height: Int) { view.minimumHeight=height }
    private fun syncSelection() { expression?.let { session.editor.select(it.selectionStart.coerceAtLeast(0),it.selectionEnd.coerceAtLeast(0)) } }
    private fun bindEditor() {
        val state=session.editor.state
        binding=true; expression?.setText(state.text); expression?.setSelection(state.start.coerceIn(0,state.text.length),state.end.coerceIn(0,state.text.length)); binding=false
    }
    private fun edited() { session.lastSaved=null; status?.text=""; status?.visibility=View.GONE; session.value=null; session.preview=""; result?.text=""; preview() }
    private fun key(key: String) {
        if(deferInput { key(key) }) return
        syncSelection()
        val editor=session.editor
        val before=editor.state.text
        try {
            when(key) {
                "C" -> editor.change(EditState())
                "undo" -> editor.undo()
                "redo" -> editor.redo()
                "=" -> { equals(); return }
                "⌫" -> { val s=editor.state; val a=minOf(s.start,s.end); val b=maxOf(s.start,s.end)
                    if(a!=b) editor.change(EditState(s.text.removeRange(a,b),a))
                    else if(a>0) editor.change(EditState(s.text.removeRange(a-1,a),a-1)) }
                "±" -> if(editor.state.text.isNotBlank()) { val s="-(${editor.state.text})"; if(s.length<=500) editor.change(EditState(s,s.length)) }
                else -> editor.insert(key,locale())
            }
            bindEditor(); if(editor.state.text!=before || key=="C") edited()
        } catch (_: Exception) { notifyError(R.string.input_error) }
    }
    private fun preview() {
        val token=++generation; val text=session.editor.state.text; val calc=engine()
        result?.text=if(text.isNotBlank()) t(R.string.busy) else ""
        work.execute {
            if(dead || generation!=token) return@execute
            val completed=runCatching { calc.complete(text) }.getOrNull()
            runOnUiThread {
                if(dead || generation!=token || session.page!="calculator") return@runOnUiThread
                session.value=completed?.value; session.preview=completed?.let { formatted(it.value) } ?: ""
                result?.text=session.preview
            }
        }
    }
    private fun deferInput(action:MainActivity.()->Unit):Boolean {
        if(!equalsPending) return false
        pendingInput.addLast(action); return true
    }
    override fun dispatchKeyEvent(event:KeyEvent):Boolean {
        if(equalsPending && event.keyCode!=KeyEvent.KEYCODE_BACK) {
            val copy=KeyEvent(event)
            pendingInput.addLast { deliverKey(copy) }
            return true
        }
        return super.dispatchKeyEvent(event)
    }
    private fun deliverKey(event:KeyEvent)=super.dispatchKeyEvent(event)
    private fun equals() {
        if(deferInput { equals() }) return
        syncSelection(); generation++
        val text=session.editor.state.text; val calc=engine()
        equalsPending=true; expression?.isEnabled=false
        result?.text=t(R.string.busy)
        work.execute {
            val completion=runCatching { calc.complete(text) }
            runOnUiThread { session.activity?.finishEquals(text,completion) }
        }
    }
    private fun finishEquals(text:String,completion:Result<CalculatorEngine.Completion?>) {
        if(dead) return
        try {
            val c=completion.getOrNull()
            if(c==null) { notifyError(R.string.calculation_error); result?.text="" }
            else {
                if(c.expression!=text) { session.editor.change(EditState(c.expression,c.expression.length)); bindEditor() }
                session.value=c.value; session.preview=formatted(c.value); result?.text=session.preview; status?.text=""
                if(c.saveable && session.lastSaved!=c.expression) {
                    session.lastSaved=c.expression
                    val entry=HistoryEntry(expression=c.expression,value=c.value.toString())
                    session.history.addFirst(entry); if(session.history.size>20) session.history.removeLast()
                    historyWork { db.history().insert(entry); db.history().prune(settings.days,settings.maximum) }
                }
            }
        } finally {
            equalsPending=false; expression?.isEnabled=true
            while(!equalsPending && pendingInput.isNotEmpty()) pendingInput.removeFirst().invoke(this)
        }
    }
    private fun paste() {
        if(deferInput { paste() }) return
        syncSelection()
        val clip=(getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip ?: return
        if(clip.itemCount==0) return
        try { val before=session.editor.state.text; val inserted=session.editor.insert(clip.getItemAt(0).coerceToText(this).toString(),locale()); bindEditor(); if(session.editor.state.text!=before) edited(); if(inserted.truncated) notifyError(R.string.partial_paste) }
        catch (_: Exception) { notifyError(R.string.input_error) }
    }
    private fun copyResult() {
        val value=session.value ?: return
        // Recurring-decimal classification is delegated to the strict engine.
        val text=engine().copyValue(session.editor.state.text,value,settings.places,settings.rounding)
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(t(R.string.result),text))
    }
    private fun memory(index: Int) {
        if(deferInput { memory(index) }) return
        syncSelection(); val stored=prefs.memory(index)
        if(stored!=null) {
            try { val before=session.editor.state.text; val inserted=session.editor.insert(stored,locale(),true); bindEditor(); if(session.editor.state.text!=before) edited(); if(inserted.truncated) notifyError(R.string.partial_paste) }
            catch (_: Exception) { notifyError(R.string.input_error) }
        } else {
            val value=session.value ?: return
            work.execute { val ok=runCatching { prefs.memory(index,value.toString()) }.getOrDefault(false); runOnUiThread { if(!dead) { if(ok) showPage() else notifyError(R.string.save_error) } } }
        }
    }
    private fun notifyError(id: Int) { status?.visibility=View.VISIBLE; status?.text=t(id); status?.announceForAccessibility(t(id)) }
    private fun navigate(page:String) { if(deferInput { navigate(page) }) return; syncSelection(); generation++; if(session.page=="history") session.deleted=null; session.page=page; showPage() }
    private fun historyWork(block:()->Unit) {
        work.execute { try { block() } catch (_:Exception) { historyError=true; runOnUiThread { if(!dead) notifyError(R.string.history_error) } } }
    }
    private fun pageHeader(title: Int) { row(entries=listOf(t(R.string.back) to { navigate("calculator") })); root.addView(label(t(title),24f)); status=label(""); root.addView(status) }
    private fun historyPage() {
        pageHeader(R.string.history)
        row(entries=listOf(t(R.string.undo_delete) to {
            val entry=session.deleted
            if(entry!=null)
            historyWork { db.history().insert(entry); session.deleted=null; runOnUiThread { if(!dead && session.page=="history") showPage() } }
        }, t(R.string.clear_all) to {
            confirm(R.string.clear_history_question) { historyWork { db.history().clear(); session.deleted=null; runOnUiThread { if(!dead && session.page=="history") showPage() } } }
        }))
        val scroll=ScrollView(this); historyScroller=scroll
        val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; scroll.addView(list); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        fun add(entry:HistoryEntry, permanent:Boolean) {
            list.addView(label(entry.expression+" = "+runCatching { formatted(BigDecimal(entry.value)) }.getOrDefault(entry.value)))
            if(permanent) list.addView(button(t(R.string.delete)) {
                historyWork { db.history().delete(entry.id); session.deleted=entry; runOnUiThread { if(!dead && session.page=="history") showPage() } }
            })
        }
        list.addView(label(t(R.string.session_history))); session.history.forEach { add(it,false) }
        list.addView(label(t(R.string.permanent_history)))
        val token=generation
        historyWork {
            db.history().prune(settings.days,settings.maximum); val entries=db.history().all()
            runOnUiThread { if(!dead && token==generation) { entries.forEach { add(it,true) }; scroll.post { scroll.scrollTo(0,session.historyScroll) } } }
        }
        scroll.setOnScrollChangeListener { _,_,y,_,_ -> session.historyScroll=y }
    }
    private fun confirm(question:Int, action:()->Unit) {
        AlertDialog.Builder(this).setMessage(question).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.confirm) { _,_-> action() }.show()
    }
    private fun settingsPage() {
        pageHeader(R.string.settings)
        val scroll=ScrollView(this); val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; scroll.addView(list); root.addView(scroll)
        fun choice(labelId:Int,values:List<String>,selected:Int,changed:(Int)->Unit) {
            list.addView(label(t(labelId)))
            val spinner=Spinner(this); spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,values)
            spinner.setSelection(selected.coerceIn(values.indices)); var ready=false
            spinner.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent:AdapterView<*>?) {}
                override fun onItemSelected(parent:AdapterView<*>?,v:View?,position:Int,id:Long) { if(ready && position!=selected) changed(position); ready=true }
            }; list.addView(spinner)
        }
        choice(R.string.language,listOf("ไทย","English"),if(settings.language=="th") 0 else 1) { saveSettings(pendingSettings.copy(language=if(it==0) "th" else "en")) }
        val themes=listOf("system","light","dark")
        choice(R.string.theme,listOf(t(R.string.system),t(R.string.light),t(R.string.dark)),themes.indexOf(settings.theme)) { saveSettings(pendingSettings.copy(theme=themes[it])) }
        choice(R.string.places,(0..20).map(Int::toString),settings.places) { saveSettings(pendingSettings.copy(places=it)) }
        val rounding=listOf(RoundingMode.HALF_UP,RoundingMode.UP,RoundingMode.DOWN)
        choice(R.string.rounding,rounding.map { it.name },rounding.indexOf(settings.rounding)) { saveSettings(pendingSettings.copy(rounding=rounding[it])) }
        val days=listOf(7,30,90); choice(R.string.days,days.map(Int::toString),days.indexOf(settings.days)) { saveSettings(pendingSettings.copy(days=days[it])) }
        val max=listOf(100,300,500); choice(R.string.maximum,max.map(Int::toString),max.indexOf(settings.maximum)) { saveSettings(pendingSettings.copy(maximum=max[it])) }
        list.addView(button(t(R.string.reset_settings)) { confirm(R.string.reset_question) { saveSettings(Settings()) } })
    }
    private fun saveSettings(next:Settings) {
        pendingSettings=next
        val token=++session.settingsGeneration
        work.execute {
            val ok=runCatching { prefs.save(next) }.getOrDefault(false)
            runOnUiThread { session.activity?.finishSettingsSave(next,ok,token) }
        }
    }
    private fun finishSettingsSave(next:Settings,ok:Boolean,token:Long) {
        if(dead) return
        if(ok) settings=next
        if(token!=session.settingsGeneration) return
        if(ok) { pendingSettings=next; applyLanguage(); showPage() }
        else { pendingSettings=settings; notifyError(R.string.save_error) }
    }
    private fun aboutPage() { pageHeader(R.string.about); val scroll=ScrollView(this); scroll.addView(label(t(R.string.about_text))); root.addView(scroll) }
    @Deprecated("Platform Back supports API 28") override fun onBackPressed() {
        if(session.page!="calculator") navigate("calculator") else { syncSelection(); moveTaskToBack(true) }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        syncSelection(); outState.putInt("orientation",resources.configuration.orientation)
        super.onSaveInstanceState(outState)
    }
    override fun onRetainNonConfigurationInstance(): Any { syncSelection(); return resources.configuration.orientation }
    override fun onDestroy() {
        syncSelection(); dead=true; generation++; work.shutdown()
        // Queued DB writes complete; close only after them.
        Thread { while(!work.awaitTermination(1,java.util.concurrent.TimeUnit.SECONDS)) { }; db.close() }.start()
        if(session.activity===this) session.activity=null
        if(isFinishing) sessions.remove(taskId)
        super.onDestroy()
    }
}
