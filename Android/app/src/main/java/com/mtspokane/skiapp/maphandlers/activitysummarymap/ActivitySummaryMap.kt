package com.mtspokane.skiapp.maphandlers.activitysummarymap

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.ktx.addPolyline
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummary
import com.mtspokane.skiapp.databases.ActivityDatabase
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.activities.mainactivity.SkierLocationService
import com.mtspokane.skiapp.databases.SkiingActivityManager
import com.mtspokane.skiapp.maphandlers.MapHandler
import com.orhanobut.dialogplus.OnItemClickListener
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

@SuppressLint("PotentialBehaviorOverride")
class ActivitySummaryMap(activity: ActivitySummary) : MapHandler(activity, CameraPosition.Builder()
	.target(LatLng(47.923275586525094, -117.10265189409256)).tilt(45.0F)
	.bearing(317.50552F).zoom(14.279241F).build()) {

	var locationMarkers: MutableList<ActivitySummaryLocationMarkers> = mutableListOf()

	var polyline: Polyline? = null

	override val additionalCallback: OnMapReadyCallback = OnMapReadyCallback {
		this.activity.lifecycleScope.async(Dispatchers.IO, CoroutineStart.LAZY) {

			if (MtSpokaneMapItems.skiAreaBounds == null || MtSpokaneMapItems.other == null) {
				MtSpokaneMapItems.initializeOtherPolygonsAsync(this@ActivitySummaryMap.activity::class,
						this@ActivitySummaryMap).await()
			}

			if (MtSpokaneMapItems.chairliftTerminals == null) {
				MtSpokaneMapItems.addChairliftTerminalPolygonsAsync(this@ActivitySummaryMap.activity::class,
						this@ActivitySummaryMap).await()
			}

			if (MtSpokaneMapItems.chairlifts == null) {
				MtSpokaneMapItems.addChairliftPolygonsAsync(this@ActivitySummaryMap.activity::class,
						this@ActivitySummaryMap).await()
			}

			if (MtSpokaneMapItems.easyRuns == null) {
				MtSpokaneMapItems.addEasyPolygonsAsync(this@ActivitySummaryMap.activity::class,
						this@ActivitySummaryMap).await()
			}

			if (MtSpokaneMapItems.moderateRuns == null) {
				MtSpokaneMapItems.addModeratePolygonsAsync(this@ActivitySummaryMap.activity::class,
						this@ActivitySummaryMap).await()
			}

			if (MtSpokaneMapItems.difficultRuns == null) {
				MtSpokaneMapItems.addDifficultPolygonsAsync(this@ActivitySummaryMap.activity::class,
						this@ActivitySummaryMap).await()
			}

			val loads = listOf(

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
							.loadActivities(SkiingActivityManager.InProgressActivities.toTypedArray())
				}

				with(this@ActivitySummaryMap.map!!) {

					this.setInfoWindowAdapter(CustomInfoWindow(this@ActivitySummaryMap.activity))

					this.setOnCircleClickListener {

						if (it.tag is ActivitySummaryLocationMarkers) {

							val activitySummaryLocationMarker: ActivitySummaryLocationMarkers = it.tag as ActivitySummaryLocationMarkers

							if (activitySummaryLocationMarker.marker != null) {
								activitySummaryLocationMarker.marker!!.isVisible = true
								activitySummaryLocationMarker.marker!!.showInfoWindow()
							}
						}
					}

					this.setOnInfoWindowCloseListener { it.isVisible = false }

				}
			}

		}.start()
	}

	/* override val mapOptionItemClickListener: OnItemClickListener = OnItemClickListener { dialog, item, view, position -> {
		// TODO
	} }*/

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
}