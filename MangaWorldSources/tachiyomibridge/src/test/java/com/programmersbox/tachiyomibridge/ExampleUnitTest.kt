package com.programmersbox.tachiyomibridge

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
        println(Locale.getDefault().toLanguageTag())
    }
}