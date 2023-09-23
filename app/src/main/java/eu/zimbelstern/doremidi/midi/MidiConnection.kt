package eu.zimbelstern.doremidi.midi

import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.util.Log

/**
 * A MidiConnection comprises a [MidiDeviceInfo](android.media.midi#MidiDeviceInfo),
 * a [MidiOutputPort](android.media.midi#MidiOutputPort) and
 * a connected [MidiReceiver](android.media.midi#MidiReceiver).
 *
 * The receiver can be disconnected from the output port with [stopReceiving].
 */
class MidiConnection(
	val deviceInfo: MidiDeviceInfo,
	private val outputPort: MidiOutputPort,
	private val receiver: MidiReceiver
) {

	companion object {
		const val TAG = "MidiConnection"
	}

	fun stopReceiving() {
		try {
			outputPort.let {
				it.disconnect(receiver)
				it.close()
				Log.d(TAG, "Stopped receiver $receiver on device $deviceInfo")
			}
		} catch (_: Exception) {
			Log.e(TAG, "Error stopping receiver $receiver on device $deviceInfo")
		}
	}

}