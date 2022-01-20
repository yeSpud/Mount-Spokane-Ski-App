package com.mtspokane.skiapp.maphandlers

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.DebugActivity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class DebugMap(activity: DebugActivity) : MapHandler(activity, CameraPosition.Builder()
	.target(LatLng(47.921774273268106, -117.10490226745605)).tilt(47.547382F)
	.bearing(319.2285F).zoom(14.169826F).build()) {

	var locationMarker: Marker? = null

	override fun destroy() {

		if (this.locationMarker != null) {
			Log.v("DebugMap", "Removing location marker")
			this.locationMarker!!.remove()
			this.locationMarker = null
		}

		super.destroy()
	}

	init {

		this.setAdditionalCallback {
			this.activity.lifecycleScope.async(Dispatchers.IO, CoroutineStart.LAZY) {

				// Add the chairlifts to the map.
				// Load in the chairlift kml file, and iterate though each placemark.
				this@DebugMap.loadPolylinesHeadlessAsync("Loading chairlift polylines",
					R.raw.lifts, R.color.chairlift,4.0F, R.drawable.ic_chairlift).start()

				// Load in the easy runs kml file, and iterate though each placemark.
				this@DebugMap.loadPolylinesHeadlessAsync("Loading easy polylines", R.raw.easy,
					R.color.easy, 3.0F, R.drawable.ic_easy).start()

				// Load in the moderate runs kml file, and iterate though each placemark.
				this@DebugMap.loadPolylinesHeadlessAsync("Loading moderate polylines",
					R.raw.moderate, R.color.moderate, 2.0F, R.drawable.ic_moderate).start()

				// Load in the difficult runs kml file, and iterate though each placemark.
				this@DebugMap.loadPolylinesHeadlessAsync("Loading difficult polylines",
					R.raw.difficult, R.color.difficult, 1.0F, R.drawable.ic_difficult).start()

				// Other polygons
				// (lodges, parking lots, vista house, tubing area, yurt, ski patrol building, and ski area bounds...)
				this@DebugMap.loadPolygonsHeadlessAsync("Loading other polygons", R.raw.other,
					R.color.other_polygon_fill, false).start()

				// Load the chairlift terminal polygons file.
				this@DebugMap.loadPolygonsHeadlessAsync("Loading chairlift terminal polygons",
					R.raw.lift_terminal_polygons, R.color.chairlift_polygon, true).start()

				// Load the chairlift polygons file.
				this@DebugMap.loadPolygonsHeadlessAsync("Loading chairlift polygons",
					R.raw.lift_polygons, R.color.chairlift_polygon, true).start()

				// Load the easy polygons file.
				this@DebugMap.loadPolygonsHeadlessAsync("Loading easy polygons",
					R.raw.easy_polygons, R.color.easy_polygon, true).start()

				// Load the moderate polygons file.
				this@DebugMap.loadPolygonsHeadlessAsync("Loading moderate polygons",
					R.raw.moderate_polygons, R.color.moderate_polygon, true).start()

				// Load the difficult polygons file.
				this@DebugMap.loadPolygonsHeadlessAsync("Loading difficult polygons",
					R.raw.difficult_polygons, R.color.difficult_polygon, true).start()
			}.start()
		}
	}
}