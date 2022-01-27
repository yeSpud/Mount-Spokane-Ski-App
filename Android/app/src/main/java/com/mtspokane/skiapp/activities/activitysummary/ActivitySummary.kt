package com.mtspokane.skiapp.activities.activitysummary

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
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
import com.mtspokane.skiapp.databases.ActivityDatabase
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding
import com.mtspokane.skiapp.databinding.FileSelectionBinding
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.maphandlers.ActivitySummaryMap
import com.mtspokane.skiapp.databases.SkiingActivity
import com.mtspokane.skiapp.databases.SkiingActivityManager
import com.mtspokane.skiapp.databases.TimeManager
import java.io.File
import java.io.InputStream
import java.util.LinkedList
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
			R.id.export_json -> this.exportJsonCallback.launch("exported.json")
			R.id.export_geojson -> this.exportGeoJsonCallback.launch("exported.geojson")
			R.id.share_json -> {

				val json: JSONObject = this.getActivitySummaryJson()

				val tmpFileName = "My Skiing Activity.json"
				this.openFileOutput(tmpFileName, Context.MODE_PRIVATE).use {
					it.write(json.toString(4).toByteArray())
				}

				val tmpFile = File(this.filesDir, tmpFileName)

				SkiingActivityManager.shareFile(this, tmpFile, JSON_MIME_TYPE)

				tmpFile.delete()
			}
			R.id.share_geojson -> {

				val json: JSONObject = this.getActivitySummaryJson()

				val geojson: JSONObject = SkiingActivityManager.convertJsonToGeoJson(json)

				val tmpFileName = "My Skiing Activity.geojson"
				this.openFileOutput(tmpFileName, Context.MODE_PRIVATE).use {
					it.write(geojson.toString(4).toByteArray())
				}

				val tmpFile = File(this.filesDir, tmpFileName)

				SkiingActivityManager.shareFile(this, tmpFile, GEOJSON_MIME_TYPE)

				tmpFile.delete()
			}
			R.id.import_activity -> this.importCallback.launch(arrayOf(JSON_MIME_TYPE, GEOJSON_MIME_TYPE))
			R.id.credits -> this.creditDialog.show()
		}

		return super.onOptionsItemSelected(item)
	}

	private fun getActivitySummaryJson(): JSONObject {
		return if (SkiingActivityManager.FinishedAndLoadedActivities != null) {
			SkiingActivityManager.convertSkiingActivitiesToJson(SkiingActivityManager.FinishedAndLoadedActivities!!)
		} else {
			SkiingActivityManager.convertSkiingActivitiesToJson(SkiingActivityManager.InProgressActivities)
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
	private suspend fun addCircleAndMarker(titleDrawableResourceMarkerIcon: ActivityItem,
	                                       skiingActivity: SkiingActivity): Unit = coroutineScope {

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
			this@ActivitySummary.mapHandler!!.addActivitySummaryLocationMarker(titleDrawableResourceMarkerIcon.name,
				skiingActivity.latitude, skiingActivity.longitude, titleDrawableResourceMarkerIcon.drawableResource,
				titleDrawableResourceMarkerIcon.markerIcon, snippetText)
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
		val startingActivityItem: ActivityItem = this@ActivitySummary.getActivityItem()
		Log.v(tag, "Starting with ${startingActivityItem.name} (${linkedList.size} items left)")

		// Add the starting circle to the map.
		this@ActivitySummary.addCircleAndMarker(startingActivityItem, startingActivity)

		// If the starting location is unknown skip it.
		if (startingActivityItem.name == UNKNOWN_LOCATION) {
			this@ActivitySummary.addToViewRecursively(linkedList)
			return@coroutineScope
		}

		// Get the ending activity, and continue to update it until the list is empty (or other... see below).
		var endingActivity: SkiingActivity? = null
		while(linkedList.isNotEmpty()) {

			// Remove the now first item in the list as a potential ending activity.
			val potentialEndingActivity: SkiingActivity = linkedList.first

			// Update the locations.
			ActivitySummaryLocations.updateLocations(potentialEndingActivity)

			// Get the ending activity item.
			val potentialEndingActivityItem: ActivityItem = this@ActivitySummary.getActivityItem()

			// Add the circle to the map.
			this@ActivitySummary.addCircleAndMarker(potentialEndingActivityItem, potentialEndingActivity)

			// If the ending activity isn't unknown then check if it has matching names.
			if (potentialEndingActivityItem.name != UNKNOWN_LOCATION) {

				// If the names don't match exit the while loop.
				if (startingActivityItem.name != potentialEndingActivityItem.name) {

					Log.v(tag, "Next item should be ${potentialEndingActivityItem.name} (${linkedList.size} items left)")
					break
				}
			}

			// Otherwise continue looping and officially remove that first item from the list.
			endingActivity = linkedList.removeFirst()
		}

		withContext(Dispatchers.Main) {

			val view: ActivityView = if (endingActivity == null) {
				this@ActivitySummary.createActivityView(startingActivityItem.drawableResource,
					startingActivityItem.name, startingActivity.time, null)
			} else {
				this@ActivitySummary.createActivityView(startingActivityItem.drawableResource,
					startingActivityItem.name, startingActivity.time, endingActivity.time)
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


	private fun getActivityItem(): ActivityItem {

		val chairlift: MapItem? = ActivitySummaryLocations.checkIfIOnChairlift()
		val returnPair: ActivityItem = if (chairlift != null) {

			ActivityItem(chairlift.name, R.drawable.ic_chairlift, BitmapDescriptorFactory
				.defaultMarker(BitmapDescriptorFactory.HUE_RED))
		} else {

			val chairliftTerminal: MapItem? = ActivitySummaryLocations.checkIfAtChairliftTerminals()
			if (chairliftTerminal != null) {
				ActivityItem(chairliftTerminal.name, R.drawable.ic_chairlift, BitmapDescriptorFactory
					.defaultMarker(BitmapDescriptorFactory.HUE_RED))
			} else {

				val other: MapItem? = ActivitySummaryLocations.checkIfOnOther()
				if (other != null) {
					val icon: Int = if (other.getIcon() != null) {
						other.getIcon()!!
					} else {
						R.drawable.ic_missing
					}
					ActivityItem(other.name, icon, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
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
						ActivityItem(run.name, icon, markerIconColor)
					} else {
						ActivityItem(UNKNOWN_LOCATION, R.drawable.ic_missing, BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
					}
				}
			}
		}

		return returnPair
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
		activityView.startTime.text = TimeManager.getTimeFromLong(startTime)

		if (endTime != null) {
			activityView.endTime.text = TimeManager.getTimeFromLong(endTime)
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

private data class ActivityItem(val name: String, @DrawableRes val drawableResource: Int = R.drawable.ic_missing,
                                val markerIcon: BitmapDescriptor)

class FileSelectionDialog(private val activity: ActivitySummary) : AlertDialog(activity) {

	fun showDialog() {

		val binding: FileSelectionBinding = FileSelectionBinding.inflate(this.layoutInflater)

		val alertDialogBuilder = Builder(this.context)
		alertDialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
		alertDialogBuilder.setView(binding.root)

		val dialog: AlertDialog = alertDialogBuilder.create()

		val db = ActivityDatabase(this.activity)
		val dates: Array<String> = ActivityDatabase.getTables(db.readableDatabase)
		db.close()

		dates.forEach { date: String ->

			val textView = TextView(this.context)
			textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT)
			textView.text = date
			textView.textSize = 25.0F
			textView.setOnClickListener {

				val database = ActivityDatabase(this.activity)
				SkiingActivityManager.FinishedAndLoadedActivities = ActivityDatabase
					.readSkiingActivesFromDatabase(date, database.readableDatabase)
				database.close()

				if (SkiingActivityManager.FinishedAndLoadedActivities != null) {
					this.activity.loadActivities(SkiingActivityManager.FinishedAndLoadedActivities!!)
				}

				dialog.dismiss()
			}

			binding.files.addView(textView)
		}

		dialog.show()
	}
}