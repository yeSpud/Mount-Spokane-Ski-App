package com.mtspokane.skiapp.activities.activitysummary

import android.app.AlertDialog
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.AnyThread
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isNotEmpty
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.mtspokane.skiapp.BuildConfig
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding
import com.mtspokane.skiapp.databinding.FileSelectionBinding
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.maphandlers.ActivitySummaryMap
import com.mtspokane.skiapp.skiingactivity.SkiingActivity
import com.mtspokane.skiapp.skiingactivity.SkiingActivityManager
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ActivitySummary : FragmentActivity() {

	private var mapHandler: ActivitySummaryMap? = null

	private lateinit var container: LinearLayout

	private lateinit var fileSelectionDialog: FileSelectionDialog

	private lateinit var creditDialog: AlertDialog

	var loadedFile: String = "${SkiingActivityManager.getDate()}.json"

	private var mostRecentlyAddedActivityView: ActivityView? = null

	private val exportCallback = this.registerForActivityResult(ActivityResultContracts.CreateDocument()) {

		if (it != null) {

			val json: JSONObject = SkiingActivityManager.readJsonFromFile(this, this.loadedFile)

			val mimeType = this.contentResolver.getType(it)

			val query: Cursor? = this.contentResolver.query(it, null, null, null, null)
			val fileName: String? = query?.use { cursor: Cursor ->
				cursor.moveToFirst()
				val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
				cursor.getString(nameIndex)
			}

			if (fileName != null) {

				if (mimeType == JSON_MIME_TYPE && fileName.endsWith(".json")) {
					SkiingActivityManager.writeToExportFile(this.contentResolver, it, json.toString(4))
				} else if ((mimeType == GEOJSON_MIME_TYPE || mimeType == "application/octet-stream")
					&& fileName.endsWith(".geojson")) {
					val geoJson: JSONObject = SkiingActivityManager.convertJsonToGeoJson(json)
					SkiingActivityManager.writeToExportFile(this.contentResolver, it, geoJson.toString(4))
				} else {
					Log.w("exportCallback", "File type unaccounted for: $fileName:$mimeType")
				}
			}
		}
	}

	private val importCallback = this.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->

		if (uri == null) {
			return@registerForActivityResult
		}

		val inputStream: InputStream? = this.contentResolver.openInputStream(uri)
		if (inputStream == null) {
			Log.w("importCallback", "Unable to open a file input stream for the imported file.")
			return@registerForActivityResult
		}

		val json: JSONObject = inputStream.bufferedReader().useLines {

			val string = it.fold("") { _, inText -> inText }

			JSONObject(string)
		}
		inputStream.close()

		val jsonDate = json.keys().next()
		val jsonArray = json.getJSONArray(jsonDate)

		val skiingActivities: Array<SkiingActivity> = SkiingActivityManager.jsonArrayToSkiingActivities(jsonArray)

		SkiingActivityManager.writeActivitiesToFile(this, skiingActivities, jsonDate)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val binding: ActivitySummaryBinding = ActivitySummaryBinding.inflate(this.layoutInflater)
		this.setContentView(binding.root)

		this.container = binding.container

		this.fileSelectionDialog = FileSelectionDialog(this)

		val creditDialogBuilder = AlertDialog.Builder(this)
		creditDialogBuilder.setView(R.layout.icon_credits)
		creditDialogBuilder.setPositiveButton(R.string.close_button) { dialog, _ -> dialog.dismiss() }
		this.creditDialog = creditDialogBuilder.create()

		// Be sure to show the action bar.
		this.actionBar!!.setDisplayShowTitleEnabled(true)

		// Setup the map handler.
		this.mapHandler = ActivitySummaryMap(this)

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.activity_map) as SupportMapFragment
		mapFragment.getMapAsync(this.mapHandler!!)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		this.menuInflater.inflate(R.menu.summary_menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {

		val shareMenu: MenuItem = menu.findItem(R.id.share)
		val exportMenu: MenuItem = menu.findItem(R.id.export)

		shareMenu.isEnabled = this.container.isNotEmpty()
		exportMenu.isEnabled = this.container.isNotEmpty()

		return super.onPrepareOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {

		when (item.itemId) {
			R.id.open -> this.fileSelectionDialog.showDialog()
			R.id.export_json -> this.exportCallback.launch(this.loadedFile)
			R.id.export_geojson -> this.exportCallback.launch(this.loadedFile.replace("json", "geojson"))
			R.id.share_json -> {
				val file = File(this.filesDir, this.loadedFile)
				SkiingActivityManager.shareFile(this, file, JSON_MIME_TYPE)
			}
			R.id.share_geojson -> {

				val json: JSONObject = SkiingActivityManager.readJsonFromFile(this, this.loadedFile)
				val geojson: JSONObject = SkiingActivityManager.convertJsonToGeoJson(json)

				val tmpFileName = this.loadedFile.replace("json", "geojson")
				this.openFileOutput(tmpFileName, Context.MODE_PRIVATE).use {
					it.write(geojson.toString(4).toByteArray())
				}

				val tmpFile = File(this.filesDir, tmpFileName)

				SkiingActivityManager.shareFile(this, tmpFile, JSON_MIME_TYPE)

				tmpFile.delete()
			}
			R.id.import_activity -> this.importCallback.launch(arrayOf(JSON_MIME_TYPE, GEOJSON_MIME_TYPE))
			R.id.credits -> this.creditDialog.show()
		}

		return super.onOptionsItemSelected(item)
	}

	override fun onDestroy() {
		super.onDestroy()

		if (this.mapHandler != null) {
			this.mapHandler!!.destroy()
			this.mapHandler = null
		}

		SkiingActivityManager.FinishedAndLoadedActivities = null

		MtSpokaneMapItems.destroyUIItems(this::class)
	}

	fun loadActivities(activities: Array<SkiingActivity>) {

		this.container.removeAllViews()

		if (this.mapHandler != null) {

			if (this.mapHandler!!.polyline != null) {
				this.mapHandler!!.polyline!!.remove()
				this.mapHandler!!.polyline = null
			}

			this.mapHandler!!.locationMarkers.forEach {
				it.destroy()
			}
			this.mapHandler!!.locationMarkers = emptyArray()
		}

		lifecycleScope.launch(Dispatchers.IO, CoroutineStart.LAZY) {

			async(Dispatchers.IO, CoroutineStart.LAZY) {

				if (activities.isEmpty()) {
					return@async
				}

				val activityQueue: Queue<SkiingActivity> = LinkedList()
				activityQueue.addAll(activities)
				this@ActivitySummary.addToViewRecursively(activityQueue)
				this@ActivitySummary.mostRecentlyAddedActivityView = null
			}.await()

			if (this@ActivitySummary.mapHandler != null) {
				withContext(Dispatchers.Main) {
					this@ActivitySummary.mapHandler!!.addPolylineFromMarker()
				}
			}

		}.start()
	}

	@AnyThread
	private suspend fun addCircleAndMarker(titleDrawableResourceMarkerIcon: Pair<String, Pair<Int, BitmapDescriptor>>,
	                               skiingActivity: SkiingActivity): Unit = coroutineScope {

		if (this@ActivitySummary.mapHandler == null) {
			return@coroutineScope
		}

		val snippetText: String? = if (BuildConfig.DEBUG) {

			val altitudeString = "Altitude: ${ActivitySummaryLocations.altitudeConfidence}"
			val speedString = "Speed: ${ActivitySummaryLocations.speedConfidence}"
			val chairliftString = "Chairlift: ${if (ActivitySummaryLocations.mostLikelyChairlift != null) {
				ActivitySummaryLocations.mostLikelyChairlift!!.name
			} else {
				"Null"
			}}"

			"$altitudeString | $speedString | $chairliftString"
		} else {
			null
		}

		withContext(Dispatchers.Main) {
			this@ActivitySummary.mapHandler!!.addActivitySummaryLocationMarker(titleDrawableResourceMarkerIcon.first,
				skiingActivity.latitude, skiingActivity.longitude, titleDrawableResourceMarkerIcon.second.first,
				titleDrawableResourceMarkerIcon.second.second, snippetText)
		}
	}

	private suspend fun addToViewRecursively(queue: Queue<SkiingActivity>): Unit = coroutineScope {

		val tag = "addToViewRecursively"

		if (queue.isEmpty()) {
			return@coroutineScope
		}

		val startingActivity: SkiingActivity = queue.remove()
		ActivitySummaryLocations.updateLocations(startingActivity)
		val startingTitleDrawableResourceMarkerIcon: Pair<String, Pair<Int, BitmapDescriptor>> =
			this@ActivitySummary.getTitleDrawableResourceMarkerIcon()
		this@ActivitySummary.addCircleAndMarker(startingTitleDrawableResourceMarkerIcon, startingActivity)
		Log.v(tag, "Starting with ${startingTitleDrawableResourceMarkerIcon.first}")

		var endingActivity: SkiingActivity?
		do {

			endingActivity = queue.peek()

			if (endingActivity == null) {
				break
			}

			ActivitySummaryLocations.updateLocations(endingActivity)
			val endingTitleDrawableResourceMarkerIcon: Pair<String, Pair<Int, BitmapDescriptor>> =
				this@ActivitySummary.getTitleDrawableResourceMarkerIcon()
			this@ActivitySummary.addCircleAndMarker(endingTitleDrawableResourceMarkerIcon, endingActivity)

			if ((startingTitleDrawableResourceMarkerIcon.first == endingTitleDrawableResourceMarkerIcon.first)
				|| (endingTitleDrawableResourceMarkerIcon.first == UNKNOWN_LOCATION)) {
					Log.v(tag, "Removing ${endingTitleDrawableResourceMarkerIcon.first} from queue")
				queue.remove()
			} else {
				Log.v(tag, "Next item should be ${endingTitleDrawableResourceMarkerIcon.first}")
				break
			}

		} while (queue.peek() != null)

		withContext(Dispatchers.Main) {
			val view: ActivityView = if (endingActivity == null) {
				this@ActivitySummary.createActivityView(startingTitleDrawableResourceMarkerIcon.second.first,
					startingTitleDrawableResourceMarkerIcon.first, startingActivity.time, null)
			} else {
				this@ActivitySummary.createActivityView(startingTitleDrawableResourceMarkerIcon.second.first,
					startingTitleDrawableResourceMarkerIcon.first, startingActivity.time, endingActivity.time)
			}

			if (this@ActivitySummary.mostRecentlyAddedActivityView != null) {
				if (this@ActivitySummary.mostRecentlyAddedActivityView!!.title.text == view.title.text) {
					Log.w(tag, "Found view with same title!")
				} else {
					this@ActivitySummary.mostRecentlyAddedActivityView = view
					this@ActivitySummary.container.addView(view)
				}
			} else {
				this@ActivitySummary.mostRecentlyAddedActivityView = view
				this@ActivitySummary.container.addView(view)
			}
		}

		this@ActivitySummary.addToViewRecursively(queue)
	}

	private fun getTitleDrawableResourceMarkerIcon(): Pair<String, Pair<Int, BitmapDescriptor>> {

		val returnPair: Pair<String, Pair<Int, BitmapDescriptor>> = if (ActivitySummaryLocations.altitudeConfidence >= 1u &&
			ActivitySummaryLocations.speedConfidence >= 1u &&
			ActivitySummaryLocations.mostLikelyChairlift != null) {

			Pair(ActivitySummaryLocations.mostLikelyChairlift!!.name, Pair(R.drawable.ic_chairlift,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
		} else {

			val chairliftTerminal: MapItem? = ActivitySummaryLocations.checkIfAtChairliftTerminals()
			if (chairliftTerminal != null) {
				Pair(chairliftTerminal.name, Pair(R.drawable.ic_chairlift,
					BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
			} else {

				val other: MapItem? = ActivitySummaryLocations.checkIfOnOther()
				if (other != null) {
					val icon: Int = if (other.getIcon() != null) {
						other.getIcon()!!
					} else {
						R.drawable.ic_missing
					}
					Pair(other.name, Pair(icon, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)))
				} else {

					val run: MapItem? = ActivitySummaryLocations.checkIfOnRun()
					if (run != null) {
						val icon: Int = if (run.getIcon() != null) {
							run.getIcon()!!
						} else {
							R.drawable.ic_missing
						}
						val markerIconColor = when (icon) {
							R.drawable.ic_easy -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
							R.drawable.ic_moderate -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
							R.drawable.ic_difficult -> bitmapDescriptorFromVector(this, R.drawable.ic_black_marker)
							else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
						}
						Pair(run.name, Pair(icon, markerIconColor))
					} else {
						Pair(UNKNOWN_LOCATION, Pair(R.drawable.ic_missing,
							BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)))
					}
				}
			}
		}

		return returnPair
	}

	private fun getTimeFromLong(time: Long): String {
		val timeFormatter = SimpleDateFormat("h:mm:ss", Locale.US)
		val date = Date(time)
		return timeFormatter.format(date)
	}

	@MainThread
	private fun createActivityView(@DrawableRes icon: Int?, titleText: String,
	                               startTime: Long, endTime: Long?): ActivityView {

		val activityView = ActivityView(this)

		if (icon != null) {
			activityView.icon.setImageDrawable(AppCompatResources.getDrawable(this, icon))
		} else {
			activityView.icon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_missing))
		}

		activityView.title.text = titleText
		activityView.startTime.text = this.getTimeFromLong(startTime)

		if (endTime != null) {
			activityView.endTime.text = this.getTimeFromLong(endTime)
		}

		return activityView
	}

	companion object {

		const val JSON_MIME_TYPE = "application/json"

		const val GEOJSON_MIME_TYPE = "application/geojson"

		private const val UNKNOWN_LOCATION = "Unknown Location"

		/**
		 * @author https://stackoverflow.com/questions/42365658/custom-marker-in-google-maps-in-android-with-vector-asset-icon/45564994#45564994
		 */
		private fun bitmapDescriptorFromVector(context: Context, @DrawableRes vectorResId: Int): BitmapDescriptor {
			val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
			vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
			val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight,
				Bitmap.Config.ARGB_8888)
			val canvas = Canvas(bitmap)
			vectorDrawable.draw(canvas)
			return BitmapDescriptorFactory.fromBitmap(bitmap)
		}
	}
}

class FileSelectionDialog(private val activity: ActivitySummary) : AlertDialog(activity) {

	fun showDialog() {

		val binding: FileSelectionBinding = FileSelectionBinding.inflate(this.layoutInflater)

		val alertDialogBuilder = Builder(this.context)
		alertDialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
		alertDialogBuilder.setView(binding.root)

		val dialog: AlertDialog = alertDialogBuilder.create()

		val files: Array<File> = this.context.filesDir.listFiles()!!
		files.forEach { file ->

			if (file.name.matches(fileRegex)) {

				val textView = TextView(this.context)
				textView.layoutParams = ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT)
				textView.text = file.name
				textView.textSize = 25.0F
				textView.setOnClickListener {

					SkiingActivityManager.FinishedAndLoadedActivities = SkiingActivityManager.readSkiingActivitiesFromFile(this.context, file.name)

					if (SkiingActivityManager.FinishedAndLoadedActivities != null) {
						this.activity.loadActivities(SkiingActivityManager.FinishedAndLoadedActivities!!)
						this.activity.loadedFile = file.name
					}

					dialog.dismiss()
				}

				binding.files.addView(textView)
			}
		}

		dialog.show()
	}

	companion object {

		val fileRegex: Regex = Regex("\\d\\d\\d\\d-\\d\\d-\\d\\d.json")
	}
}