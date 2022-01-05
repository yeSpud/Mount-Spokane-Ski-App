package com.mtspokane.skiapp.debugview

import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.awaitMap
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.mapactivity.MapHandler
import java.util.Locale
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class DebugViewModel : ViewModel() {

	var locationMarker: Marker? = null

	var map: GoogleMap? = null

	suspend fun mapCoroutine(supportFragment: SupportMapFragment, activity: DebugActivity) {

		this.map = supportFragment.awaitMap()

		// Move the camera.
		val cameraPosition = CameraPosition.Builder()
			.target(LatLng(47.921774273268106, -117.10490226745605))
			.tilt(47.547382F)
			.bearing(319.2285F)
			.zoom(14.169826F)
			.build()
		this.map!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
		this.map!!.setLatLngBoundsForCameraTarget(LatLngBounds(LatLng(47.912728,
			-117.133402), LatLng(47.943674, -117.092470)))
		this.map!!.setMaxZoomPreference(20F)
		this.map!!.setMinZoomPreference(13F)

		/*
		this.map!!.setOnCameraIdleListener {
			val c: CameraPosition = this.map!!.cameraPosition

			Log.v("OnCameraIdle", "Bearing: ${c.bearing}")
			Log.v("OnCameraIdle", "Target: ${c.target}")
			Log.v("OnCameraIdle", "Tilt: ${c.tilt}")
			Log.v("OnCameraIdle", "Zoom: ${c.zoom}")
		} */

		// Set the map to use satellite view.
		this.map!!.mapType = GoogleMap.MAP_TYPE_SATELLITE

		viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {

			// Add the chairlifts to the map.
			// Load in the chairlift kml file, and iterate though each placemark.
			this@DebugViewModel.loadPolylinesAsync("Loading chairlift polylines",
				R.raw.lifts, activity, R.color.chairlift,4.0F, R.drawable.ic_chairlift).start()

			// Load in the easy runs kml file, and iterate though each placemark.
			this@DebugViewModel.loadPolylinesAsync("Loading easy polylines", R.raw.easy,
				activity, R.color.easy, 3.0F, R.drawable.ic_easy).start()

			// Load in the moderate runs kml file, and iterate though each placemark.
			this@DebugViewModel.loadPolylinesAsync("Loading moderate polylines",
				R.raw.moderate, activity, R.color.moderate, 2.0F, R.drawable.ic_moderate).start()

			// Load in the difficult runs kml file, and iterate though each placemark.
			this@DebugViewModel.loadPolylinesAsync("Loading difficult polylines",
				R.raw.difficult, activity, R.color.difficult, 1.0F, R.drawable.ic_difficult).start()

			// Other polygons
			// (lodges, parking lots, vista house, tubing area, yurt, ski patrol building, and ski area bounds...)
			this@DebugViewModel.loadPolygonsAsync("Loading other polygons", R.raw.other,
				activity, R.color.other_polygon_fill, false).start()

			// Load the chairlift terminal polygons file.
			this@DebugViewModel.loadPolygonsAsync("Loading chairlift terminal polygons",
				R.raw.lift_terminal_polygons, activity, R.color.chairlift_polygon, true).start()

			// Load the chairlift polygons file.
			this@DebugViewModel.loadPolygonsAsync("Loading chairlift polygons",
				R.raw.lift_polygons, activity, R.color.chairlift_polygon, true).start()

			// Load the easy polygons file.
			this@DebugViewModel.loadPolygonsAsync("Loading easy polygons",
				R.raw.easy_polygons, activity, R.color.easy_polygon, true).start()

			// Load the moderate polygons file.
			this@DebugViewModel.loadPolygonsAsync("Loading moderate polygons",
				R.raw.moderate_polygons, activity, R.color.moderate_polygon, true).start()

			// Load the difficult polygons file.
			this@DebugViewModel.loadPolygonsAsync("Loading difficult polygons",
				R.raw.difficult_polygons, activity, R.color.difficult_polygon, true).start()
		}.start()
	}

	private fun loadPolylinesAsync(jobDescription: String, @RawRes polylineResource: Int, activity: DebugActivity,
	                               @ColorRes color: Int, zIndex: Float, @DrawableRes icon: Int): Deferred<Int> {
		return viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
			val tag = "loadPolylinesAsync"
			Log.v(tag, "Starting ${jobDescription.lowercase(Locale.getDefault())}")

			MapHandler.loadPolylines(this@DebugViewModel.map!!, polylineResource, activity, color,
				zIndex, icon)
			Log.v(tag, "Finished ${jobDescription.lowercase(Locale.getDefault())}")
		}
	}

	private fun loadPolygonsAsync(jobDescription: String, @RawRes polygonResource: Int, activity: DebugActivity,
	                              @ColorRes color: Int, visible: Boolean): Deferred<Int> {
		return viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
			val tag = "loadPolygonsAsync"
			Log.v(tag, "Starting ${jobDescription.lowercase(Locale.getDefault())}")
			MapHandler.loadPolygons(this@DebugViewModel.map!!, polygonResource, activity, color, visible)
			Log.v(tag, "Finished ${jobDescription.lowercase(Locale.getDefault())}")
		}
	}
}