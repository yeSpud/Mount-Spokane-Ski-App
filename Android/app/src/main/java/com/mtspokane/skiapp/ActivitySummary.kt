package com.mtspokane.skiapp

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isNotEmpty
import androidx.fragment.app.FragmentActivity
import androidx.room.Room
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding
import com.mtspokane.skiapp.databinding.FileSelectionBinding
import com.orhanobut.dialogplus.DialogPlus
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import xyz.thespud.skimap.activities.InfoMapActivity
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import xyz.thespud.skimap.dialogs.InfoMapOptionsDialog
import xyz.thespud.skimap.locationmanager.SkiAreaObjects
import xyz.thespud.skimap.mapItem.InfoMapMarker
import xyz.thespud.skimap.mapItem.SkiRun

class ActivitySummary : FragmentActivity() {

	private lateinit var binding: ActivitySummaryBinding

	private var totalRunsNumber = 0
	private var absoluteMaxSpeed = 0F
	private var averageSpeedSum = 0F

	private lateinit var fileSelectionDialog: FileSelectionDialog

	private lateinit var map: InfoMapActivity

	private lateinit var optionsView: DialogPlus

	private lateinit var databaseDao: SkiingActivityDao

	private val exportJsonCallback: ActivityResultLauncher<String> = registerForActivityResult(
		ActivityResultContracts.CreateDocument(JSON_MIME_TYPE)) {
		if (it != null) {
			val json: JSONObject = convertSkiingActivitiesToJson()
			writeToExportFile(it, json.toString(4))
		}
	}

	private val exportGeoJsonCallback: ActivityResultLauncher<String> = registerForActivityResult(
		ActivityResultContracts.CreateDocument(GEOJSON_MIME_TYPE)) {
		if (it != null) {
			val json: JSONObject = convertSkiingActivitiesToJson()
			val geoJson: JSONObject = convertJsonToGeoJson(json)
			writeToExportFile(it, geoJson.toString(4))
		}
	}

	private val importCallback: ActivityResultLauncher<Array<String>> = registerForActivityResult(
		ActivityResultContracts.OpenDocument()) { uri: Uri? ->

		if (uri == null) { return@registerForActivityResult }

		val tag = "importCallback"

		val inputStream: InputStream? = contentResolver.openInputStream(uri)
		if (inputStream == null) {
			Log.w(tag, "Unable to open a file input stream for the imported file")
			return@registerForActivityResult
		}

		val string: String = inputStream.bufferedReader().use { it.readText() }
		inputStream.close()

		val json = JSONObject(string)
		val importedLongName: String = json.keys().next()

		val skiingActivities = json.getJSONArray(importedLongName)
		if (skiingActivities.length() == 0) {
			return@registerForActivityResult
		}

		val shortDate = Database.getShortDateFromLong(skiingActivities.getJSONObject(0).getLong(TIME))
		if (!shortDate.matches("\\d{4}-\\d{1,2}-\\d{1,2}".toRegex())) {
			val msg = "Unable to import skiing activity"
			Toast.makeText(this, msg, Toast.LENGTH_SHORT)
			Log.w(tag, msg)
			return@registerForActivityResult
		}

		val shortName = "${shortDate}-imported"
		val skiingDates = databaseDao.getAllSkiingDatesWithActivities()
		if (skiingDates.find { it.skiingDate.shortDate == shortName } != null) {
			val msg = "Unable to import skiing activity - name already in use"
			Toast.makeText(this, msg, Toast.LENGTH_LONG)
			Log.w(tag, msg)
			return@registerForActivityResult
		}

		databaseDao.addSkiingDate(LongAndShortDate("$importedLongName (Imported)", shortName))

		val skiingDate = databaseDao.getSkiingDateWithActivitiesByShortDate(shortName)
		if (skiingDate == null) {
			val msg = "Unable to import skiing activity"
			Toast.makeText(this, msg, Toast.LENGTH_SHORT)
			Log.w(tag, msg)
			return@registerForActivityResult
		}

		for (i in 0 until skiingActivities.length()) {
			val skiingActivity: JSONObject = skiingActivities.getJSONObject(i)

			databaseDao.addSkiingActivity(
				SkiingActivity(
					skiingActivity.getDouble(ACCURACY).toFloat(),
					skiingActivity.getDouble(ALTITUDE),
					skiingActivity.optDouble(ALTITUDE_ACCURACY).toFloat(),
					skiingActivity.getDouble(LATITUDE),
					skiingActivity.getDouble(LONGITUDE),
					skiingActivity.getDouble(SPEED).toFloat(),
					skiingActivity.optDouble(SPEED_ACCURACY).toFloat(),
					skiingActivity.getLong(TIME),
					skiingDate.skiingDate.id
				)
			)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Be sure to show the action bar.
		if (actionBar != null) { actionBar!!.setDisplayShowTitleEnabled(true) }

		binding = ActivitySummaryBinding.inflate(layoutInflater)
		setContentView(binding.root)

		// Fix edge to edge behavior
		ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			val lpad = systemBars.left
			val tpad = systemBars.top
			val rpad = systemBars.right
			val bpad = systemBars.bottom

			// For the activity summary the right padding is useless since its always in the center
			val summaryParams: ViewGroup.MarginLayoutParams = binding.activitySummary.layoutParams as ViewGroup.MarginLayoutParams
			if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
				summaryParams.setMargins(
					summaryParams.leftMargin + lpad,
					summaryParams.topMargin + tpad,0,
					summaryParams.bottomMargin
				)
			} else {
				summaryParams.setMargins(
					summaryParams.leftMargin + lpad,
					summaryParams.topMargin + tpad,0,
					summaryParams.bottomMargin + bpad
				)
			}
			binding.activitySummary.layoutParams = summaryParams

			val buttonParams: ViewGroup.MarginLayoutParams = binding.optionsButton.layoutParams as ViewGroup.MarginLayoutParams
			if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
				buttonParams.setMargins(
					buttonParams.leftMargin + lpad, buttonParams.topMargin,
					buttonParams.rightMargin + rpad, buttonParams.bottomMargin + bpad
				)
			} else {
				buttonParams.setMargins(
					buttonParams.leftMargin + lpad, buttonParams.topMargin + tpad,
					buttonParams.rightMargin + rpad, buttonParams.bottomMargin + bpad
				)
			}

			binding.optionsButton.layoutParams = buttonParams

			binding.optionsButton.setOnClickListener {
				optionsView.holderView.setPadding(lpad, 0, rpad, bpad)
				optionsView.show()
			}

			insets
		}

		fileSelectionDialog = FileSelectionDialog()

		// Load the map polylines and polygons
		val skiAreaObjects = SkiAreaObjects(R.raw.bounds)
		skiAreaObjects.chairliftsPolylines = R.raw.lifts
		skiAreaObjects.greenRunPolylines = R.raw.easy
		skiAreaObjects.blueRunPolylines = R.raw.moderate
		skiAreaObjects.blackRunPolylines = R.raw.difficult
		skiAreaObjects.chairliftBounds = R.raw.lift_polygons
		skiAreaObjects.chairliftTerminals = R.raw.lift
		skiAreaObjects.greenRunBounds = R.raw.easy_polygons
		skiAreaObjects.blueRunBounds = R.raw.moderate_polygons
		skiAreaObjects.blackRunBounds = R.raw.difficult_polygons
		skiAreaObjects.other = R.raw.other

		val cameraPosition = CameraPosition.Builder()
			.target(LatLng(47.92517834073426, -117.10480503737926))
			.tilt(45F)
			.bearing(317.50552F)
			.zoom(14.414046F).build()

		val cameraBounds = LatLngBounds(LatLng(47.912728, -117.133402), LatLng(47.943674, -117.092470))

		// Setup the map handler.
		// The top and left padding are useless to us
		// because the map is always at the bottom while in portrait and on the right in landscape
		map = InfoMapActivity(this, binding.activityMap, cameraPosition, cameraBounds, skiAreaObjects, Icons())

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.activity_map) as SupportMapFragment
		mapFragment.getMapAsync(map)

		optionsView = DialogPlus.newDialog(this)
			.setAdapter(InfoMapOptionsDialog(map))
			.setExpanded(false)
			.setContentBackgroundResource(R.color.dark_blue)
			.create()

		val database = Room.databaseBuilder(this, Database::class.java, Database.NAME)
			.allowMainThreadQueries().build()
		databaseDao = database.skiingActivityDao()

		// Stop showing the activity notification
		val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.cancel(ACTIVITY_SUMMARY_ID)

		val dateId = intent.getIntExtra(ACTIVITY_SUMMARY_LAUNCH_DATE, 0)
		loadActivities(dateId)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.summary_menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {

		val shareMenu: MenuItem = menu.findItem(R.id.share)
		val exportMenu: MenuItem = menu.findItem(R.id.export)

		shareMenu.isEnabled = binding.container.isNotEmpty()
		exportMenu.isEnabled = binding.container.isNotEmpty()

		return super.onPrepareOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.open -> fileSelectionDialog.showDialog()
			R.id.export_json -> exportJsonCallback.launch("exported.json") // TODO Change my name
			R.id.export_geojson -> exportGeoJsonCallback.launch("exported.geojson") // TODO Change my name
			R.id.import_activity -> importCallback.launch(arrayOf(JSON_MIME_TYPE, GEOJSON_MIME_TYPE))
			R.id.share_json -> {
				val json: JSONObject = convertSkiingActivitiesToJson()
				writeToShareFile("My Skiing Activity.json", json, JSON_MIME_TYPE) // TODO Change my name
			}
			R.id.share_geojson -> {
				val json: JSONObject = convertSkiingActivitiesToJson()
				val geojson: JSONObject = convertJsonToGeoJson(json)
				writeToShareFile("My Skiing Activity.geojson", geojson, GEOJSON_MIME_TYPE) // TODO Change my name
			}
			R.id.privacy_policy -> {
				val uri = "https://thespud.xyz/mount-spokane-ski-app/privacy/".toUri()
				val intent = Intent(Intent.ACTION_VIEW, uri)
				startActivity(intent)
			}
		}

		return super.onOptionsItemSelected(item)
	}

	private fun writeToExportFile(uri: Uri, outText: String) {
		val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
		outputStream?.use { it.write(outText.toByteArray()) }
	}

	private fun writeToShareFile(filename: String, jsonToWrite: JSONObject, mime: String) {

		val tmpFile = File(filesDir, filename)
		if (tmpFile.exists()) {
			tmpFile.delete()
		}

		openFileOutput(filename, MODE_PRIVATE).use { it.write(jsonToWrite.toString(4).toByteArray()) }

		val providerString = "${packageName}.provider"
		Log.v("shareFIle", "Provider string: $providerString")
		val fileUri: Uri = FileProvider.getUriForFile(this, providerString, tmpFile)

		val sharingIntent = Intent(Intent.ACTION_SEND)
		sharingIntent.type = mime
		sharingIntent.putExtra(Intent.EXTRA_STREAM, fileUri)

		val chooserIntent = Intent.createChooser(sharingIntent, getText(R.string.share_description))
		startActivity(chooserIntent)
	}

	private fun convertSkiingActivitiesToJson(): JSONObject {

		if (map.loadedSkiRuns.isEmpty()) {
			val emptyObject = JSONObject()
			emptyObject.put(Database.getTodaysDate(), JSONArray())
			return emptyObject
		}

		val date = Database.getLongDateFromLong(map.loadedSkiRuns[0].startTime)

		val jsonArray = JSONArray()
		for (skiingActivity in map.loadedSkiRuns) {
			for (location in skiingActivity.locations) {
				val activityObject = JSONObject()
				activityObject.put(ACCURACY, location.accuracy)
				activityObject.put(ALTITUDE, location.altitude)
				activityObject.put(ALTITUDE_ACCURACY, location.verticalAccuracyMeters)
				activityObject.put(LATITUDE, location.latitude)
				activityObject.put(LONGITUDE, location.longitude)
				activityObject.put(SPEED, location.speed)
				activityObject.put(SPEED_ACCURACY, location.speedAccuracyMetersPerSecond)
				activityObject.put(TIME, location.time)

				jsonArray.put(activityObject)
			}
		}

		val jsonObject = JSONObject()
		jsonObject.put(date, jsonArray)
		return jsonObject
	}

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

		binding.container.removeAllViews()
		map.clearMap()

		map.locationManager?.resetLocations()
	}

	fun loadActivities(dateId: Int) {
		val tag = "loadActivities"
		clearScreen()

		if (dateId == 0) { return }

		val loadingToast: Toast = Toast.makeText(this, R.string.computing_location, Toast.LENGTH_LONG)
		loadingToast.show()

		val activites = databaseDao.getActivitiesByDateId(dateId)

		if (activites.isEmpty()) { return }

		lifecycleScope.launch(Dispatchers.Default) {

			val processingJob = launch {
				Log.d(tag, "Started parsing activity from file")

				val locationManager = map.locationManager
				if (locationManager == null) {
					Log.w(tag, "Location manager is null!")
					return@launch
				}

				val mapMarkers = mutableListOf<InfoMapMarker>()
				for (skiingActivity in activites) {
					val location = Database.skiingActivityToLocation(skiingActivity)
					val mapMarker = locationManager.getMapMarker(location)
					mapMarkers.add(mapMarker)
				}
				map.loadSkiRuns(mapMarkers)

				addActivity()
				Log.d("loadActivities", "Finished parsing activities from file")
			}
			processingJob.join()

			loadingToast.cancel()
			withContext(Dispatchers.Main) {
				Toast.makeText(this@ActivitySummary, R.string.done, Toast.LENGTH_SHORT).show()
				Log.d("loadActivities", "Done! (Adding polyline)")
			}
		}
	}

	/*
	private fun parseMapMarkersForMap(): Array<ActivitySummaryEntry> {

		// Create a place to store all the ActivitySummaryEntries.
		val arraySummaryEntries = mutableListOf<ActivitySummaryEntry>()

		var startingIndexOffset = 0
		for (i in loadedMapMarkers.indices) {
			if (loadedMapMarkers[i].name != UNKNOWN_LOCATION) {
				startingIndexOffset = i
				break
			}
		}

		var startingMapMarker = loadedMapMarkers[startingIndexOffset]
		var maxSpeed = 0.0F
		var speedSum = 0.0F
		var sum = 0

		for (i in startingIndexOffset until loadedMapMarkers.size) {
			val entry: MapMarker = loadedMapMarkers[i]

			if (entry.name != UNKNOWN_LOCATION) {

				if (entry.name != startingMapMarker.name) {

					val newActivitySummaryEntry = ActivitySummaryEntry(startingMapMarker, maxSpeed,
						speedSum/sum, entry.location.time)
					arraySummaryEntries.add(newActivitySummaryEntry)

					maxSpeed = 0.0F
					speedSum = 0.0F
					sum = 0
					startingMapMarker = entry
				}
			}

			if (entry.location.speed > maxSpeed) {
				maxSpeed = entry.location.speed
			}
			speedSum += entry.location.speed
			++sum
		}

		val finalActivitySummary = ActivitySummaryEntry(startingMapMarker, maxSpeed,
			speedSum/sum, loadedMapMarkers.last().location.time)
		arraySummaryEntries.add(finalActivitySummary)

		return arraySummaryEntries.toTypedArray()
	}*/

	@AnyThread
	private suspend fun addActivity() = withContext(Dispatchers.Main) {
		Log.d("addActivity", "Started creating activities view")
		for (skiRun in map.loadedSkiRuns) {
			val view: ActivityView = createActivityView(skiRun)
			binding.container.addView(view)
		}

		binding.totalRuns.text = getString(R.string.total_runs, totalRunsNumber)
		binding.totalRuns.visibility = View.VISIBLE

		try {
			binding.maxSpeed.text = getString(R.string.max_speed, absoluteMaxSpeed.roundToInt())
			binding.maxSpeed.visibility = View.VISIBLE
		} catch (_: IllegalArgumentException) {
			binding.maxSpeed.visibility = View.GONE
		}

		try {
			binding.averageSpeed.text = getString(R.string.average_speed, (averageSpeedSum/totalRunsNumber).roundToInt())
			binding.averageSpeed.visibility = View.VISIBLE
		} catch (_: IllegalArgumentException) {
			binding.averageSpeed.visibility = View.GONE
		}

		System.gc()
		Log.d("addActivity", "Finished creating activities view")
	}

	@UiThread
	private fun createActivityView(skiRun: SkiRun): ActivityView {
		val activityView = ActivityView(this)

		val isRun = (skiRun.icon == xyz.thespud.skimap.R.drawable.ic_chairlift ||
				skiRun.icon == xyz.thespud.skimap.R.drawable.ic_green ||
				skiRun.icon == xyz.thespud.skimap.R.drawable.ic_blue ||
				skiRun.icon == xyz.thespud.skimap.R.drawable.ic_black)

		activityView.icon.setImageDrawable(AppCompatResources.getDrawable(this, skiRun.icon))
		activityView.title.text = skiRun.name

		// Convert from meters per second to miles per hour.
		val conversion = 0.44704f

		if (isRun) {

			val maxSpeed = (skiRun.maxSpeed / conversion)
			activityView.maxSpeed.text = getString(R.string.max_speed, maxSpeed.roundToInt())

			val averageSpeed = (skiRun.averageSpeed / conversion)
			activityView.averageSpeed.text = getString(R.string.average_speed, averageSpeed.roundToInt())

			if (skiRun.icon != xyz.thespud.skimap.R.drawable.ic_chairlift) {
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

		activityView.startTime.text = getTimeFromLong(skiRun.startTime)
		activityView.endTime.text = getTimeFromLong(skiRun.endTime)

		return activityView
	}

	companion object {

		const val JSON_MIME_TYPE = "application/json"
		const val GEOJSON_MIME_TYPE = "application/geojson"

		private const val ACCURACY = "acc"
		private const val ALTITUDE = "alt"
		private const val ALTITUDE_ACCURACY = "altacc"
		private const val LATITUDE = "lat"
		private const val LONGITUDE = "lng"
		private const val SPEED = "speed"
		private const val SPEED_ACCURACY = "speedacc"
		private const val TIME = "time"

		const val ACTIVITY_SUMMARY_LAUNCH_DATE = "xyz.thespud.mtspokane.date"
		const val ACTIVITY_SUMMARY_CHANNEL_ID = "skiAppProgress"
		const val ACTIVITY_SUMMARY_ID = 5887

		fun getTimeFromLong(time: Long): String {
			val timeFormatter = SimpleDateFormat("h:mm:ss", Locale.US)
			val date = Date(time)
			return timeFormatter.format(date)
		}

		fun convertJsonToGeoJson(json: JSONObject): JSONObject {

			val geoJson = JSONObject()
			geoJson.put("type", "FeatureCollection")

			val key: String = json.keys().next()
			val jsonArray: JSONArray = json.getJSONArray(key)

			val featureArray = JSONArray()
			for (i in 0 until jsonArray.length()) {

				val featureEntry = JSONObject()
				featureEntry.put("type", "Feature")

				val geometryJson = JSONObject()
				geometryJson.put("type", "Point")

				val coordinateJson = JSONArray()
				val jsonEntry: JSONObject = jsonArray.getJSONObject(i)
				coordinateJson.put(0, jsonEntry.getDouble(LONGITUDE))
				coordinateJson.put(1, jsonEntry.getDouble(LATITUDE))
				coordinateJson.put(2, jsonEntry.getDouble(ALTITUDE))
				geometryJson.put("coordinates", coordinateJson)
				featureEntry.put("geometry", geometryJson)

				val propertiesJson = JSONObject()
				for (properties in listOf(ACCURACY, ALTITUDE_ACCURACY, SPEED, SPEED_ACCURACY, TIME)) {
					propertiesJson.put(properties, jsonEntry.opt(properties))
				}
				featureEntry.put("properties", propertiesJson)

				featureArray.put(featureEntry)
			}

			geoJson.put("features", featureArray)
			return geoJson
		}
	}

	private inner class FileSelectionDialog: AlertDialog(this) {

		fun showDialog() {

			val binding: FileSelectionBinding = FileSelectionBinding.inflate(layoutInflater)

			val alertDialogBuilder = Builder(context)
			alertDialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
			alertDialogBuilder.setView(binding.root)

			val dialog: AlertDialog = alertDialogBuilder.create()

			val dates = databaseDao.getAllSkiingDatesWithActivities()
			for (datesWithActivities: SkiingDateWithActivities in dates) {

				// If there are no activities for the date simply don't show it
				if (datesWithActivities.skiingActivities.isEmpty()) { continue }

				val textView = TextView(context)
				textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT)
				textView.text = datesWithActivities.skiingDate.longDate
				textView.textSize = 25.0F
				textView.setOnClickListener {
					loadActivities(datesWithActivities.skiingDate.id)
					dialog.dismiss()
				}

				binding.files.addView(textView)
			}

			dialog.show()
		}
	}


	/*
	private inner class Map(lpad: Int, tpad: Int, rpad: Int, bpad: Int, skiRuns: SkiRuns) : InfoMapActivity(
		this@ActivitySummary, lpad, tpad, rpad, bpad, skiRuns) {

		@SuppressLint("PotentialBehaviorOverride")
        override val additionalCallback: OnMapReadyCallback = OnMapReadyCallback {
			super.additionalCallback

			val skiingDateWithActivities = databaseDao.getSkiingDateWithActivitiesByShortDate(
				Database.getTodaysDate())
			if (skiingDateWithActivities != null) {
				loadedSkiingActivities = skiingDateWithActivities.skiingActivities
				if (intent.hasExtra(SkiingNotification.ACTIVITY_SUMMARY_LAUNCH_DATE)) {
					val dateId = intent.getIntExtra(SkiingNotification.ACTIVITY_SUMMARY_LAUNCH_DATE, 0)
					loadedSkiingActivities = databaseDao.getActivitiesByDateId(dateId)

					val notificationManager: NotificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE)
							as NotificationManager
					notificationManager.cancel(SkiingNotification.ACTIVITY_SUMMARY_ID)
				}

				drawLoadedSkiingActivities()
			}
		}
	}*/
}