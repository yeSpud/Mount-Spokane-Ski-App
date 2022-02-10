package com.mtspokane.skiapp.maphandlers

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.ktx.addCircle
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.addPolyline
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummary
import com.mtspokane.skiapp.databases.ActivityDatabase
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.activities.mainactivity.SkierLocationService
import com.mtspokane.skiapp.databases.SkiingActivityManager
import com.mtspokane.skiapp.mapItem.MapMarker
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class ActivitySummaryMap(activity: ActivitySummary) : MapHandler(activity, CameraPosition.Builder()
	.target(LatLng(47.923275586525094, -117.10265189409256)).tilt(45.0F)
	.bearing(317.50552F).zoom(14.279241F).build()) {

	var locationMarkers: MutableList<ActivitySummaryLocationMarkers> = mutableListOf()

	var polyline: Polyline? = null

	override fun destroy() {

		if (this.locationMarkers.isNotEmpty()) {

			Log.v("ActivitySummaryMap", "Removing location markers")
			this.locationMarkers.forEach {
				it.destroy()
			}
			this.locationMarkers.clear()
		}

		if (this.polyline != null) {
			this.polyline!!.remove()
			this.polyline = null
		}

		super.destroy()
	}

	@MainThread
	fun addPolylineFromMarker() {

		if (this.map == null) {
			return
		}

		this.polyline = this.map!!.addPolyline {

			this@ActivitySummaryMap.locationMarkers.forEach {
				if (it.circle != null) {
					add(it.circle!!.center)
				}
			}

			color(this@ActivitySummaryMap.getARGB(R.color.yellow))
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

	internal inner class CustomInfoWindow : GoogleMap.InfoWindowAdapter {

		private val markerView: View = this@ActivitySummaryMap.activity.layoutInflater.inflate(R.layout.info_window,
			null)

		override fun getInfoContents(marker: Marker): View? {

			if (marker.tag is Pair<*, *>) {

				val markerInfo: Pair<MapMarker, String?> = marker.tag as Pair<MapMarker, String?>

				val name: TextView = this.markerView.findViewById(R.id.marker_name)
				name.text = markerInfo.first.name

				val altitude: TextView = markerView.findViewById(R.id.marker_altitude)
				altitude.text = this@ActivitySummaryMap.activity.getString(R.string.marker_altitude, markerInfo.first
					.skiingActivity.altitude.roundToInt()) // TODO Convert to feet & catch round NaN

				val speed: TextView = this.markerView.findViewById(R.id.marker_speed)

				// Convert from meters per second to miles per hour.
				val speedConversion = 0.44704f

				speed.text = this@ActivitySummaryMap.activity.getString(R.string.marker_speed, (markerInfo.first
					.skiingActivity.speed / speedConversion).roundToInt()) // TODO Catch round NaN

				if (markerInfo.second != null) {

					val debug: TextView = this.markerView.findViewById(R.id.marker_debug)
					debug.text = markerInfo.second
				}

				return this.markerView

			} else {

				return null
			}
		}

		override fun getInfoWindow(marker: Marker): View? {

			return null
		}
	}

	init {

		this.setAdditionalCallback {

			it.setInfoWindowAdapter(CustomInfoWindow())

			this.activity.lifecycleScope.async(Dispatchers.IO, CoroutineStart.LAZY) {

				if (MtSpokaneMapItems.skiAreaBounds == null || MtSpokaneMapItems.other == null) {
					MtSpokaneMapItems.initializeOtherAsync(this@ActivitySummaryMap.activity::class,
						this@ActivitySummaryMap).await()
				}

				if (MtSpokaneMapItems.chairliftTerminals == null) {
					MtSpokaneMapItems.initializeChairliftTerminalsAsync(this@ActivitySummaryMap.activity::class,
					this@ActivitySummaryMap).await()
				}

				if (MtSpokaneMapItems.chairlifts == null) {
					MtSpokaneMapItems.initializeChairliftsAsync(this@ActivitySummaryMap.activity::class,
					this@ActivitySummaryMap).await()
					MtSpokaneMapItems.addChairliftPolygonsAsync(this@ActivitySummaryMap.activity::class,
						this@ActivitySummaryMap).await()
				}

				if (MtSpokaneMapItems.easyRuns == null) {
					MtSpokaneMapItems.initializeEasyRunsAsync(this@ActivitySummaryMap.activity::class,
						this@ActivitySummaryMap).await()
					MtSpokaneMapItems.addEasyPolygonsAsync(this@ActivitySummaryMap.activity::class,
						this@ActivitySummaryMap).await()
				}

				if (MtSpokaneMapItems.moderateRuns == null) {
					MtSpokaneMapItems.initializeModerateRunsAsync(this@ActivitySummaryMap.activity::class,
						this@ActivitySummaryMap).await()
					MtSpokaneMapItems.addModeratePolygonsAsync(this@ActivitySummaryMap.activity::class,
						this@ActivitySummaryMap).await()
				}

				if (MtSpokaneMapItems.difficultRuns == null) {
					MtSpokaneMapItems.initializeDifficultRunsAsync(this@ActivitySummaryMap.activity::class,
						this@ActivitySummaryMap).await()
					MtSpokaneMapItems.addDifficultPolygonsAsync(this@ActivitySummaryMap.activity::class,
						this@ActivitySummaryMap).await()
				}

				val loads = listOf(

					// Add the chairlifts to the map.
					// Load in the chairlift kml file, and iterate though each placemark.
					this@ActivitySummaryMap.loadPolylinesHeadlessAsync("Loading chairlift polylines",
						R.raw.lifts, R.color.chairlift, 4.0F, R.drawable.ic_chairlift),

					// Load in the easy runs kml file, and iterate though each placemark.
					this@ActivitySummaryMap.loadPolylinesHeadlessAsync("Loading easy polylines",
						R.raw.easy, R.color.easy, 3.0F, R.drawable.ic_easy),

					// Load in the moderate runs kml file, and iterate though each placemark.
					this@ActivitySummaryMap.loadPolylinesHeadlessAsync("Loading moderate polylines",
						R.raw.moderate, R.color.moderate, 2.0F, R.drawable.ic_moderate),

					// Load in the difficult runs kml file, and iterate though each placemark.
					this@ActivitySummaryMap.loadPolylinesHeadlessAsync("Loading difficult polylines",
						R.raw.difficult, R.color.difficult, 1.0F, R.drawable.ic_difficult),

					// Other polygons
					// (lodges, parking lots, vista house, tubing area, yurt, ski patrol building, and ski area bounds...)
					this@ActivitySummaryMap.loadPolygonsHeadlessAsync("Loading other polygons",
						R.raw.other, R.color.other_polygon_fill, false),

					// Load the chairlift terminal polygons file.
					this@ActivitySummaryMap.loadPolygonsHeadlessAsync("Loading chairlift terminal polygons",
						R.raw.lift_terminal_polygons, R.color.chairlift_polygon),

					// Load the chairlift polygons file.
					this@ActivitySummaryMap.loadPolygonsHeadlessAsync("Loading chairlift polygons",
						R.raw.lift_polygons, R.color.chairlift_polygon),

					// Load the easy polygons file.
					this@ActivitySummaryMap.loadPolygonsHeadlessAsync("Loading easy polygons",
						R.raw.easy_polygons, R.color.easy_polygon),

					// Load the moderate polygons file.
					this@ActivitySummaryMap.loadPolygonsHeadlessAsync("Loading moderate polygons",
						R.raw.moderate_polygons, R.color.moderate_polygon),

					// Load the difficult polygons file.
					this@ActivitySummaryMap.loadPolygonsHeadlessAsync("Loading difficult polygons",
						R.raw.difficult_polygons, R.color.difficult_polygon)
				)

				loads.awaitAll()
				withContext(Dispatchers.Main) {

					if (this@ActivitySummaryMap.activity.intent.hasExtra(SkierLocationService.ACTIVITY_SUMMARY_LAUNCH_DATE)) {
						this@ActivitySummaryMap.loadFromIntent(this@ActivitySummaryMap.activity.intent
							.getStringExtra(SkierLocationService.ACTIVITY_SUMMARY_LAUNCH_DATE))
					}

					if (SkiingActivityManager.FinishedAndLoadedActivities != null) {
						(this@ActivitySummaryMap.activity as ActivitySummary)
							.loadActivities(SkiingActivityManager.FinishedAndLoadedActivities!!)
					} else {
						(this@ActivitySummaryMap.activity as ActivitySummary)
							.loadActivities(SkiingActivityManager.InProgressActivities)
					}

					this@ActivitySummaryMap.map!!.setOnCircleClickListener {

						if (it.tag is ActivitySummaryLocationMarkers) {

							val activitySummaryLocationMarker: ActivitySummaryLocationMarkers = it.tag as ActivitySummaryLocationMarkers

							if (activitySummaryLocationMarker.marker != null) {
								activitySummaryLocationMarker.marker!!.isVisible = true
								activitySummaryLocationMarker.marker!!.showInfoWindow()
							}
						}
					}

					this@ActivitySummaryMap.map!!.setOnInfoWindowCloseListener { it.isVisible = false }
				}

			}.start()
		}
	}
}

@UiThread
class ActivitySummaryLocationMarkers(map: GoogleMap, mapMarker: MapMarker, debugSnippetText: String?) {

	var marker: Marker? = null
	private set

	var circle: Circle? = null
	private set

	fun destroy() {

		if (this.marker != null) {
			this.marker!!.remove()
			this.marker = null
		}

		if (this.circle != null) {
			this.circle!!.remove()
			this.circle = null
		}
	}

	init {

		val location = LatLng(mapMarker.skiingActivity.latitude, mapMarker.skiingActivity.longitude)

		this.circle = map.addCircle {
			center(location)
			strokeColor(mapMarker.circleColor)
			fillColor(mapMarker.circleColor)
			clickable(true)
			radius(3.0)
			zIndex(50.0F)
			visible(true)
		}

		this.marker = map.addMarker {
			position(location)
			icon(mapMarker.markerColor)
			title(mapMarker.name)
			zIndex(99.0F)
			visible(false)
		}

		this.marker!!.tag = Pair(mapMarker, debugSnippetText)

		/*
		if (debugSnippetText != null) {
			this.marker!!.snippet = debugSnippetText
		}*/

		this.circle!!.tag = this
	}
}