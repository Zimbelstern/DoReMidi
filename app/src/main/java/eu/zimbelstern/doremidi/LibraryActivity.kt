package eu.zimbelstern.doremidi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * This activity launches an ActivityResultLauncher with [ActivityResultContracts.OpenDocument]
 * for document choosing and starts the [MusicSheetActivity] with the received result URI.
 * */
class LibraryActivity : AppCompatActivity() {

	private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
		if (it != null) {
			startActivity(Intent(this, MusicSheetActivity::class.java).apply {
				action = Intent.ACTION_VIEW
				setDataAndType(it, contentResolver.getType(it))
				addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
				putExtra(componentName.packageName + ".ReturnToLibraryActivity", 1)
			})
		}
		finish()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		Toast.makeText(this, getString(R.string.welcome), Toast.LENGTH_LONG).show()

		activityResultLauncher.launch(
			arrayOf(
				"application/pdf",
				"application/vnd.ms-xpsdocument",
				"application/oxps",
				"application/x-cbz",
				"application/vnd.comicbook+zip",
				"application/epub+zip",
				"application/x-fictionbook",
				"application/x-mobipocket-ebook",
				"application/octet-stream"
			)
		)
	}

}