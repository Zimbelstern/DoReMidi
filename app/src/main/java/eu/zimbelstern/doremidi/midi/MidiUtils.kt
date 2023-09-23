package eu.zimbelstern.doremidi.midi

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Build
import android.util.Log
import eu.zimbelstern.doremidi.R

/**
 * A utility class with static methods for working with MIDI data.
 * */
class MidiUtils {

	companion object {

		private const val TAG = "MidiUtils"
		private const val PREF_MIDI_FILTER_ACTION = "PREF_MIDI_FILTER_ACTION"
		private const val PREF_MIDI_FILTER_STATUS = "PREF_MIDI_FILTER_STATUS"
		private const val PREF_MIDI_FILTER_CHANNEL = "PREF_MIDI_FILTER_CHANNEL"
		private const val PREF_MIDI_FILTER_DATA1 = "PREF_MIDI_FILTER_DATA1"
		private const val PREF_MIDI_FILTER_DATA2 = "PREF_MIDI_FILTER_DATA2"
		private const val PREF_MIDI_FILTER_DATA2_OPERATOR = "PREF_MIDI_FILTER_DATA2_OPERATOR"
		private const val SOSTENUTO_PEDAL = "SOSTENUTO_PEDAL"
		private const val SOFT_PEDAL = "SOFT_PEDAL"
		const val CUSTOM = "CUSTOM"

		val statusNames = mapOf(
			0x80 to "Note Off (0x80)",
			0x90 to "Note On (0x90)",
			0xA0 to "Polyphonic Aftertouch (0xA0)",
			0xB0 to "Control Change (0xB0)",
			0xC0 to "Program Change (0xC0)",
			0xD0 to "Monophonic/Channel Aftertouch (0xD0)",
			0xE0 to "Pitch Bending (0xE0)",
			0xF0 to "System Message (0xF0)",
		)

		/**
		 * Preset [MidiFilters](MidiFilter) for common use cases.
		 * */
		val filters = listOf(
			MidiFilter(SOFT_PEDAL, 0xB0, -1, 67, 64, 1),
			MidiFilter(SOSTENUTO_PEDAL, 0xB0, -1, 66, 64, 1)
		)
		val filterActions = filters.map { it.action }

		val actionStringIds = mapOf(
			SOFT_PEDAL to R.string.soft_pedal,
			SOSTENUTO_PEDAL to R.string.sostenuto_pedal,
			CUSTOM to R.string.custom
		)

		/**
		 * Reads a ByteArray of midi data and returns a list of [MidiEvents](MidiEvent);
		 * a system message aborts the collecting of events.
		 * */
		fun readMidiByteArray(msg: ByteArray, startOffset: Int, startCount: Int, timestamp: Long): List<MidiEvent> {
			fun Byte.toUnsigned() = if (this < 0) 256 + this else this.toInt()
			val events = mutableListOf<MidiEvent>()
			var offset = startOffset
			var count = startCount
			var statusByte = 0

			while (count > 0) {
				val nextByte = msg[offset].toUnsigned()

				if (msg[offset].toUnsigned() >= 0x80) {
					statusByte = nextByte
					offset++
					count--
				}

				val status = statusByte and 0xF0
				val channel = statusByte and 0x0F

				if (status == 0xF0) {
					Log.v(TAG, "$timestamp – ${statusNames[status]}")
					break
				}

				val data1 = msg[offset].toUnsigned()
				val data2 = if (status < 0xC0 || status == 0xE0) msg[offset+1].toUnsigned() else null

				events.add(MidiEvent(status, channel, data1, data2))

				offset += if (status < 0xC0 || status == 0xE0) 1 else 2
				count -= if (status < 0xC0 || status == 0xE0) 1 else 2
			}

			for (event in events) {
				Log.v(TAG, "$timestamp – $event")
			}

			return events.toList()
		}

		/**
		 * Retrieves a [MidiFilter] from the app's shared preferences.
		 * Needs a fallback for the case that there is no saved filter in the preferences.
		 * */
		fun getMidiFilterFromPreferences(sharedPrefs: SharedPreferences, fallback: MidiFilter): MidiFilter {
			return try {
				MidiFilter(
					sharedPrefs.getString(PREF_MIDI_FILTER_ACTION, null) ?: throw Error(),
					sharedPrefs.getInt(PREF_MIDI_FILTER_STATUS, -1),
					sharedPrefs.getInt(PREF_MIDI_FILTER_CHANNEL, -1),
					sharedPrefs.getInt(PREF_MIDI_FILTER_DATA1, -1),
					sharedPrefs.getInt(PREF_MIDI_FILTER_DATA2, -1),
					sharedPrefs.getInt(PREF_MIDI_FILTER_DATA2_OPERATOR, -1)
				)
			} catch (_: Error) {
				fallback
			}
		}

		/**
		 * Saves a [MidiFilter] in the app's shared preferences.
		 * */
		fun putMidiFilterInPreferences(sharedPrefs: SharedPreferences, result: MidiFilter) {
			sharedPrefs.edit()
				.putString(PREF_MIDI_FILTER_ACTION, result.action)
				.putInt(PREF_MIDI_FILTER_STATUS, result.status)
				.putInt(PREF_MIDI_FILTER_CHANNEL, result.channel)
				.putInt(PREF_MIDI_FILTER_DATA1, result.data1)
				.putInt(PREF_MIDI_FILTER_DATA2, result.data2)
				.putInt(PREF_MIDI_FILTER_DATA2_OPERATOR, result.operator)
				.apply()
		}

		/**
		 * Short way to call get the devices from [MidiManager] with [MidiManager.TRANSPORT_MIDI_BYTE_STREAM]
		 * on all APIs.
		 * */
		@SuppressLint("WrongConstant")
		fun MidiManager.getDevicesWithType(type: Int): List<MidiDeviceInfo> {
			return if (Build.VERSION.SDK_INT >= 33) {
				getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM)
			} else {
				@Suppress("DEPRECATION")
				devices.toList()
			}.filter { it.type == type }
		}

	}

}