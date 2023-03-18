package com.mtspokane.skiapp.activities

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.mtspokane.skiapp.databases.ActivityDatabase
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding
import com.mtspokane.skiapp.databases.SkiingActivity
import com.mtspokane.skiapp.databases.SkiingActivityManager
import com.mtspokane.skiapp.databases.TimeManager
import com.mtspokane.skiapp.databinding.FileSelectionBinding
import com.mtspokane.skiapp.mapItem.MapMarker
import com.mtspokane.skiapp.mapItem.PolylineMapItem
import com.mtspokane.skiapp.maphandlers.MapHandler
import com.mtspokane.skiapp.maphandlers.CustomDialogEntry
import com.mtspokane.skiapp.maphandlers.ActivitySummaryLocationMarkers
import com.orhanobut.dialogplus.DialogPlus
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import kotlin.math.roundToInt
import org.json.JSONObject

class ActivitySummary : FragmentActivity() {

	private lateinit var totalRuns: TextView
	private var totalRunsNumber = 0

	private lateinit var maxSpeed: TextView
	private var absoluteMaxSpeed = 0F

	private lateinit var averageSpeed: TextView
	private var averageSpeedSum = 0F

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

		val string: String = inputStream.bufferedReader().use { it.readText() }
		inputStream.close()

		val json = JSONObject(string)

		val database = ActivityDatabase(this)
		ActivityDatabase.importJsonToDatabase(json, database.writableDatabase)
		database.close()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val binding: ActivitySummaryBinding = ActivitySummaryBinding.inflate(layoutInflater)
		setContentView(binding.root)

		totalRuns = binding.toalRuns
		maxSpeed = binding.maxSpeed
		averageSpeed = binding.averageSpeed
		container = binding.container

		fileSelectionDialog = FileSelectionDialog()

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

	private fun clearScreen() {

		totalRunsNumber = 0
		totalRuns.visibility = View.INVISIBLE

		absoluteMaxSpeed = 0F
		maxSpeed.visibility = View.INVISIBLE

		averageSpeedSum = 0F
		averageSpeed.visibility = View.INVISIBLE

		container.removeAllViews()
	}

	fun loadActivities(activities: Array<SkiingActivity>) {

		clearScreen()

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

		if (activities.isEmpty()) {
			return
		}

		val loadingToast: Toast = Toast.makeText(this, R.string.computing_location, Toast.LENGTH_LONG)
		lifecycleScope.launch(Dispatchers.IO, CoroutineStart.LAZY) {

			//async(Dispatchers.IO, CoroutineStart.LAZY) {
			loadingToast.show()

			val mapMarkers: Array<MapMarker> = MapMarker.loadFromSkiingActivityArray(activities)
			for (mapMarker in mapMarkers) {
				addMapMarkerToMap(mapMarker)
			}

			val activitySummaryEntries: Array<ActivitySummaryEntry> = parseMapMarkersForMap(mapMarkers)

			withContext(Dispatchers.Main) {

				for (entry in activitySummaryEntries) {
					val view: ActivityView = this@ActivitySummary.createActivityView(entry)
					container.addView(view)
				}

				totalRuns.text = getString(R.string.total_runs, totalRunsNumber)
				totalRuns.visibility = View.VISIBLE

				try {
					maxSpeed.text = getString(R.string.max_speed, absoluteMaxSpeed.roundToInt())
					maxSpeed.visibility = View.VISIBLE
				} catch (e: IllegalArgumentException) {
					maxSpeed.visibility = View.INVISIBLE
				}

				try {
					averageSpeed.text = getString(R.string.average_speed,
							(averageSpeedSum/totalRunsNumber).roundToInt())
					averageSpeed.visibility = View.VISIBLE
				} catch (e: IllegalArgumentException) {
					averageSpeed.visibility = View.INVISIBLE
				}

				loadingToast.cancel()
				Toast.makeText(this@ActivitySummary, R.string.done, Toast.LENGTH_SHORT).show()
				map.addPolylineFromMarker()
			}
			//}.await()
		}.start()

		//loadingToast.show()
	}

	@AnyThread
	private suspend fun addMapMarkerToMap(mapMarker: MapMarker) = coroutineScope {

		val snippetText: String? = if (BuildConfig.DEBUG) {

			val altitudeString = "Altitude: ${mapMarker.debugAltitude}"
			val speedString = "Speed: ${mapMarker.debugSpeed}"
			val verticalDirectionString = "Vertical: ${mapMarker.debugVertical.name}"

			"$altitudeString | $speedString | $verticalDirectionString"
		} else {
			null
		}

		withContext(Dispatchers.Main) {
			val activitySummaryLocation = ActivitySummaryLocationMarkers(map.map, mapMarker, snippetText)
			map.locationMarkers.add(activitySummaryLocation)
		}
	}

	@MainThread
	private fun createActivityView(activitySummaryEntry: ActivitySummaryEntry): ActivityView {

		val activityView = ActivityView(this)

		val isNotRun = !(activitySummaryEntry.mapMarker.icon == R.drawable.ic_chairlift ||
				activitySummaryEntry.mapMarker.icon == R.drawable.ic_easy ||
				activitySummaryEntry.mapMarker.icon == R.drawable.ic_moderate ||
				activitySummaryEntry.mapMarker.icon == R.drawable.ic_difficult)

		activityView.icon.setImageDrawable(AppCompatResources.getDrawable(this, activitySummaryEntry.mapMarker.icon))

		activityView.title.text = activitySummaryEntry.mapMarker.name

		// Convert from meters per second to miles per hour.
		val conversion = 0.44704f

		if (!isNotRun) {

			totalRunsNumber++

			try {
				val maxSpeed = (activitySummaryEntry.maxSpeed / conversion)
				if (maxSpeed > absoluteMaxSpeed) {
					absoluteMaxSpeed = maxSpeed
				}
				activityView.maxSpeed.text = this.getString(R.string.max_speed, maxSpeed.roundToInt())
			} catch (e: IllegalArgumentException) {
				activityView.maxSpeed.text = this.getString(R.string.max_speed, 0)
				activityView.maxSpeed.visibility = View.INVISIBLE
			}

			try {
				val averageSpeed = (activitySummaryEntry.averageSpeed / conversion)
				averageSpeedSum += averageSpeed
				activityView.averageSpeed.text = this.getString(R.string.average_speed, averageSpeed.roundToInt())
			} catch (e: IllegalArgumentException) {
				activityView.averageSpeed.text = this.getString(R.string.average_speed, 0)
				activityView.averageSpeed.visibility = View.INVISIBLE
			}
		} else {
			activityView.maxSpeed.visibility = View.INVISIBLE
			activityView.averageSpeed.visibility = View.INVISIBLE
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
				val entry: MapMarker = mapMarkers[i]

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

	private inner class FileSelectionDialog: AlertDialog(this) {

		fun showDialog() {

			val binding: FileSelectionBinding = FileSelectionBinding.inflate(this.layoutInflater)

			val alertDialogBuilder = Builder(this.context)
			alertDialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
			alertDialogBuilder.setView(binding.root)

			val dialog: AlertDialog = alertDialogBuilder.create()

			val db = ActivityDatabase(this@ActivitySummary)
			val dates: Array<String> = ActivityDatabase.getTables(db.readableDatabase)
			db.close()

			for (date in dates) {

				val textView = TextView(this.context)
				textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT)
				textView.text = date
				textView.textSize = 25.0F
				textView.setOnClickListener {

					val database = ActivityDatabase(this@ActivitySummary)
					SkiingActivityManager.FinishedAndLoadedActivities = ActivityDatabase
							.readSkiingActivesFromDatabase(date, database.readableDatabase)
					database.close()

					if (SkiingActivityManager.FinishedAndLoadedActivities != null) {
						loadActivities(SkiingActivityManager.FinishedAndLoadedActivities!!)
					}

					dialog.dismiss()
				}

				binding.files.addView(textView)
			}

			dialog.show()
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
							chairliftPolyline.togglePolyLineVisibility(chairliftPolyline.defaultVisibility,
									isNightOnly)
						}

						for (easyRunPolyline in easyRunsPolylines) {
							easyRunPolyline.togglePolyLineVisibility(easyRunPolyline.defaultVisibility,
									isNightOnly)
						}

						for (moderateRunPolyline in moderateRunsPolylines) {
							moderateRunPolyline.togglePolyLineVisibility(moderateRunPolyline.defaultVisibility,
									isNightOnly)
						}

						for (difficultRunPolyline in difficultRunsPolylines) {
							difficultRunPolyline.togglePolyLineVisibility(difficultRunPolyline.defaultVisibility,
									isNightOnly)
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
}

class ActivityView : ConstraintLayout {

	val icon: ImageView

	val title: TextView

	val maxSpeed: TextView

	val averageSpeed: TextView

	val startTime: TextView

	val endTime: TextView

	constructor(context: Context): this(context, null)

	constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)

	constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context,
			attributeSet, defStyleAttr) {

		inflate(context, R.layout.activity_view, this)

		icon = findViewById(R.id.icon)
		title = findViewById(R.id.title_view)
		maxSpeed = findViewById(R.id.max_speed)
		averageSpeed = findViewById(R.id.average_speed)
		startTime = findViewById(R.id.start_time)
		endTime = findViewById(R.id.end_time)
	}
}

data class ActivitySummaryEntry(val mapMarker: MapMarker, val maxSpeed: Float, val averageSpeed: Float, val endTime: Long?)