package eu.zimbelstern.doremidi.screenshots

import android.R
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import eu.zimbelstern.doremidi.FilterSetActivity
import eu.zimbelstern.doremidi.FilterSetActivity.Companion.EXTRA_MIDI_FILTER
import eu.zimbelstern.doremidi.midi.MidiUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

class CaptureFilterSetActivity {

	@Rule
	@JvmField
	val localeTestRule = LocaleTestRule()

	@Before
	fun setUp() {
		ActivityScenario.launch<FilterSetActivity>(Intent(ApplicationProvider.getApplicationContext(), FilterSetActivity::class.java).apply {
			putExtra(EXTRA_MIDI_FILTER, MidiUtils.filters[0].copy(action = MidiUtils.CUSTOM))
		})

	}

	@Test
	fun captureSettingsView() {
		Espresso.onView(ViewMatchers.withId(R.id.content)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
		Screengrab.screenshot("2")
	}

}