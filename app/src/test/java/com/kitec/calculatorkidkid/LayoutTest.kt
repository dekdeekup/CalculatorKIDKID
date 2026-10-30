package com.kitec.calculatorkidkid

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w360dp-h640dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayoutTest {
    private fun children(v:View):List<View> = listOf(v)+(if(v is ViewGroup) (0 until v.childCount).flatMap { children(v.getChildAt(it)) } else emptyList())
    @Test fun portraitControlsFitAndProduceReviewImage() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); val a=controller.get()
        val content=a.findViewById<View>(android.R.id.content)
        content.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY)); content.layout(0,0,360,640)
        val buttons=children(content).filterIsInstance<Button>()
        assertEquals(31,buttons.size)
        for(b in buttons) {
            val point=IntArray(2); b.getLocationInWindow(point)
            assertTrue("${b.text} is above viewport",point[1]>=0)
            assertTrue("${b.text} is below viewport",point[1]+b.height<=640)
            assertTrue("${b.text} touch height",b.height>=48)
        }
        val bitmap=Bitmap.createBitmap(360,640,Bitmap.Config.ARGB_8888); content.draw(Canvas(bitmap))
        val out=File("build/reports/screenshots"); out.mkdirs()
        File(out,"portrait.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
        controller.pause().stop().destroy()
    }
    @Test @Config(qualifiers="w640dp-h360dp-land-mdpi")
    fun landscapeControlsFitWithoutKeypadScrolling() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); val a=controller.get()
        val content=a.findViewById<View>(android.R.id.content)
        content.measure(View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY)); content.layout(0,0,640,360)
        for(b in children(content).filterIsInstance<Button>()) {
            val point=IntArray(2); b.getLocationInWindow(point)
            assertTrue("${b.text} below landscape viewport",point[1]+b.height<=360)
            assertTrue("${b.text} touch height",b.height>=48)
        }
        val bitmap=Bitmap.createBitmap(640,360,Bitmap.Config.ARGB_8888); content.draw(Canvas(bitmap))
        val out=File("build/reports/screenshots"); out.mkdirs()
        File(out,"landscape.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
        controller.pause().stop().destroy()
    }
    @Test fun largeFontsKeepKeypadReachableByScrolling() {
        val app=org.robolectric.RuntimeEnvironment.getApplication()
        val config=android.content.res.Configuration(app.resources.configuration); config.fontScale=2f
        @Suppress("DEPRECATION") app.resources.updateConfiguration(config,app.resources.displayMetrics)
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup(); val a=controller.get()
        val content=a.findViewById<View>(android.R.id.content)
        content.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY)); content.layout(0,0,360,640)
        val outer=children(content).filterIsInstance<android.widget.ScrollView>().first()
        outer.fullScroll(View.FOCUS_DOWN)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        val equals=children(content).filterIsInstance<Button>().single { it.text.toString()=="=" }
        val point=IntArray(2); equals.getLocationInWindow(point)
        assertTrue("large-font equals is reachable",point[1]>=0 && point[1]+equals.height<=640)
        assertTrue(equals.height>=96)
        controller.pause().stop().destroy()
    }
}
