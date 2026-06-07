package com.example.aiadflow

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Android 仪器测试示例。
 * 这类测试运行在真机或模拟器上，可以访问 Android 运行环境。
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    /** 验证测试环境中获取到的应用包名正确。 */
    @Test
    fun useAppContext() {
        // 获取被测应用的 Context。
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.aiadflow", appContext.packageName)
    }
}
