package com.mtspokane.skiapp.activities.activitysummary

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isNotEmpty
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.SupportMapFragment
import com.mtspokane.skiapp.BuildConfig
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databases.ActivityDatabase
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.maphandlers.activitysummarymap.ActivitySummaryMap
import com.mtspokane.skiapp.databases.SkiingActivity
import com.mtspokane.skiapp.databases.SkiingActivityManager
import com.mtspokane.skiapp.databases.TimeManager
import com.mtspokane.skiapp.mapItem.MapMarker
import com.mtspokane.skiapp.maphandlers.activitysummarymap.ActivitySummaryLocationMarkers
import java.io.File
import java.io.InputStream
import java.util.LinkedList
import kotlin.math.roundToInt
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

	private val exportJsonCallback: ActivityResultLauncher<String> = this.registerForActivityResult(ActivityResultContracts.CreateDocument(JSON_MIME_TYPE)) {

		if (it != null) {

			val json: JSONObject = this.getActivitySummaryJson()

			SkiingActivityManager.writeToExportFile(this.contentResolver, it, json.toString(4))
		}
	}

	private val exportGeoJsonCallback: ActivityResultLauncher<String> = this.registerForActivityResult(ActivityResultContracts.CreateDocument(GEOJSON_MIME_TYPE)) {

		if (it != null) {

			val json: JSONObject = this.getActivitySummaryJson()

			val geoJson: JSONObject = SkiingActivityManager.convertJsonToGeoJson(json)
			SkiingActivityManager.writeToExportFile(this.contentResolver, it, geoJson.toString(4))
		}
	}

	private val importCallback: ActivityResultLauncher<Array<String>> = this.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->

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

		val database = ActivityDatabase(this)
		ActivityDatabase.importJsonToDatabase(json, database.writableDatabase)
		database.close()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val binding: ActivitySummaryBinding = ActivitySummaryBinding.inflate(this.layoutInflater)
		this.setContentView(binding.root)

		this.container = binding.container

		this.fileSelectionDialog = FileSelectionDialog(this)

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
			R.id.export_json -> this.exportJsonCallback.launch("exported.json")
			R.id.export_geojson -> this.exportGeoJsonCallback.launch("exported.geojson")
			R.id.share_json -> {

				val json: JSONObject = this.getActivitySummaryJson()

				this.writeToShareFile("My Skiing Activity.json", json, JSON_MIME_TYPE)
			}
			R.id.share_geojson -> {

				val json: JSONObject = this.getActivitySummaryJson()

				val geojson: JSONObject = SkiingActivityManager.convertJsonToGeoJson(json)

				this.writeToShareFile("My Skiing Activity.geojson", geojson, GEOJSON_MIME_TYPE)
			}
			R.id.import_activity -> this.importCallback.launch(arrayOf(JSON_MIME_TYPE, GEOJSON_MIME_TYPE))
		}

		return super.onOptionsItemSelected(item)
	}

	private fun writeToShareFile(filename: String, jsonToWrite: JSONObject, mime: String) {

		val tmpFile = File(this.filesDir, filename)
		if (tmpFile.exists()) {
			tmpFile.delete()
		}

		this.openFileOutput(filename, Context.MODE_PRIVATE).use {
			it.write(jsonToWrite.toString(4).toByteArray())
		}

		SkiingActivityManager.shareFile(this, tmpFile, mime)
	}

	private fun getActivitySummaryJson(): JSONObject {
		return if (SkiingActivityManager.FinishedAndLoadedActivities != null) {
			SkiingActivityManager.convertSkiingActivitiesToJson(SkiingActivityManager.FinishedAndLoadedActivities!!)
		} else {
			SkiingActivityManager.convertSkiingActivitiesToJson(SkiingActivityManager
				.InProgressActivities.toTypedArray())
		}
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

			with(this.mapHandler!!) {

				if (this.polyline != null) {
					this.polyline!!.remove()
					this.polyline = null
				}

				this.locationMarkers.forEach {
					it.destroy()
				}
				this.locationMarkers.clear()
			}
		}

		val loadingToast: Toast = Toast.makeText(this, R.string.computing_location, Toast.LENGTH_LONG)

		lifecycleScope.launch(Dispatchers.IO, CoroutineStart.LAZY) {

			async(Dispatchers.IO, CoroutineStart.LAZY) {

				if (activities.isEmpty()) {
					return@async
				}

				val mapMarkers: Array<MapMarker> = MapMarker.loadFromSkiingActivityArray(activities)

				for (mapMarker in mapMarkers) {
					addMapMarkerToMap(mapMarker)
				}

				val activitySummaryEntries: Array<ActivitySummaryEntry> = parseMapMarkersForMap(mapMarkers)

				withContext(Dispatchers.Main) {
					for (entry in activitySummaryEntries) {
						val view = this@ActivitySummary.createActivityView(entry)
						container.addView(view)
					}
					loadingToast.cancel()
					Toast.makeText(this@ActivitySummary, R.string.done, Toast.LENGTH_SHORT).show()
				}
			}.await()

			if (this@ActivitySummary.mapHandler != null) {
				withContext(Dispatchers.Main) {
					this@ActivitySummary.mapHandler!!.addPolylineFromMarker()
				}
			}

		}.start()

		loadingToast.show()
	}

	@AnyThread
	private suspend fun addMapMarkerToMap(mapMarker: MapMarker): Unit = coroutineScope {

		if (this@ActivitySummary.mapHandler == null) {
			return@coroutineScope
		}

		val snippetText: String? = if (BuildConfig.DEBUG) {

			val altitudeString = "Altitude: ${ActivitySummaryLocations.altitudeConfidence}"
			val speedString = "Speed: ${ActivitySummaryLocations.speedConfidence}"
			val verticalDirectionString = "Vertical: ${ActivitySummaryLocations.getVerticalDirection().name}"

			"$altitudeString | $speedString | $verticalDirectionString"
		} else {
			null
		}

		withContext(Dispatchers.Main) {
			val activitySummaryLocation = ActivitySummaryLocationMarkers(this@ActivitySummary.mapHandler!!.map!!,
					mapMarker, snippetText)
			this@ActivitySummary.mapHandler!!.locationMarkers.add(activitySummaryLocation)
		}
	}

	@MainThread
	private fun createActivityView(activitySummaryEntry: ActivitySummaryEntry): ActivityView {

		val activityView = ActivityView(this)

		activityView.icon.setImageDrawable(AppCompatResources.getDrawable(this, activitySummaryEntry.mapMarker.icon))

		activityView.title.text = activitySummaryEntry.mapMarker.name

		// Convert from meters per second to miles per hour.
		val conversion = 0.44704f

		try {
			activityView.maxSpeed.text = this.getString(R.string.max_speed,
					(activitySummaryEntry.maxSpeed / conversion).roundToInt())
		} catch (e: IllegalArgumentException) {
			activityView.maxSpeed.text = this.getString(R.string.max_speed, 0)
		}

		try {
			activityView.averageSpeed.text = this.getString(R.string.average_speed,
					(activitySummaryEntry.averageSpeed / conversion).roundToInt())
		} catch (e: IllegalArgumentException) {
			activityView.averageSpeed.text = this.getString(R.string.average_speed, 0)
		}

		activityView.startTime.text = TimeManager.getTimeFromLong(activitySummaryEntry.mapMarker.skiingActivity.time)

		if (activitySummaryEntry.endTime != null) {
			activityView.endTime.text = TimeManager.getTimeFromLong(activitySummaryEntry.endTime)
		}

		return activityView
	}

	companion object {

		const val JSON_MIME_TYPE = "application/json"

		const val GEOJSON_MIME_TYPE = "application/geojson"

		fun parseMapMarkersForMap(mapMarkers: Array<MapMarker>): Array<ActivitySummaryEntry> {

			// Create a place to store all the ActivitySummaryEntries.
			val arraySummaryEntries = mutableListOf<ActivitySummaryEntry>()

			var startingIndexOffset = 0
			for (i in 0..mapMarkers.size) {
				if (mapMarkers[i].name != MapMarker.UNKNOWN_LOCATION) {
					startingIndexOffset = i
					break
				}
			}

			var startingMapMarker = mapMarkers[startingIndexOffset]
			var maxSpeed = 0.0F
			var speedSum = 0.0F
			var sum = 0

			for (i in startingIndexOffset until mapMarkers.size) {
				val entry:MapMarker = mapMarkers[i]

				if (entry.name != MapMarker.UNKNOWN_LOCATION) {

					if (entry.name != startingMapMarker.name) {

						val newActivitySummaryEntry = ActivitySummaryEntry(startingMapMarker, maxSpeed, speedSum/sum, entry.skiingActivity.time)
						arraySummaryEntries.add(newActivitySummaryEntry)

						maxSpeed = 0.0F
						speedSum = 0.0F
						sum = 0
						startingMapMarker = entry
					}
				}

				if (entry.skiingActivity.speed > maxSpeed) {
					maxSpeed = entry.skiingActivity.speed;
				}
				speedSum += entry.skiingActivity.speed
				++sum
			}

			val finalActivitySummary = ActivitySummaryEntry(startingMapMarker, maxSpeed, speedSum/sum, mapMarkers.last().skiingActivity.time)
			arraySummaryEntries.add(finalActivitySummary)

			return arraySummaryEntries.toTypedArray()
		}
	}

	data class ActivitySummaryEntry(val mapMarker: MapMarker, val maxSpeed: Float, val averageSpeed: Float, val endTime: Long?)
}