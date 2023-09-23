package eu.zimbelstern.doremidi.midi

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.math.sign

/**
 * A set of bytes to filter [MIDIEvents](MidiEvent).
 * */
@Parcelize
data class MidiFilter(

	/** Descriptive name of the event to be filtered */
	var action: String,

	/** Filter for [MidiEvent.status]; between 0x80 and 0xF0 */
	var status: Int,

	/** Filter for [MidiEvent.channel]; between 0 and 15 or -1 for all channels */
	var channel: Int,

	/** Filter for [MidiEvent.data1]; between 0x00 and 0x7F */
	var data1: Int,

	/** Filter for [MidiEvent.data2]; be between 0x00 and 0x7F or -1 if not present */
	var data2: Int,

	/** Operator for comparison of the [data2]; -1 for inclusion of lower values, 1 for inclusion of greater values, 0 for equality */
	var operator: Int

) : Parcelable {

	fun matches(event: MidiEvent): Boolean {
		return event.status == this.status
				&& (this.channel == -1 || event.channel == this.channel)
				&& event.data1 == this.data1
				&& (this.data2 == -1 || event.data2 == this.data2 || (event.data2?.minus(this.data2))?.sign == this.operator)
	}

}
