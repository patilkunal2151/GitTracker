package com.example.gittracker.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.example.gittracker",
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        
        // Find the repository list and scroll it
        val repoList = device.findObject(By.scrollable(true))
        repoList?.let {
            it.setGestureMargin(device.displayWidth / 5)
            it.fling(Direction.DOWN)
            device.waitForIdle()
            it.fling(Direction.UP)
        }
    }
}
