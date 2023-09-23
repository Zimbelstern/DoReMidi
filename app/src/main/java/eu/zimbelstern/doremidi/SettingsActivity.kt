package eu.zimbelstern.doremidi

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.midi.MidiDeviceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import eu.zimbelstern.doremidi.FilterSetActivity.Companion.EXTRA_MIDI_FILTER
import eu.zimbelstern.doremidi.midi.MidiFilter
import eu.zimbelstern.doremidi.midi.MidiUtils

/**
 * Activity for changing preferences and viewing information about the app.
 * Clicking on a page turn trigger preference will start the [FilterSetActivity];
 * the returned result is saved in the preferences.
 * */
class SettingsActivity : AppCompatActivity() {

	companion object {
		const val PREFS_GENERAL = BuildConfig.APPLICATION_ID + "_preferences"
		const val KEY_PAGE_FORWARD = "page_forward"
		const val KEY_PAGE_BACK = "page_back"
		const val PREF_DEVICE_TYPE = "PREF_DEVICE_TYPE"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		@SuppressLint("CommitTransaction")
		if (savedInstanceState == null) {
			supportFragmentManager
				.beginTransaction()
				.replace(android.R.id.content, SettingsFragment())
				.commit()
		}

		supportActionBar?.apply {
			title = getString(R.string.settings)
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(true)
		}
	}

	class SettingsFragment : PreferenceFragmentCompat() {

		private val filterSetResultLaunchers = (0..1).map { position ->
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
				if (it.resultCode == Activity.RESULT_OK) {
					val key = listOf(KEY_PAGE_FORWARD, KEY_PAGE_BACK)[position]
					val filter = if (Build.VERSION.SDK_INT >= 33) {
						it.data?.getParcelableExtra(EXTRA_MIDI_FILTER, MidiFilter::class.java)
					}
					else {
						@Suppress("DEPRECATION")
						it.data?.getParcelableExtra(EXTRA_MIDI_FILTER)
					}
					if (filter != null) {
						MidiUtils.putMidiFilterInPreferences(requireActivity().getSharedPreferences(
							BuildConfig.APPLICATION_ID + "_$key",
							Context.MODE_PRIVATE
						), filter)
						updateFilterPref(position)
					}
				}
			}
		}

		private var egg = 1

		@SuppressLint("CommitPrefEdits")
		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			setPreferencesFromResource(R.xml.preferences, rootKey)

			val sharedPrefs = requireActivity().getSharedPreferences(
				PREFS_GENERAL,
				Context.MODE_PRIVATE
			)

			findPreference<ListPreference>("device_type")?.apply {
				val options = arrayOf(MidiDeviceInfo.TYPE_USB, MidiDeviceInfo.TYPE_BLUETOOTH, MidiDeviceInfo.TYPE_VIRTUAL)
				entryValues = arrayOf("USB", "BLUETOOTH", "VIRTUAL")
				setDefaultValue(entryValues[0])

				val deviceType = sharedPrefs.getInt(PREF_DEVICE_TYPE, MidiDeviceInfo.TYPE_USB)
				summary = context.resources.getStringArray(R.array.device_type_options)[options.indexOf(deviceType)]
				value = entryValues[options.indexOf(deviceType)].toString()

				setOnPreferenceChangeListener { _, value ->
					sharedPrefs
						.edit()
						.putInt(PREF_DEVICE_TYPE, options[entryValues.indexOf(value)])
						.apply()
					summary = context.resources.getStringArray(R.array.device_type_options)[entryValues.indexOf(value)]
					true
				}
			}

			updateFilterPref(0)
			updateFilterPref(1)

			findPreference<PreferenceCategory>("about")?.title = getString(R.string.about_app_name, getString(R.string.app_name))
			findPreference<Preference>("version")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
				if (egg < 3) egg++ else Toast.makeText(context, "Note implemented yet! ♪", Toast.LENGTH_LONG).show()
				true
			}
			for (key in listOf("", "license", "source", "issues", "donate")) {
				findPreference<Preference>(key)?.onPreferenceClickListener = preferenceKeyAsUrl()
			}

		}

		private fun updateFilterPref(position: Int) {
			val key = listOf(KEY_PAGE_FORWARD, KEY_PAGE_BACK)[position]
			findPreference<Preference>(key)?.apply {
				val filter = MidiUtils.getMidiFilterFromPreferences(
					requireActivity().getSharedPreferences(
						BuildConfig.APPLICATION_ID + "_$key",
						Context.MODE_PRIVATE
					),
					MidiUtils.filters[position])
				summary = filter.toText()
				onPreferenceClickListener = Preference.OnPreferenceClickListener {
					filterSetResultLaunchers[position].launch(Intent(activity, FilterSetActivity::class.java).apply {
						putExtra(EXTRA_MIDI_FILTER, filter)
					})
					true
				}
			}
		}

		private fun preferenceKeyAsUrl() = Preference.OnPreferenceClickListener {
			actionViewUri("https://doremidi.zimbelstern.eu/${it.key}")
			true
		}

		private fun actionViewUri(uriString: String) {
			val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
			try {
				startActivity(intent)
			} catch (_: ActivityNotFoundException) {
				Toast.makeText(context, uriString, Toast.LENGTH_LONG).show()
			}
		}

		private fun MidiFilter.toText(): String {
			return getString(MidiUtils.actionStringIds[action] ?: R.string.unknown) + "\n" +
					MidiUtils.statusNames[status] + "; " +
					getString(R.string.channel) + ": " + (channel.takeIf { it != -1 } ?: "*") + "\n" +
					getString(R.string.data_byte_1) + ": " + data1 + "; " +
					getString(R.string.data_byte_2) + ": " + listOf("\u2264", "", "\u2265")[operator + 1] + data2
		}


	}


	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return if (item.itemId == android.R.id.home) {
			finish()
			true
		} else
			super.onOptionsItemSelected(item)
	}

}