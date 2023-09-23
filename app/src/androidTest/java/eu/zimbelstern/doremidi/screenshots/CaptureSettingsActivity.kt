package eu.zimbelstern.doremidi.screenshots

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import eu.zimbelstern.doremidi.SettingsActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

class CaptureSettingsActivity {

	@Rule
	@JvmField
	val localeTestRule = LocaleTestRule()

	@Before
	fun setUp() {
		ActivityScenario.launch(SettingsActivity::class.java)
	}

	@Test
	fun captureSettingsView() {
		onView(withId(android.R.id.content)).check(matches(isDisplayed()))
		Screengrab.screenshot("1")
	}

}