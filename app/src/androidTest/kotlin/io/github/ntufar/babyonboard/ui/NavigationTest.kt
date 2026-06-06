package io.github.ntufar.babyonboard.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ntufar.babyonboard.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunches_showsOnboardingScreen() {
        composeTestRule.onNodeWithText("Baby on Board").assertExists()
        composeTestRule.onNodeWithText("Safe-Driving Telemetry for Families").assertExists()
    }

    @Test
    fun onboardingScreen_showsGetStartedButton() {
        composeTestRule.onNodeWithText("Get Started").assertExists()
    }

    @Test
    fun onboardingScreen_showsBabyModeToggle() {
        composeTestRule.onNodeWithText("Baby Mode").assertExists()
    }

    @Test
    fun onboardingScreen_showsHonestLimitsCard() {
        composeTestRule.onNodeWithText("Honest Limits").assertExists()
    }
}
