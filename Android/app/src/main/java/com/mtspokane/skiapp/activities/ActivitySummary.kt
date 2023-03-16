package com.mtspokane.skiapp.activities

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isNotEmpty
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.ktx.addPolyline
import com.mtspokane.skiapp.BuildConfig
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummaryLocations
import com.mtspokane.skiapp.activities.activitysummary.ActivityView
import com.mtspokane.skiapp.activities.activitysummary.FileSelectionDialog
import com.mtspokane.skiapp.databases.ActivityDatabase
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding
import com.mtspokane.skiapp.databases.SkiingActivity
import com.mtspokane.skiapp.databases.SkiingActivityManager
import com.mtspokane.skiapp.databases.TimeManager
import com.mtspokane.skiapp.mapItem.MapMarker
import com.mtspokane.skiapp.mapItem.PolylineMapItem
import com.mtspokane.skiapp.maphandlers.MapHandler
import com.mtspokane.skiapp.maphandlers.CustomDialogEntry
import com.mtspokane.skiapp.maphandlers.ActivitySummaryLocationMarkers
import com.orhanobut.dialogplus.DialogPlus
import java.io.File
import java.io.InputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ActivitySummary : FragmentActivity() {

	private lateinit var container: LinearLayout

	private lateinit var fileSelectionDialog: FileSelectionDialog

	private lateinit var map: Map

	private lateinit var optionsView: DialogPlus

	private val exportJsonCallback: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.CreateDocument(JSON_MIME_TYPE)) {
		if (it != null) {
			val json: JSONObject = getActivitySummaryJson()
			SkiingActivityManager.writeToExportFile(contentResolver, it, json.toString(4))
		}
	}

	private val exportGeoJsonCallback: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.CreateDocument(GEOJSON_MIME_TYPE)) {
		if (it != null) {
			val json: JSONObject = getActivitySummaryJson()
			val geoJson: JSONObject = SkiingActivityManager.convertJsonToGeoJson(json)
			SkiingActivityManager.writeToExportFile(contentResolver, it, geoJson.toString(4))
		}
	}

	private val importCallback: ActivityResultLauncher<Array<String>> = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->

		if (uri == null) {
			return@registerForActivityResult
		}

		val inputStream: InputStream? = contentResolver.openInputStream(uri)
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

		val binding: ActivitySummaryBinding = ActivitySummaryBinding.inflate(layoutInflater)
		setContentView(binding.root)

		container = binding.container

		fileSelectionDialog = FileSelectionDialog(this)

		// Be sure to show the action bar.
		if (actionBar != null) {
			actionBar!!.setDisplayShowTitleEnabled(true)
		}

		// Setup the map handler.
		map = Map()

		optionsView = DialogPlus.newDialog(this).setAdapter(OptionsDialog()).setExpanded(false).create()

		binding.optionsButton.setOnClickListener {
			optionsView.show()
		}

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.activity_map) as SupportMapFragment
		mapFragment.getMapAsync(map)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.summary_menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {

		val shareMenu: MenuItem = menu.findItem(R.id.share)
		val exportMenu: MenuItem = menu.findItem(R.id.export)

		shareMenu.isEnabled = container.isNotEmpty()
		exportMenu.isEnabled = container.isNotEmpty()

		return super.onPrepareOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {

		when (item.itemId) {
			R.id.open -> fileSelectionDialog.showDialog()
			R.id.export_json -> exportJsonCallback.launch("exported.json")
			R.id.export_geojson -> exportGeoJsonCallback.launch("exported.geojson")
			R.id.share_json -> {

				val json: JSONObject = getActivitySummaryJson()

				writeToShareFile("My Skiing Activity.json", json, JSON_MIME_TYPE)
			}
			R.id.share_geojson -> {

				val json: JSONObject = getActivitySummaryJson()

				val geojson: JSONObject = SkiingActivityManager.convertJsonToGeoJson(json)

				writeToShareFile("My Skiing Activity.geojson", geojson, GEOJSON_MIME_TYPE)
			}
			R.id.import_activity -> importCallback.launch(arrayOf(JSON_MIME_TYPE, GEOJSON_MIME_TYPE))
		}

		return super.onOptionsItemSelected(item)
	}

	private fun writeToShareFile(filename: String, jsonToWrite: JSONObject, mime: String) {

		val tmpFile = File(filesDir, filename)
		if (tmpFile.exists()) {
			tmpFile.delete()
		}

		openFileOutput(filename, Context.MODE_PRIVATE).use {
			it.write(jsonToWrite.toString(4).toByteArray())
		}

		SkiingActivityManager.shareFile(this, tmpFile, mime)
	}

	private fun getActivitySummaryJson(): JSONObject {
		return if (SkiingActivityManager.FinishedAndLoadedActivities != null) {
			SkiingActivityManager.convertSkiingActivitiesToJson(SkiingActivityManager.FinishedAndLoadedActivities!!)
		} else {
			SkiingActivityManager.convertSkiingActivitiesToJson(SkiingActivityManager.InProgressActivities.toTypedArray())
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		map.destroy()
		SkiingActivityManager.FinishedAndLoadedActivities = null
	}

	fun loadActivities(activities: Array<SkiingActivity>) {

		container.removeAllViews()

		with(map) {

			if (this.polyline != null) {
				this.polyline!!.remove()
				this.polyline = null
			}

			this.locationMarkers.forEach {
				it.destroy()
			}
			this.locationMarkers.clear()
		}

		val loadingToast: Toast = Toast.makeText(this, R.string.computing_location, Toast.LENGTH_LONG)
		lifecycleScope.launch(Dispatchers.IO, CoroutineStart.LAZY) {

			//async(Dispatchers.IO, CoroutineStart.UNDISPATCHED) {

			if (activities.isEmpty()) {
				return@launch
			}

			loadingToast.show()

			val mapMarkers: Array<MapMarker> = MapMarker.loadFromSkiingActivityArray(activities)

			withContext(Dispatchers.Main) {
				for (mapMarker in mapMarkers) {
					addMapMarkerToMap(mapMarker)
				}
			}

			val activitySummaryEntries: Array<ActivitySummaryEntry> = parseMapMarkersForMap(mapMarkers)

			withContext(Dispatchers.Main) {
				for (entry in activitySummaryEntries) {
					val view = this@ActivitySummary.createActivityView(entry)
					container.addView(view)
				}
				loadingToast.cancel()
				Toast.makeText(this@ActivitySummary, R.string.done, Toast.LENGTH_SHORT).show()
				map.addPolylineFromMarker()
			}
					//}//.await()
		}.start()

		//loadingToast.show()
	}

	@UiThread
	private fun addMapMarkerToMap(mapMarker: MapMarker) {

		val snippetText: String? = if (BuildConfig.DEBUG) {

			val altitudeString = "Altitude: ${ActivitySummaryLocations.altitudeConfidence}"
			val speedString = "Speed: ${ActivitySummaryLocations.speedConfidence}"
			val verticalDirectionString = "Vertical: ${ActivitySummaryLocations.getVerticalDirection().name}"

			"$altitudeString | $speedString | $verticalDirectionString"
		} else {
			null
		}

		val activitySummaryLocation = ActivitySummaryLocationMarkers(map.map, mapMarker, snippetText)
		map.locationMarkers.add(activitySummaryLocation)
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

						val newActivitySummaryEntry = ActivitySummaryEntry(startingMapMarker, maxSpeed,
								speedSum/sum, entry.skiingActivity.time)
						arraySummaryEntries.add(newActivitySummaryEntry)

						maxSpeed = 0.0F
						speedSum = 0.0F
						sum = 0
						startingMapMarker = entry
					}
				}

				if (entry.skiingActivity.speed > maxSpeed) {
					maxSpeed = entry.skiingActivity.speed
				}
				speedSum += entry.skiingActivity.speed
				++sum
			}

			val finalActivitySummary = ActivitySummaryEntry(startingMapMarker, maxSpeed,
					speedSum/sum, mapMarkers.last().skiingActivity.time)
			arraySummaryEntries.add(finalActivitySummary)

			return arraySummaryEntries.toTypedArray()
		}
	}

	private inner class Map : MapHandler(this@ActivitySummary), GoogleMap.InfoWindowAdapter {

		var locationMarkers: MutableList<ActivitySummaryLocationMarkers> = mutableListOf()

		var polyline: Polyline? = null

		override val additionalCallback: OnMapReadyCallback = OnMapReadyCallback {

			if (intent.hasExtra(SkierLocationService.ACTIVITY_SUMMARY_LAUNCH_DATE)) {
				loadFromIntent(intent.getStringExtra(SkierLocationService.ACTIVITY_SUMMARY_LAUNCH_DATE))
			}

			if (SkiingActivityManager.FinishedAndLoadedActivities != null) {
				loadActivities(SkiingActivityManager.FinishedAndLoadedActivities!!)
			} else {
				loadActivities(SkiingActivityManager.InProgressActivities.toTypedArray())
			}

			map.setOnCircleClickListener {

				map.setInfoWindowAdapter(this)

				if (it.tag is ActivitySummaryLocationMarkers) {

					val activitySummaryLocationMarker: ActivitySummaryLocationMarkers = it.tag as ActivitySummaryLocationMarkers

					if (activitySummaryLocationMarker.marker != null) {
						activitySummaryLocationMarker.marker!!.isVisible = true
						activitySummaryLocationMarker.marker!!.showInfoWindow()
					}
				}
			}

			map.setOnInfoWindowCloseListener { it.isVisible = false }
		}

		override fun destroy() {

			if (locationMarkers.isNotEmpty()) {

				Log.v("ActivitySummaryMap", "Removing location markers")
				for (marker in locationMarkers) {
					marker.destroy()
				}
				locationMarkers.clear()
			}

			if (polyline != null) {
				polyline!!.remove()
				polyline = null
			}

			super.destroy()
		}

		override fun getInfoContents(marker: Marker): View? {
			Log.v("CustomInfoWindow", "getInfoContents called")

			if (marker.tag is Pair<*, *>) {

				val markerInfo: Pair<MapMarker, String?> = marker.tag as Pair<MapMarker, String?>

				val markerView: View = layoutInflater.inflate(R.layout.info_window, null)

				val name: TextView = markerView.findViewById(R.id.marker_name)
				name.text = markerInfo.first.name

				val altitude: TextView = markerView.findViewById(R.id.marker_altitude)

				// Convert from meters to feet.
				val altitudeConversion = 3.280839895f

				try {
					altitude.text = getString(R.string.marker_altitude,
							(markerInfo.first.skiingActivity.altitude * altitudeConversion).roundToInt())
				} catch (e: IllegalArgumentException) {
					altitude.text = getString(R.string.marker_altitude, 0)
				}

				val speed: TextView = markerView.findViewById(R.id.marker_speed)

				// Convert from meters per second to miles per hour.
				val speedConversion = 0.44704f

				try {
					speed.text = getString(R.string.marker_speed,
							(markerInfo.first.skiingActivity.speed / speedConversion).roundToInt())
				} catch (e: IllegalArgumentException) {
					speed.text = getString(R.string.marker_speed, 0)
				}

				if (markerInfo.second != null) {
					val debug: TextView = markerView.findViewById(R.id.marker_debug)
					debug.text = markerInfo.second
				}

				return markerView
			} else {
				return null
			}
		}

		override fun getInfoWindow(marker: Marker): View? {
			Log.v("CustomInfoWindow", "getInfoWindow called")
			return null
		}

		@MainThread
		fun addPolylineFromMarker() {

			polyline = map.addPolyline {

				for (marker in locationMarkers) {
					if (marker.circle != null) {
						add(marker.circle!!.center)
					}
				}

				color(getARGB(R.color.yellow))
				zIndex(10.0F)
				geodesic(true)
				startCap(RoundCap())
				endCap(RoundCap())
				clickable(false)
				width(8.0F)
				visible(true)
			}
		}

		private fun loadFromIntent(date: String?) {

			if (date == null) {
				return
			}

			val database = ActivityDatabase(this.activity)
			SkiingActivityManager.FinishedAndLoadedActivities = ActivityDatabase
					.readSkiingActivesFromDatabase(date, database.readableDatabase)
			database.close()

			val notificationManager: NotificationManager = this.activity.getSystemService(Context.NOTIFICATION_SERVICE)
					as NotificationManager
			notificationManager.cancel(SkierLocationService.ACTIVITY_SUMMARY_ID)
		}
	}

	private inner class OptionsDialog : BaseAdapter() {

		private var showChairliftImage: CustomDialogEntry? = null

		private var showEasyRunsImage: CustomDialogEntry? = null

		private var showModerateRunsImage: CustomDialogEntry? = null

		private var showDifficultRunsImage: CustomDialogEntry? = null

		private var showNightRunsImage: CustomDialogEntry? = null

		override fun getCount(): Int {
			return 1
		}

		override fun getItem(position: Int): Any {
			return position // Todo properly implement me?
		}

		override fun getItemId(position: Int): Long {
			return position.toLong() // Todo properly implement me?
		}

		override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

			val view: View = convertView ?: layoutInflater.inflate(R.layout.activity_map_options, parent, false)

			if (showChairliftImage == null) {
				val chairliftImage: CustomDialogEntry = view.findViewById(R.id.show_chairlift)
				chairliftImage.setOnClickListener(CustomOnClickListener(map.chairliftPolylines))
				chairliftImage.setGlowing(map.chairliftPolylines[0].polylines[0].isVisible)
				showChairliftImage = chairliftImage
			}

			if (showEasyRunsImage == null) {
				val easyRunImage: CustomDialogEntry = view.findViewById(R.id.show_easy_runs)
				easyRunImage.setOnClickListener(CustomOnClickListener(map.easyRunsPolylines))
				easyRunImage.setGlowing(map.easyRunsPolylines[0].polylines[0].isVisible)
				showEasyRunsImage = easyRunImage
			}

			if (showModerateRunsImage == null) {
				val moderateRunImage: CustomDialogEntry = view.findViewById(R.id.show_moderate_runs)
				moderateRunImage.setOnClickListener(CustomOnClickListener(map.moderateRunsPolylines))
				moderateRunImage.setGlowing(map.moderateRunsPolylines[0].polylines[0].isVisible)
				showModerateRunsImage = moderateRunImage
			}

			if (showDifficultRunsImage == null) {
				val difficultRunImage: CustomDialogEntry = view.findViewById(R.id.show_difficult_runs)
				difficultRunImage.setOnClickListener(CustomOnClickListener(map.difficultRunsPolylines))
				difficultRunImage.setGlowing(map.difficultRunsPolylines[0].polylines[0].isVisible)
				showDifficultRunsImage = difficultRunImage
			}

			if (showNightRunsImage == null) {
				val nightRunImage: CustomDialogEntry = view.findViewById(R.id.show_night_runs)
				nightRunImage.setOnClickListener {
					if (it == null || it !is CustomDialogEntry) {
						return@setOnClickListener
					}

					with(map) {

						isNightOnly = !isNightOnly

						for (chairliftPolyline in chairliftPolylines) {
							chairliftPolyline.togglePolyLineVisibility(chairliftPolyline.defaultVisibility, isNightOnly)
						}

						for (easyRunPolyline in easyRunsPolylines) {
							easyRunPolyline.togglePolyLineVisibility(easyRunPolyline.defaultVisibility, isNightOnly)
						}

						for (moderateRunPolyline in moderateRunsPolylines) {
							moderateRunPolyline.togglePolyLineVisibility(moderateRunPolyline.defaultVisibility, isNightOnly)
						}

						for (difficultRunPolyline in difficultRunsPolylines) {
							difficultRunPolyline.togglePolyLineVisibility(difficultRunPolyline.defaultVisibility, isNightOnly)
						}

						it.setGlowing(isNightOnly)
					}
				}
				nightRunImage.setGlowing(map.isNightOnly)
				showNightRunsImage = nightRunImage
			}

			return view
		}
	}

	private inner class CustomOnClickListener(val polylineMapItems: List<PolylineMapItem>): View.OnClickListener {

		override fun onClick(v: View?) {

			if (v == null || v !is CustomDialogEntry) {
				return
			}

			for (polylineMapItem in polylineMapItems) {
				polylineMapItem.togglePolyLineVisibility(!polylineMapItem.defaultVisibility, map.isNightOnly)
			}

			v.setGlowing(polylineMapItems[0].polylines[0].isVisible)
		}
	}

	data class ActivitySummaryEntry(val mapMarker: MapMarker, val maxSpeed: Float, val averageSpeed: Float, val endTime: Long?)
}