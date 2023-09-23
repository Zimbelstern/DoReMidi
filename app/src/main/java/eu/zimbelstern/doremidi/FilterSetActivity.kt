package eu.zimbelstern.doremidi

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.google.android.material.textfield.TextInputLayout
import eu.zimbelstern.doremidi.SettingsActivity.Companion.PREFS_GENERAL
import eu.zimbelstern.doremidi.databinding.FilterSetActivityBinding
import eu.zimbelstern.doremidi.midi.MidiEvent
import eu.zimbelstern.doremidi.midi.MidiFilter
import eu.zimbelstern.doremidi.midi.MidiUtils
import eu.zimbelstern.doremidi.midi.MidiUtils.Companion.CUSTOM
import eu.zimbelstern.doremidi.midi.MidiUtils.Companion.actionStringIds
import eu.zimbelstern.doremidi.midi.MidiUtils.Companion.getDevicesWithType

/**
 * Activity called from [SettingsActivity] that lets the user set a [MidiFilter] returns the result.
 * In addition, any [MidiEvents](MidiEvent) are displayed to the user.
 * */
class FilterSetActivity : AppCompatActivity(R.layout.filter_set_activity) {

	private lateinit var binding: FilterSetActivityBinding
	private lateinit var filter: MidiFilter

	companion object {
		const val EXTRA_MIDI_FILTER = "EXTRA_MIDI_FILTER"
	}

	@SuppressLint("WrongConstant")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = FilterSetActivityBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		supportActionBar?.apply {
			title = getString(R.string.set_filter)
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(true)
		}

		filter = if (Build.VERSION.SDK_INT >= 33) {
			savedInstanceState?.getParcelable(EXTRA_MIDI_FILTER, MidiFilter::class.java) ?:
			intent.getParcelableExtra(EXTRA_MIDI_FILTER, MidiFilter::class.java)!!
		} else {
			@Suppress("DEPRECATION")
			savedInstanceState?.getParcelable(EXTRA_MIDI_FILTER) ?:
			intent.getParcelableExtra(EXTRA_MIDI_FILTER)!!
		}

		binding.action.let {
			// Takes predefined Midi Filter actions, adds "CUSTOM" and gets the localized strings
			it.setAdapter(DropdownArrayAdapter((MidiUtils.filterActions + CUSTOM).map { action ->
				getString(actionStringIds[action] ?: R.string.unknown)
			}))
			it.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
				filter.action = MidiUtils.filterActions.getOrNull(position) ?: CUSTOM
				if (position < MidiUtils.filters.size) {
					filter = MidiUtils.filters[position]
				}
				setValues(filter)
			}
		}

		binding.status.let {
			// Takes Midi status names except for 0xF0 system message
			it.setAdapter(DropdownArrayAdapter(MidiUtils.statusNames.values.toList().dropLast(1)))
			it.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
				filter.status = 0x80 + position * 0x10
				setValues(filter)
			}
		}

		binding.channel.let {
			// Channels from 0 to 15 plus * for all
			it.setAdapter(DropdownArrayAdapter(listOf("*") + (0..15)))
			it.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
				filter.channel = position - 1
			}
		}

		binding.data1.let {
			// Byte values from 0 to 127
			it.setAdapter(DropdownArrayAdapter((0..127).toList()))
			it.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
				filter.data1 = position
			}
		}

		binding.data2.let {
			// Byte values from 0 to 127
			it.setAdapter(DropdownArrayAdapter((0..127).toList()))
			it.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
				filter.data2 = position
			}
		}

		binding.operator.let {
			// lower than, equals, greater than
			it.setAdapter(DropdownArrayAdapter(listOf("\u2264", "=", "\u2265")))
			it.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
				filter.operator = position - 1
			}
		}

		binding.save.setOnClickListener {
			setResult(Activity.RESULT_OK, Intent().apply {
				putExtra(EXTRA_MIDI_FILTER, filter)
			})
			finish()
		}

		setValues(filter)

		with (getSystemService(Context.MIDI_SERVICE) as MidiManager) {
			for (device in getDevicesWithType(
				getSharedPreferences(PREFS_GENERAL, Context.MODE_PRIVATE)
					.getInt(SettingsActivity.PREF_DEVICE_TYPE, MidiDeviceInfo.TYPE_USB)
			)) {
				openDevice(device, {
					it.openOutputPort(0).connect(object : MidiReceiver() {
						var lastTime: Long? = null
						override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
							val events = MidiUtils.readMidiByteArray(msg, offset, count, timestamp)
							runOnUiThread {
								if (events.isNotEmpty()) {
									if (System.currentTimeMillis() - (lastTime ?: System.currentTimeMillis()) > 1000) {
										binding.log.text = ""
									}
									for (event in events) {
										binding.log.text = binding.log.text.lines().take(100).joinToString("\n", "${event.toText()}\n")
									}
									lastTime = System.currentTimeMillis()
								}
							}
						}
					})
				}, Handler(Looper.getMainLooper()))
			}
		}

	}

	@SuppressLint("MissingSuperCall")
	override fun onSaveInstanceState(outState: Bundle) {
		outState.putParcelable(EXTRA_MIDI_FILTER, filter)
		super.onSaveInstanceState(outState)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return if (item.itemId == android.R.id.home) {
			finish()
			true
		} else
			super.onOptionsItemSelected(item)
	}

	// Enables or disables all layouts in LinearLayouts recursively
	private fun LinearLayout.setEnabledRecursively(boolean: Boolean) {
		for (child in children) {
			if (child is LinearLayout && child !is TextInputLayout)
				child.setEnabledRecursively(boolean)
			else
				child.isEnabled = boolean
		}
	}

	private fun setValues(filter: MidiFilter) {
		val isCustom = filter.action !in MidiUtils.filterActions
		binding.action.setText(getString(actionStringIds[filter.action] ?: R.string.unknown), false)

		// Enables or disables the custom settings
		binding.customSettings.setEnabledRecursively(isCustom)
		binding.status.setText(MidiUtils.statusNames[filter.status], false)
		binding.channel.setText((listOf("*") + (0..15))[filter.channel + 1].toString(), false)
		binding.data1.setText(filter.data1.toString(), false)

		binding.data2.isEnabled = isCustom && filter.status != 0xC0 && filter.status != 0xD0
		binding.operator.isEnabled = isCustom && filter.status != 0xC0 && filter.status != 0xD0

		if (filter.data2 != -1) {
			binding.data2.setText(filter.data2.toString(), false)
			binding.operator.setText(listOf("\u2264", "=", "\u2265")[filter.operator + 1], false)
		}

	}

	private fun MidiEvent.toText(): String {
		return MidiUtils.statusNames[status] + "; " +
				getString(R.string.channel) + ": " + channel + "; " +
				getString(R.string.data_byte_1) + ": " + data1 + "; " +
				getString(R.string.data_byte_2) + ": " + data2
	}

	inner class DropdownArrayAdapter<T>(var items: List<T>) : ArrayAdapter<T>(this, R.layout.dropdown_item, items) {

		private val filter: Filter = object : Filter() {

			override fun performFiltering(charSequence: CharSequence): FilterResults {
				return FilterResults().apply {
					values = items
					count = items.size
				}
			}

			override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
				notifyDataSetChanged()
			}

		}

		override fun getFilter(): Filter {
			return filter
		}

	}

}