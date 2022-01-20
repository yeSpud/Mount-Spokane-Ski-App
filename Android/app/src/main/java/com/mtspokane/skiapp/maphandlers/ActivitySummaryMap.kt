package com.mtspokane.skiapp.maphandlers

import android.graphics.Color
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.UiThread
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
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
import com.mtspokane.skiapp.skiingactivity.SkiingActivityManager
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class ActivitySummaryMap(activity: ActivitySummary) : MapHandler(activity, CameraPosition.Builder()
	.target(LatLng(47.923275586525094, -117.10265189409256)).tilt(45.0F)
	.bearing(317.50552F).zoom(14.279241F).build()) {

	var locationMarkers: Array<ActivitySummaryLocationMarkers> = emptyArray()

	var polyline: Polyline? = null

	override fun destroy() {

		if (this.locationMarkers.isNotEmpty()) {

			Log.v("ActivitySummaryMap", "Removing location markers")
			this.locationMarkers.forEach {
				it.destroy()
			}
			this.locationMarkers = emptyArray()
		}

		if (this.polyline != null) {
			this.polyline!!.remove()
			this.polyline = null
		}

		super.destroy()
	}

	fun addActivitySummaryLocationMarker(title: String, latitude: Double, longitude: Double,
	                                     @DrawableRes drawableResource: Int, icon: BitmapDescriptor) {

		if (this.map != null) {

			this.locationMarkers = Array(this.locationMarkers.size + 1) {

				if (it == this.locationMarkers.size) {
					ActivitySummaryLocationMarkers(this.map!!, title, latitude, longitude,
						drawableResource, icon)
				} else {
					this.locationMarkers[it]
				}
			}
		}
	}

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

	init {

		this.setAdditionalCallback {
			this.activity.lifecycleScope.async(Dispatchers.IO, CoroutineStart.LAZY) {

				val loads = listOf(

					// Add the chairlifts to the map.
					// Load in the chairlift kml file, and iterate though each placemark.
					this@ActivitySummaryMap.loadPolylinesAsync("Loading chairlift polylines",
						R.raw.lifts, R.color.chairlift, 4.0F, R.drawable.ic_chairlift),

					// Load in the easy runs kml file, and iterate though each placemark.
					this@ActivitySummaryMap.loadPolylinesAsync("Loading easy polylines",
						R.raw.easy, R.color.easy, 3.0F, R.drawable.ic_easy),

					// Load in the moderate runs kml file, and iterate though each placemark.
					this@ActivitySummaryMap.loadPolylinesAsync("Loading moderate polylines",
						R.raw.moderate, R.color.moderate, 2.0F, R.drawable.ic_moderate),

					// Load in the difficult runs kml file, and iterate though each placemark.
					this@ActivitySummaryMap.loadPolylinesAsync("Loading difficult polylines",
						R.raw.difficult, R.color.difficult, 1.0F, R.drawable.ic_difficult),

					// Other polygons
					// (lodges, parking lots, vista house, tubing area, yurt, ski patrol building, and ski area bounds...)
					this@ActivitySummaryMap.loadPolygonsAsync("Loading other polygons",
						R.raw.other, R.color.other_polygon_fill, false),

					// Load the chairlift terminal polygons file.
					this@ActivitySummaryMap.loadPolygonsAsync("Loading chairlift terminal polygons",
						R.raw.lift_terminal_polygons, R.color.chairlift_polygon),

					// Load the chairlift polygons file.
					this@ActivitySummaryMap.loadPolygonsAsync("Loading chairlift polygons",
						R.raw.lift_polygons, R.color.chairlift_polygon),

					// Load the easy polygons file.
					this@ActivitySummaryMap.loadPolygonsAsync("Loading easy polygons",
						R.raw.easy_polygons, R.color.easy_polygon),

					// Load the moderate polygons file.
					this@ActivitySummaryMap.loadPolygonsAsync("Loading moderate polygons",
						R.raw.moderate_polygons, R.color.moderate_polygon),

					// Load the difficult polygons file.
					this@ActivitySummaryMap.loadPolygonsAsync("Loading difficult polygons",
						R.raw.difficult_polygons, R.color.difficult_polygon)
				)

				loads.awaitAll()
				withContext(Dispatchers.Main) {

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
class ActivitySummaryLocationMarkers(map: GoogleMap, title: String, latitude: Double, longitude: Double,
                                     @DrawableRes drawableResource: Int, icon: BitmapDescriptor) {

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

		val location = LatLng(latitude, longitude)

		val color: Int = when (drawableResource) {
			R.drawable.ic_easy -> Color.GREEN
			R.drawable.ic_moderate -> Color.BLUE
			R.drawable.ic_difficult -> Color.BLACK
			R.drawable.ic_chairlift -> Color.RED
			else -> Color.MAGENTA
		}

		this.circle = map.addCircle {
			center(location)
			strokeColor(color)
			fillColor(color)
			clickable(true)
			radius(8.0)
			zIndex(50.0F)
			visible(true)
		}

		this.marker = map.addMarker {
			position(location)
			icon(icon)
			title(title)
			zIndex(99.0F)
			visible(false)
		}

		this.circle!!.tag = this
	}
}