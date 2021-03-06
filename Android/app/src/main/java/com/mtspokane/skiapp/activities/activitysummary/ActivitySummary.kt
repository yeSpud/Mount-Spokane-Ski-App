package com.mtspokane.skiapp.activities.activitysummary

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isNotEmpty
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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

	@Deprecated("Fix this")
	private var mostRecentlyAddedActivityView: ActivityView? = null

	private val exportJsonCallback: ActivityResultLauncher<String> = this.registerForActivityResult(ActivityResultContracts.CreateDocument()) {

		if (it != null) {

			val json: JSONObject = this.getActivitySummaryJson()

			SkiingActivityManager.writeToExportFile(this.contentResolver, it, json.toString(4))
		}
	}

	private val exportGeoJsonCallback: ActivityResultLauncher<String> = this.registerForActivityResult(ActivityResultContracts.CreateDocument()) {

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

		lifecycleScope.launch(Dispatchers.IO, CoroutineStart.LAZY) {

			async(Dispatchers.IO, CoroutineStart.LAZY) {

				if (activities.isEmpty()) {
					return@async
				}

				val activityQueue: LinkedList<SkiingActivity> = LinkedList()
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
			val foo = ActivitySummaryLocationMarkers(this@ActivitySummary.mapHandler!!.map!!,
				mapMarker, snippetText)
			this@ActivitySummary.mapHandler!!.locationMarkers.add(foo)
		}
	}

	private suspend fun addToViewRecursively(linkedList: LinkedList<SkiingActivity>): Unit = coroutineScope { // FIXME (still duplicated locations and misses others)

		val tag = "addToViewRecursively"

		// If the list is empty then return now.
		if (linkedList.isEmpty()) {
			return@coroutineScope
		}

		// Get the starting activity from the list. Be sure it is removed.
		val startingActivity: SkiingActivity = linkedList.removeFirst()

		// Update the locations.
		ActivitySummaryLocations.updateLocations(startingActivity)

		// Get the starting activity item.
		val startingMapMarker: MapMarker = this@ActivitySummary.getMapMarker()
		Log.v(tag, "Starting with ${startingMapMarker.name} (${linkedList.size} items left)")

		// Add the starting circle to the map.
		this@ActivitySummary.addMapMarkerToMap(startingMapMarker)

		// If the starting location is unknown skip it.
		if (startingMapMarker.name == UNKNOWN_LOCATION) {
			this@ActivitySummary.addToViewRecursively(linkedList)
			return@coroutineScope
		}

		// Get the ending activity, and continue to update it until the list is empty (or other... see below).
		var endingActivity: SkiingActivity? = null
		var maxSpeed = 0.0F
		var speedSum = 0.0F
		var sum = 0
		while(linkedList.isNotEmpty()) {

			// Remove the now first item in the list as a potential ending activity.
			val potentialEndingActivity: SkiingActivity = linkedList.first

			// Update the locations.
			ActivitySummaryLocations.updateLocations(potentialEndingActivity)

			// Get the ending activity item.
			val potentialEndingMapMarker: MapMarker = this@ActivitySummary.getMapMarker()

			// Add the circle to the map.
			this@ActivitySummary.addMapMarkerToMap(potentialEndingMapMarker)

			// If the ending activity isn't unknown then check if it has matching names.
			if (potentialEndingMapMarker.name != UNKNOWN_LOCATION) {

				// If the names don't match exit the while loop.
				if (startingMapMarker.name != potentialEndingMapMarker.name) {

					Log.v(tag, "Next item should be ${potentialEndingMapMarker.name} (${linkedList.size} items left)")
					break
				}
			}

			if (potentialEndingActivity.speed > maxSpeed) {
				maxSpeed = potentialEndingActivity.speed
			}

			speedSum += potentialEndingActivity.speed
			sum++

			// Otherwise continue looping and officially remove that first item from the list.
			endingActivity = linkedList.removeFirst()
		}

		withContext(Dispatchers.Main) {

			val view: ActivityView = if (endingActivity == null) {
				this@ActivitySummary.createActivityView(startingMapMarker, maxSpeed,
						speedSum / sum, null)
			} else {
				this@ActivitySummary.createActivityView(startingMapMarker, maxSpeed,
						speedSum / sum, endingActivity.time)
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

		this@ActivitySummary.addToViewRecursively(linkedList)
	}

	private fun getMapMarker(): MapMarker {

		var marker: MapMarker? = ActivitySummaryLocations.checkIfIOnChairlift()
		if (marker != null) {
			return marker
		}

		marker = ActivitySummaryLocations.checkIfAtChairliftTerminals()
		if (marker != null) {
			return marker
		}

		marker = ActivitySummaryLocations.checkIfOnOther()
		if (marker != null) {
			return marker
		}

		marker = ActivitySummaryLocations.checkIfOnRun()
		if (marker != null) {
			return marker
		}

		Log.w("getMapMarker", "Unable to determine location")
		return  MapMarker(UNKNOWN_LOCATION, ActivitySummaryLocations.currentLocation!!,
			R.drawable.ic_missing, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
			Color.MAGENTA)
	}

	@MainThread
	private fun createActivityView(mapMarker: MapMarker, maxSpeed: Float, averageSpeed: Float,
								   endTime: Long?): ActivityView {

		val activityView = ActivityView(this)

		activityView.icon.setImageDrawable(AppCompatResources.getDrawable(this, mapMarker.icon))

		activityView.title.text = mapMarker.name

		// Convert from meters per second to miles per hour.
		val conversion = 0.44704f

		try {
			activityView.maxSpeed.text = this.getString(R.string.max_speed, (maxSpeed / conversion)
				.roundToInt())
		} catch (e: IllegalArgumentException) {
			activityView.maxSpeed.text = this.getString(R.string.max_speed, 0)
		}

		try {
			activityView.averageSpeed.text = this.getString(R.string.average_speed,
				(averageSpeed / conversion).roundToInt())
		} catch (e: IllegalArgumentException) {
			activityView.averageSpeed.text = this.getString(R.string.average_speed, 0)
		}

		activityView.startTime.text = TimeManager.getTimeFromLong(mapMarker.skiingActivity.time)

		if (endTime != null) {
			activityView.endTime.text = TimeManager.getTimeFromLong(endTime)
		}

		return activityView
	}

	companion object {

		const val JSON_MIME_TYPE = "application/json"

		const val GEOJSON_MIME_TYPE = "application/geojson"

		private const val UNKNOWN_LOCATION = "Unknown Location"
	}
}