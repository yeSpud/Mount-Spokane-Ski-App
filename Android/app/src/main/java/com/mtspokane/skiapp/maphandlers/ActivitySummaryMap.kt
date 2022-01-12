package com.mtspokane.skiapp.maphandlers

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addMarker
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummary
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class ActivitySummaryMap(activity: ActivitySummary) : MapHandler(activity, CameraPosition.Builder()
	.target(LatLng(47.92517834073426, -117.10480503737926)).tilt(45F)
	.bearing(317.50552F).zoom(14.414046F).build()) {

	var locationMarkers: Array<Marker> = emptyArray()

	override fun destroy() {

		if (this.locationMarkers.isNotEmpty()) {
			Log.v("ActivitySummaryMap", "Removing location markers")
			this.locationMarkers.forEach {
				it.remove()
			}
			this.locationMarkers = emptyArray()
		}

		super.destroy()
	}

	fun addMarker(latitude: Double, longitude: Double) {

		if (this.map != null) {

			this.locationMarkers = Array(this.locationMarkers.size + 1) {

				if (it == this.locationMarkers.size) {
					this.map!!.addMarker {
						position(LatLng(latitude, longitude))
					}!!
				} else {
					this.locationMarkers[it]
				}
			}
		}
	}

	init {

		this.setAdditionalCallback{
			this.activity.lifecycleScope.async(Dispatchers.IO, CoroutineStart.LAZY) {

				// Add the chairlifts to the map.
				// Load in the chairlift kml file, and iterate though each placemark.
				this@ActivitySummaryMap.loadPolylinesAsync("Loading chairlift polylines",
					R.raw.lifts, R.color.chairlift,4.0F, R.drawable.ic_chairlift).start()

				// Load in the easy runs kml file, and iterate though each placemark.
				this@ActivitySummaryMap.loadPolylinesAsync("Loading easy polylines", R.raw.easy,
					R.color.easy, 3.0F, R.drawable.ic_easy).start()

				// Load in the moderate runs kml file, and iterate though each placemark.
				this@ActivitySummaryMap.loadPolylinesAsync("Loading moderate polylines",
					R.raw.moderate, R.color.moderate, 2.0F, R.drawable.ic_moderate).start()

				// Load in the difficult runs kml file, and iterate though each placemark.
				this@ActivitySummaryMap.loadPolylinesAsync("Loading difficult polylines",
					R.raw.difficult, R.color.difficult, 1.0F, R.drawable.ic_difficult).start()

				// Other polygons
				// (lodges, parking lots, vista house, tubing area, yurt, ski patrol building, and ski area bounds...)
				this@ActivitySummaryMap.loadPolygonsAsync("Loading other polygons", R.raw.other,
					R.color.other_polygon_fill, false).start()

				// Load the chairlift terminal polygons file.
				this@ActivitySummaryMap.loadPolygonsAsync("Loading chairlift terminal polygons",
					R.raw.lift_terminal_polygons, R.color.chairlift_polygon).start()

				// Load the chairlift polygons file.
				this@ActivitySummaryMap.loadPolygonsAsync("Loading chairlift polygons",
					R.raw.lift_polygons, R.color.chairlift_polygon).start()

				// Load the easy polygons file.
				this@ActivitySummaryMap.loadPolygonsAsync("Loading easy polygons",
					R.raw.easy_polygons, R.color.easy_polygon).start()

				// Load the moderate polygons file.
				this@ActivitySummaryMap.loadPolygonsAsync("Loading moderate polygons",
					R.raw.moderate_polygons, R.color.moderate_polygon).start()

				// Load the difficult polygons file.
				this@ActivitySummaryMap.loadPolygonsAsync("Loading difficult polygons",
					R.raw.difficult_polygons, R.color.difficult_polygon).start()
			}.start()
		}
	}
}