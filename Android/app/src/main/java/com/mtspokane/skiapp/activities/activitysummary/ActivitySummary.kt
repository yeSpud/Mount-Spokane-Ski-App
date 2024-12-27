package com.mtspokane.skiapp.activities.activitysummary

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isNotEmpty
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.ktx.addCircle
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.addPolyline
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.SkierLocationService
import com.mtspokane.skiapp.databases.Database
import com.mtspokane.skiapp.databases.SkiingActivity
import com.mtspokane.skiapp.databases.SkiingActivityDao
import com.mtspokane.skiapp.databases.SkiingDateWithActivities
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding
import com.mtspokane.skiapp.databinding.FileSelectionBinding
import com.mtspokane.skiapp.mapItem.Locations
import com.mtspokane.skiapp.mapItem.MapMarker
import com.mtspokane.skiapp.maphandlers.MapHandler
import com.mtspokane.skiapp.maphandlers.MapOptionsDialog
import com.orhanobut.dialogplus.DialogPlus
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class ActivitySummary : FragmentActivity() {

	private lateinit var binding: ActivitySummaryBinding
	private var totalRunsNumber = 0
	private var absoluteMaxSpeed = 0F
	private var averageSpeedSum = 0F

	private lateinit var container: LinearLayout

	private lateinit var fileSelectionDialog: FileSelectionDialog

	private lateinit var map: Map

	private lateinit var optionsView: DialogPlus

	private lateinit var databaseDao: SkiingActivityDao

	/*
	private val exportJsonCallback: ActivityResultLauncher<String> = registerForActivityResult(
		ActivityResultContracts.CreateDocument(JSON_MIME_TYPE)) {
		if (it != null) {
			val json: JSONObject = getActivitySummaryJson()
			SkiingActivityManager.writeToExportFile(contentResolver, it, json.toString(4))
		}
	}

	private val exportGeoJsonCallback: ActivityResultLauncher<String> = registerForActivityResult(
		ActivityResultContracts.CreateDocument(GEOJSON_MIME_TYPE)) {
		if (it != null) {
			val json: JSONObject = getActivitySummaryJson()
			val geoJson: JSONObject = SkiingActivityManager.convertJsonToGeoJson(json)
			SkiingActivityManager.writeToExportFile(contentResolver, it, geoJson.toString(4))
		}
	}

	private val importCallback: ActivityResultLauncher<Array<String>> = registerForActivityResult(
		ActivityResultContracts.OpenDocument()) { uri: Uri? ->

		if (uri == null) {
			return@registerForActivityResult
		}

		val inputStream: InputStream? = contentResolver.openInputStream(uri)
		if (inputStream == null) {
			Log.w("importCallback", "Unable to open a file input stream for the imported file.")
			return@registerForActivityResult
		}

		val string: String = inputStream.bufferedReader().use { it.readText() }
		inputStream.close()

		val json = JSONObject(string)

		val database = ActivityDatabase(this)
		ActivityDatabase.importJsonToDatabase(json, database.writableDatabase)
		database.close()
	}
	 */

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivitySummaryBinding.inflate(layoutInflater)
		setContentView(binding.root)

		container = binding.container

		val database = Room.databaseBuilder(this, Database::class.java, Database.NAME)
			.allowMainThreadQueries().build()
		databaseDao = database.skiingActivityDao()

		fileSelectionDialog = FileSelectionDialog()

		// Be sure to show the action bar.
		if (actionBar != null) {
			actionBar!!.setDisplayShowTitleEnabled(true)
		}

		// Setup the map handler.
		map = Map()

		optionsView = DialogPlus.newDialog(this).setAdapter(MapOptionsDialog(layoutInflater,
			R.layout.activity_map_options, map)).setExpanded(false).create()

		binding.optionsButton.setOnClickListener { optionsView.show() }

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
			/*
			R.id.export_json -> exportJsonCallback.launch("exported.json")
			R.id.export_geojson -> exportGeoJsonCallback.launch("exported.geojson")
			R.id.import_activity -> importCallback.launch(arrayOf(JSON_MIME_TYPE, GEOJSON_MIME_TYPE))
			R.id.share_json -> {
				val json: JSONObject = getActivitySummaryJson()
				writeToShareFile("My Skiing Activity.json", json, JSON_MIME_TYPE)
			}
			R.id.share_geojson -> {
				val json: JSONObject = getActivitySummaryJson()
				val geojson: JSONObject = SkiingActivityManager.convertJsonToGeoJson(json)
				writeToShareFile("My Skiing Activity.geojson", geojson, GEOJSON_MIME_TYPE)
			}
			 */
			R.id.privacy_policy -> {
				val uri = Uri.parse("https://thespud.xyz/mount-spokane-ski-app/privacy/")
				val intent = Intent(Intent.ACTION_VIEW, uri)
				startActivity(intent)
			}
		}

		return super.onOptionsItemSelected(item)
	}

	/*
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
	 */

	override fun onDestroy() {
		super.onDestroy()
		map.destroy()
	}

	private fun clearScreen() {

		totalRunsNumber = 0
		binding.totalRuns.visibility = View.GONE

		absoluteMaxSpeed = 0F
		binding.maxSpeed.visibility = View.GONE

		averageSpeedSum = 0F
		binding.averageSpeed.visibility = View.GONE

		container.removeAllViews()
		map.clearMap()
		System.gc()
	}

	fun loadActivities(activities: List<SkiingActivity>) {

		clearScreen()

		if (activities.isEmpty()) {
			return
		}

		val loadingToast: Toast = Toast.makeText(this, R.string.computing_location, Toast.LENGTH_LONG)
		loadingToast.show()

		lifecycleScope.launch(Dispatchers.Default) {

			var mapMarkers: Array<MapMarker> = arrayOf()
			var activitySummaryEntries: Array<ActivitySummaryEntry> = arrayOf()
			val processingJob = launch {
				Log.d("loadActivities", "Started parsing activity from file")
				mapMarkers = Array(activities.size) { getMapMarker(activities[it]) }
				activitySummaryEntries = parseMapMarkersForMap(mapMarkers)
				Log.d("loadActivities", "Finished parsing activities from file")
			}
			processingJob.join()

			val addCirclesJob = launch { addCirclesToMap(mapMarkers) }
			val addActivitiesJob = launch { addActivity(activitySummaryEntries) }
			joinAll(addCirclesJob, addActivitiesJob)

			loadingToast.cancel()
			withContext(Dispatchers.Main) {
				Toast.makeText(this@ActivitySummary, R.string.done, Toast.LENGTH_SHORT).show()
				Log.d("loadActivities", "Done! (Adding polyline)")
				map.addPolylineFromMarker()
			}
		}
	}

	@AnyThread
	private suspend fun addCirclesToMap(mapMarkers: Array<MapMarker>) = withContext(Dispatchers.Default) {
		Log.d("loadActivities", "Started adding circles to map")
		for (mapMarker in mapMarkers) {
			val location = LatLng(mapMarker.skiingActivity.latitude, mapMarker.skiingActivity.longitude)
			val circle = withContext(Dispatchers.Main) {
				map.googleMap.addCircle { // FIXME this is using too much RAM
					center(location)
					strokeColor(mapMarker.circleColor)
					fillColor(mapMarker.circleColor)
					clickable(true)
					radius(3.0)
					zIndex(50.0F)
					visible(true)
				}
			}
			withContext(Dispatchers.Main) { circle.tag = mapMarker }
			map.circles.add(circle)
		}

		System.gc()
		Log.d("loadActivities", "Finished adding circles to map")
	}

	@AnyThread
	private suspend fun addActivity(activitySummaryEntries: Array<ActivitySummaryEntry>) = withContext(Dispatchers.Main) {
		Log.d("loadActivities", "Started creating activities view")
		for (entry in activitySummaryEntries) {
			val view: ActivityView = createActivityView(entry)
			container.addView(view)
		}

		binding.totalRuns.text = getString(R.string.total_runs, totalRunsNumber)
		binding.totalRuns.visibility = View.VISIBLE

		try {
			binding.maxSpeed.text = getString(R.string.max_speed, absoluteMaxSpeed.roundToInt())
			binding.maxSpeed.visibility = View.VISIBLE
		} catch (e: IllegalArgumentException) {
			binding.maxSpeed.visibility = View.GONE
		}

		try {
			binding.averageSpeed.text = getString(R.string.average_speed, (averageSpeedSum/totalRunsNumber).roundToInt())
			binding.averageSpeed.visibility = View.VISIBLE
		} catch (e: IllegalArgumentException) {
			binding.averageSpeed.visibility = View.GONE
		}

		System.gc()
		Log.d("loadActivities", "Finished creating activities view")
	}

	@UiThread
	private fun createActivityView(activitySummaryEntry: ActivitySummaryEntry): ActivityView {

		val activityView = ActivityView(this)

		val isRun = (activitySummaryEntry.mapMarker.icon == R.drawable.ic_chairlift ||
				activitySummaryEntry.mapMarker.icon == R.drawable.ic_easy ||
				activitySummaryEntry.mapMarker.icon == R.drawable.ic_moderate ||
				activitySummaryEntry.mapMarker.icon == R.drawable.ic_difficult)

		activityView.icon.setImageDrawable(AppCompatResources.getDrawable(this,
			activitySummaryEntry.mapMarker.icon))

		activityView.title.text = activitySummaryEntry.mapMarker.name

		// Convert from meters per second to miles per hour.
		val conversion = 0.44704f

		if (isRun) {

			val maxSpeed = (activitySummaryEntry.maxSpeed / conversion)
			activityView.maxSpeed.text = getString(R.string.max_speed, maxSpeed.roundToInt())

			val averageSpeed = (activitySummaryEntry.averageSpeed / conversion)
			activityView.averageSpeed.text = getString(R.string.average_speed, averageSpeed.roundToInt())

			if (activitySummaryEntry.mapMarker.icon != R.drawable.ic_chairlift) {
				if (maxSpeed > absoluteMaxSpeed) {
					absoluteMaxSpeed = maxSpeed
				}
				averageSpeedSum += averageSpeed
				totalRunsNumber++
			}

		} else {
			activityView.maxSpeed.visibility = View.INVISIBLE
			activityView.averageSpeed.visibility = View.INVISIBLE
		}

		activityView.startTime.text = getTimeFromLong(activitySummaryEntry.mapMarker.skiingActivity.time)

		if (activitySummaryEntry.endTime != null) {
			activityView.endTime.text = getTimeFromLong(activitySummaryEntry.endTime)
		}

		return activityView
	}

	@AnyThread
	private fun getMapMarker(skiingActivity: SkiingActivity): MapMarker {

		Locations.updateLocations(skiingActivity)

		var marker: MapMarker? = Locations.checkIfIOnChairlift(map.startingChairliftTerminals,
			map.endingChairliftTerminals)
		if (marker != null) {
			return marker
		}

		marker = Locations.checkIfOnOther(map.otherBounds)
		if (marker != null) {
			return marker
		}

		marker = Locations.checkIfOnRun(map.easyRunsBounds, map.moderateRunsBounds,
			map.difficultRunsBounds)
		if (marker != null) {
			return marker
		}

		if (Locations.previousLocation != null) {
			return getMapMarker(Locations.previousLocation!!)
		}

		Log.w("getMapMarker", "Unable to determine location")
		return MapMarker(UNKNOWN_LOCATION, Locations.currentLocation!!, R.drawable.ic_missing,
			BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA), Color.MAGENTA)
	}

	companion object {

		const val JSON_MIME_TYPE = "application/json"

		const val GEOJSON_MIME_TYPE = "application/geojson"

		const val UNKNOWN_LOCATION = "Unknown Location"

		fun getTimeFromLong(time: Long): String {
			val timeFormatter = SimpleDateFormat("h:mm:ss", Locale.US)
			val date = Date(time)
			return timeFormatter.format(date)
		}

		fun parseMapMarkersForMap(mapMarkers: Array<MapMarker>): Array<ActivitySummaryEntry> {

			// Create a place to store all the ActivitySummaryEntries.
			val arraySummaryEntries = mutableListOf<ActivitySummaryEntry>()

			var startingIndexOffset = 0
			for (i in 0..mapMarkers.size) {
				if (mapMarkers[i].name != UNKNOWN_LOCATION) {
					startingIndexOffset = i
					break
				}
			}

			var startingMapMarker = mapMarkers[startingIndexOffset]
			var maxSpeed = 0.0F
			var speedSum = 0.0F
			var sum = 0

			for (i in startingIndexOffset until mapMarkers.size) {
				val entry: MapMarker = mapMarkers[i]

				if (entry.name != UNKNOWN_LOCATION) {

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

	private inner class FileSelectionDialog: AlertDialog(this) {

		fun showDialog() {

			val binding: FileSelectionBinding = FileSelectionBinding.inflate(this.layoutInflater)

			val alertDialogBuilder = Builder(this.context)
			alertDialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
			alertDialogBuilder.setView(binding.root)

			val dialog: AlertDialog = alertDialogBuilder.create()

			val dates = databaseDao.getAllSkiingDatesWithActivities()
			for (datesWithActivities: SkiingDateWithActivities in dates) {

				// If there are no activities for the date simply don't show it
				if (datesWithActivities.skiingActivities.isEmpty()) {
					continue
				}

				val textView = TextView(this.context)
				textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT)
				textView.text = datesWithActivities.skiingDate.longDate
				textView.textSize = 25.0F
				textView.setOnClickListener {
					loadActivities(datesWithActivities.skiingActivities)
					dialog.dismiss()
				}

				binding.files.addView(textView)
			}

			dialog.show()
		}
	}

	private inner class Map : MapHandler(this@ActivitySummary), GoogleMap.InfoWindowAdapter {

		var circles: MutableList<Circle> = mutableListOf()

		private var runMarker: Marker? = null

		var polyline: Polyline? = null

		@SuppressLint("PotentialBehaviorOverride")
        override val additionalCallback: OnMapReadyCallback = OnMapReadyCallback {

			val skiingDateWithActivities = databaseDao.getSkiingDateWithActivitiesByShortDate(Database.getTodaysDate())
			if (skiingDateWithActivities != null) {
				var skiingActivities = skiingDateWithActivities.skiingActivities
				if (intent.hasExtra(SkierLocationService.ACTIVITY_SUMMARY_LAUNCH_DATE)) {
					val dateId = intent.getIntExtra(SkierLocationService.ACTIVITY_SUMMARY_LAUNCH_DATE, 0)
					skiingActivities = databaseDao.getActivitiesByDateId(dateId)

					val notificationManager: NotificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE)
							as NotificationManager
					notificationManager.cancel(SkierLocationService.ACTIVITY_SUMMARY_ID)
				}

				loadActivities(skiingActivities)
			}

			googleMap.setOnCircleClickListener {

				googleMap.setInfoWindowAdapter(this)

				val mapMarker = it.tag as MapMarker
				val location = LatLng(mapMarker.skiingActivity.latitude, mapMarker.skiingActivity.longitude)

				if (runMarker == null) {
					runMarker = googleMap.addMarker {
						position(location)
						icon(mapMarker.markerColor)
						title(mapMarker.name)
						zIndex(99.0F)
						visible(true)
					}
				} else {
					runMarker!!.position = location
					runMarker!!.setIcon(mapMarker.markerColor)
					runMarker!!.title = mapMarker.name
					runMarker!!.isVisible = true
				}

				runMarker!!.isVisible = true
				runMarker!!.tag = mapMarker
				runMarker!!.showInfoWindow()
			}

			googleMap.setOnInfoWindowCloseListener { it.isVisible = false }
		}

		override fun destroy() {
			super.destroy()
			clearMap()
		}

		fun clearMap() {
			for (circle in circles) {
				circle.remove()
			}
			circles.clear()

			if (polyline != null) {
				polyline!!.remove()
				polyline = null
			}
		}

		override fun getInfoContents(marker: Marker): View? {
			Log.v("CustomInfoWindow", "getInfoContents called")

			if (marker.tag !is MapMarker) {
				return null
			}

			val markerView: View = layoutInflater.inflate(R.layout.info_window, null)
			val name: TextView = markerView.findViewById(R.id.marker_name)

			val markerInfo: MapMarker = marker.tag as MapMarker
			name.text = markerInfo.name

			val altitude: TextView = markerView.findViewById(R.id.marker_altitude)

			// Convert from meters to feet.
			val altitudeConversion = 3.280839895f

			try {
				altitude.text = getString(R.string.marker_altitude,
					(markerInfo.skiingActivity.altitude * altitudeConversion).roundToInt())
			} catch (e: IllegalArgumentException) {
				altitude.text = getString(R.string.marker_altitude, 0)
			}

			val speed: TextView = markerView.findViewById(R.id.marker_speed)

			// Convert from meters per second to miles per hour.
			val speedConversion = 0.44704f

			try {
				speed.text = getString(R.string.marker_speed,
						(markerInfo.skiingActivity.speed / speedConversion).roundToInt())
			} catch (e: IllegalArgumentException) {
				speed.text = getString(R.string.marker_speed, 0)
			}

			return markerView
		}

		override fun getInfoWindow(marker: Marker): View? {
			Log.v("CustomInfoWindow", "getInfoWindow called")
			return null
		}

		@UiThread
		fun addPolylineFromMarker() {

			polyline = googleMap.addPolyline {

				for (circle in circles) {
					add(circle.center)
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
	}
}