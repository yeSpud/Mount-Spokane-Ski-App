package com.mtspokane.skiapp.activities.activitysummary

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isNotEmpty
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.maphandlers.ActivitySummaryMap
import com.mtspokane.skiapp.skierlocation.SkierLocationService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

class ActivitySummary : FragmentActivity() {

	private var mapHandler: ActivitySummaryMap? = null

	private lateinit var container: LinearLayout

	private lateinit var fileSelectionDialog: FileSelectionDialog

	private lateinit var creditDialog: AlertDialog

	var loadedFile: String = "${SkiingActivity.getDate()}.json"

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

		if (this.intent.extras != null) {

			val filename: String? = this.intent.extras!!.getString("file")

			if (filename != null) {
				val notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE)
						as NotificationManager
				notificationManager.cancel(SkierLocationService.ACTIVITY_SUMMARY_ID)

				val activities: Array<SkiingActivity> = SkiingActivity
					.readSkiingActivitiesFromFile(this, filename)
				this.loadActivities(activities)
				this.loadedFile = filename
				return
			} else {
				this.loadActivities(SkiingActivity.Activities.toTypedArray())
			}
		} else {

			// If all else fails just load from the current activities array.
			this.loadActivities(SkiingActivity.Activities.toTypedArray())
		}
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
			R.id.export_json -> SkiingActivity.createNewFileSAF(this, this.loadedFile,
				JSON_MIME_TYPE, WRITE_JSON_CODE)
			R.id.export_geojson -> SkiingActivity.createNewFileSAF(
				this, this.loadedFile.replace("json", "geojson"),
				GEOJSON_MIME_TYPE, WRITE_GEOJSON_CODE)
			R.id.share_json -> {
				val file = File(this.filesDir, this.loadedFile)
				SkiingActivity.shareFile(this, file, JSON_MIME_TYPE)
			}
			R.id.share_geojson -> {

				val json: JSONObject = SkiingActivity.readJsonFromFile(this, this.loadedFile)
				val geojson: JSONObject = SkiingActivity.convertJsonToGeoJson(json)

				val tmpFileName = this.loadedFile.replace("json", "geojson")
				this.openFileOutput(tmpFileName, Context.MODE_PRIVATE).use {
					it.write(geojson.toString(4).toByteArray())
				}

				val tmpFile = File(this.filesDir, tmpFileName)

				SkiingActivity.shareFile(this, tmpFile, JSON_MIME_TYPE)

				tmpFile.delete()
			}
			R.id.credits -> this.creditDialog.show()
		}

		return super.onOptionsItemSelected(item)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		if (resultCode == RESULT_OK) {

			if (data == null) {
				return
			}

			val fileUri: Uri = data.data ?: return
			val json: JSONObject = SkiingActivity.readJsonFromFile(this, this.loadedFile)

			when (requestCode) {
				WRITE_JSON_CODE -> SkiingActivity.writeToExportFile(this.contentResolver, fileUri,
					json.toString(4))
				WRITE_GEOJSON_CODE -> {
					val geoJson: JSONObject = SkiingActivity.convertJsonToGeoJson(json)
					SkiingActivity.writeToExportFile(this.contentResolver, fileUri, geoJson.toString(4))
				}
				else -> Log.w("onActivityResult", "Unaccounted for code: $resultCode")
			}
		}
	}

	fun loadActivities(activities: Array<SkiingActivity>) {

		this.container.removeAllViews()

		if (this.mapHandler != null) {

			if (this.mapHandler!!.polyline != null) {
				this.mapHandler!!.polyline!!.remove()
				this.mapHandler!!.polyline = null
			}

			this.mapHandler!!.locationMarkers.forEach {
				it.remove()
			}
			this.mapHandler!!.locationMarkers = emptyArray()
		}

		if (activities.isEmpty()) {
			return
		}

		var endingActivity: SkiingActivity? = null

		lateinit var endingTitleIconColor: Pair<String, Pair<Int, BitmapDescriptor>>

		lateinit var currentTitleIconColor: Pair<String, Pair<Int, BitmapDescriptor>>

		for (index in 0 until (activities.size + 1)) {

			if (index == activities.size) {
				val finalActivityView = if (endingActivity != null) {
					this.createActivityView(currentTitleIconColor.second.first, currentTitleIconColor.first,
						endingActivity.time, activities[activities.size - 1].time)
				} else {
					this.createActivityView(currentTitleIconColor.second.first, currentTitleIconColor.first,
							activities[activities.size - 1].time, null)
				}
				this.container.addView(finalActivityView)
				break
			}

			ActivitySummaryLocations.updateLocations(activities[index])

			if (index > 0) {
				endingTitleIconColor = currentTitleIconColor
			}

			currentTitleIconColor = this.getTitleIconAndColor()

			if (this.mapHandler != null) {

				this.mapHandler!!.addMarker(activities[index].latitude, activities[index].longitude,
					currentTitleIconColor.second.second)
			}

			if (endingActivity == null) {
				endingActivity = activities[index]
			} else {

				if (endingTitleIconColor.first != currentTitleIconColor.first) {
					this.container.addView(this.createActivityView(endingTitleIconColor.second.first,
						endingTitleIconColor.first, endingActivity.time, activities[index].time))
					endingActivity = activities[index]
				}
			}
		}

		if (this.mapHandler != null) {
			this.mapHandler!!.addPolylineFromMarker()
		}
	}

	private fun getTitleIconAndColor(): Pair<String, Pair<Int, BitmapDescriptor>> {

		val returnPair: Pair<String, Pair<Int, BitmapDescriptor>> = if (ActivitySummaryLocations.altitudeConfidence >= 2u &&
			ActivitySummaryLocations.speedConfidence >= 1u &&
			ActivitySummaryLocations.mostLikelyChairlift != null) {

			Pair(ActivitySummaryLocations.mostLikelyChairlift!!.name, Pair(R.drawable.ic_chairlift,
				BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
		} else {

			val chairliftTerminal: MapItem? = ActivitySummaryLocations.checkIfAtChairliftTerminals()
			if (chairliftTerminal != null) {
				Pair(chairliftTerminal.name, Pair(R.drawable.ic_chairlift, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
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
						Pair("Unknown Location", Pair(R.drawable.ic_missing, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)))
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

		private const val JSON_MIME_TYPE = "application/json"

		private const val GEOJSON_MIME_TYPE = "application/geojson"

		private const val WRITE_JSON_CODE = 509

		private const val WRITE_GEOJSON_CODE = 666

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