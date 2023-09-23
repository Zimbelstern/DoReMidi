package eu.zimbelstern.doremidi

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import eu.zimbelstern.doremidi.SettingsActivity.Companion.KEY_PAGE_BACK
import eu.zimbelstern.doremidi.SettingsActivity.Companion.KEY_PAGE_FORWARD
import eu.zimbelstern.doremidi.SettingsActivity.Companion.PREFS_GENERAL
import eu.zimbelstern.doremidi.SettingsActivity.Companion.PREF_DEVICE_TYPE
import eu.zimbelstern.doremidi.midi.MidiConnection
import eu.zimbelstern.doremidi.midi.MidiFilter
import eu.zimbelstern.doremidi.midi.MidiUtils
import eu.zimbelstern.doremidi.midi.MidiUtils.Companion.getDevicesWithType

/**
 * Activity that displays a document and manages MIDI connections according to the preferences.
 * Certain [MidiFilters](MidiFilter) trigger [goForward] and [goBackward] of the superclass.
 * */
class MusicSheetActivity : com.artifex.mupdf.mini.DocumentActivity() {

	companion object {
		private const val TAG = "MusicSheetActivity"
	}

	private lateinit var midiManager: MidiManager
	private lateinit var midiControls: LinearLayout
	private lateinit var midiStatusButton: ImageButton
	private lateinit var midiSettingsButton: ImageButton

	private val midiConnections = mutableListOf<MidiConnection>()


	@SuppressLint("WrongConstant")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val rootView = (findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as RelativeLayout)
		layoutInflater.inflate(R.layout.midi_controls, rootView)

		midiManager = (getSystemService(Context.MIDI_SERVICE) as MidiManager)

		midiControls = findViewById(R.id.midi_controls)

		midiStatusButton = findViewById<ImageButton?>(R.id.midi_status).also {
			it.setOnClickListener { showMidiDeviceInfo() }
		}

		midiSettingsButton = findViewById<ImageButton?>(R.id.midi_settings).also {
			it.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
		}

		val deviceCallback = object : MidiManager.DeviceCallback() {
			override fun onDeviceAdded(device: MidiDeviceInfo) {
				Log.i(TAG, "Device added: $device")
				reStartMidiDevices()
			}
			override fun onDeviceRemoved(device: MidiDeviceInfo) {
				Log.i(TAG, "Device removed: $device")
				reStartMidiDevices()
			}
		}

		if (Build.VERSION.SDK_INT >= 33) {
			midiManager.registerDeviceCallback(
				MidiManager.TRANSPORT_MIDI_BYTE_STREAM,
				ContextCompat.getMainExecutor(this),
				deviceCallback
			)
		} else {
			@Suppress("DEPRECATION")
			midiManager.registerDeviceCallback(
				deviceCallback,
				Handler(Looper.getMainLooper())
			)
		}

	}

	override fun onStart() {
		super.onStart()
		reStartMidiDevices()
	}

	override fun onStop() {
		stopMidiDevices()
		super.onStop()
	}

	private fun updateMidiStatusButton() {
		midiStatusButton.apply {
			isActivated = midiConnections.isNotEmpty()
			alpha = if (isActivated) 1f else 0.25f
		}
	}

	private fun showMidiDeviceInfo() {
		if (midiConnections.isNotEmpty()) {
			var info = ""
			for (connection in midiConnections) {
				info += connection.deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
				info += "\n"
			}
			AlertDialog.Builder(this)
				.setTitle(getString(R.string.connected_midi_devices))
				.setMessage(info)
				.setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
				.create()
				.show()
		}
		else {
			Toast.makeText(this, R.string.no_devices_connected, Toast.LENGTH_LONG).show()
		}
	}

	@SuppressLint("WrongConstant")
	fun reStartMidiDevices() {
		stopMidiDevices()
		// Adds all midi devices of a given type to activeMidiDevices and start new receivers
		val devices = midiManager.getDevicesWithType(
			getSharedPreferences(PREFS_GENERAL, Context.MODE_PRIVATE).getInt(PREF_DEVICE_TYPE, MidiDeviceInfo.TYPE_USB)
		)
		val receiver = DoReMidiReceiver()
		for (device in devices) {
			midiManager.openDevice(device, {
				val port = it.openOutputPort(0)
				midiConnections.add(MidiConnection(device, port, receiver))
				updateMidiStatusButton()
				port.connect(receiver)
				Log.d(TAG, "Started receiver $receiver on device $device")
			}, Handler(Looper.getMainLooper()))
		}
	}

	private fun stopMidiDevices() {
		// Removes receiver for devices in activeMidiDevices and clears the list
		midiConnections.forEach {
			it.stopReceiving()
		}
		midiConnections.clear()
		updateMidiStatusButton()
	}

	// Midi navbar
	override fun toggleUI() {
		super.toggleUI()
		midiControls.layoutParams = RelativeLayout.LayoutParams(midiControls.layoutParams).apply {
			setMargins(0, if (navigationBar.visibility == View.VISIBLE) (48 * resources.displayMetrics.density).toInt() else 0, 0, 0)
		}
	}

	inner class DoReMidiReceiver : MidiReceiver() {

		private val filterForward: MidiFilter = MidiUtils.getMidiFilterFromPreferences(getSharedPreferences(
			BuildConfig.APPLICATION_ID + "_" + KEY_PAGE_FORWARD,
			Context.MODE_PRIVATE
		), MidiUtils.filters[0])

		private val filterBack: MidiFilter = MidiUtils.getMidiFilterFromPreferences(getSharedPreferences(
			BuildConfig.APPLICATION_ID + "_" + KEY_PAGE_BACK,
			Context.MODE_PRIVATE
		), MidiUtils.filters[1])

		override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
			val event = MidiUtils.readMidiByteArray(msg, offset, count, timestamp)

			if (event.any { filterForward.matches(it) }) {
				runOnUiThread {
					pageView.goForward()
				}
			}

			if (event.any { filterBack.matches(it) }) {
				runOnUiThread {
					pageView.goBackward()
				}
			}
		}

	}

}