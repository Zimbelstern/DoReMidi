package eu.zimbelstern.doremidi.midi

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A MidiEvent is any message of the MIDI protocol consisting of a status byte (status and channel)
 * and one or two data bytes. System messages (status >= 0xF0) are not implemented (yet).
 * */
@Parcelize
data class MidiEvent(

	/** High nibble of the status byte (AND 0xF0); should be between 0x80 and 0xF0 */
	val status: Int,

	/** Low nibble of the status byte (AND 0x0F); should be between 0 and 15 */
	val channel: Int,

	/** First data byte; should be between 0x00 and 0x7F (0-255) */
	val data1: Int,

	/** Second data byte; should be between 0x00 and 0x7F (0-255) if present */
	val data2: Int?,

) : Parcelable