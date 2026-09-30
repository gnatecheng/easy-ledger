package com.qingjizhang.app

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = QingJiZhangApp::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MainActivityLaunchTest {

    @Test
    fun mainActivityCreate_doesNotThrow() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
        controller.get()
        controller.pause().stop().destroy()
    }
}
