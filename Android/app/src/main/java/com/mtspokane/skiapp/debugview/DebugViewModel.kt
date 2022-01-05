package com.mtspokane.skiapp.debugview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon
import com.google.maps.android.ktx.awaitMap
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.mapItem.UIMapItem
import com.mtspokane.skiapp.mapactivity.MapHandler
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class DebugViewModel : ViewModel() {

	var locationMarker: Marker? = null

	var map: GoogleMap? = null

	suspend fun mapCoroutine(supportFragment: SupportMapFragment, activity: DebugActivity) {

		val tag = "mapCoroutine"

		this.map = supportFragment.awaitMap()

		// Move the camera.
		val cameraPosition = CameraPosition.Builder()
			.target(LatLng(47.92517834073426, -117.10480503737926))
			.tilt(45F)
			.bearing(317.50552F)
			.zoom(14.414046F)
			.build()
		this.map!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
		this.map!!.setLatLngBoundsForCameraTarget(LatLngBounds(LatLng(47.912728,
			-117.133402), LatLng(47.943674, -117.092470)))
		this.map!!.setMaxZoomPreference(20F)
		this.map!!.setMinZoomPreference(13F)

		// Set the map to use satellite view.
		this.map!!.mapType = GoogleMap.MAP_TYPE_SATELLITE

		viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
			// Add the chairlifts to the map.
			// Load in the chairlift kml file, and iterate though each placemark.
			viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
				Log.v(tag, "Started loading chairlift polylines")
				MapHandler.loadPolylines(this@DebugViewModel.map!!, R.raw.lifts, activity,
					R.color.chairlift, 4f, R.drawable.ic_chairlift)
				Log.v(tag, "Finished loading chairlift polylines")
			}.start()

			// Load in the easy runs kml file, and iterate though each placemark.
			viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
				Log.v(tag, "Started loading easy polylines")
				MapHandler.loadPolylines(this@DebugViewModel.map!!, R.raw.easy, activity, R.color.easy,
					3f, R.drawable.ic_easy)
				Log.v(tag, "Finished loading easy run polylines")
			}.start()

			// Load in the moderate runs kml file, and iterate though each placemark.
			viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
				Log.v(tag, "Started loading moderate polylines")
				MapHandler.loadPolylines(this@DebugViewModel.map!!, R.raw.moderate, activity,
					R.color.moderate, 2f, R.drawable.ic_moderate)
				Log.v(tag, "Finished loading moderate run polylines")
			}.start()

			// Load in the difficult runs kml file, and iterate though each placemark.
			viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
				Log.v(tag, "Started loading difficult polylines")
				MapHandler.loadPolylines(this@DebugViewModel.map!!, R.raw.difficult, activity,
					R.color.difficult, 1f, R.drawable.ic_difficult)
				Log.v(tag, "Finished loading difficult polylines")
			}.start()

			// Other polygons
			// (lodges, parking lots, vista house, tubing area, yurt, ski patrol building, and ski area bounds...)
			viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
				Log.v(tag, "Started loading other polygons")

				val hashmap: HashMap<String, Array<Polygon>> = MapHandler.loadPolygons(this@DebugViewModel.map!!,
					R.raw.other, activity, R.color.other_polygon_fill)

				val skiAreaBoundsKeyName = "Ski Area Bounds"
				val skiAreaBounds = hashmap[skiAreaBoundsKeyName]
				MtSpokaneMapItems.skiAreaBounds = UIMapItem(skiAreaBoundsKeyName, skiAreaBounds?.get(0))
				hashmap.remove(skiAreaBoundsKeyName)

				val names: Array<String> = hashmap.keys.toTypedArray()
				MtSpokaneMapItems.other = Array(9) {

					val icon: Int? = when (names[it]) {
						"Lodge 1" -> R.drawable.ic_missing // TODO Lodge icon
						"Lodge 2" -> R.drawable.ic_missing // TODO Lodge icon
						"Yurt" -> R.drawable.ic_yurt
						"Vista House" -> R.drawable.ic_missing // TODO Vista house icon
						"Ski Patrol" -> R.drawable.ic_ski_patrol_icon
						"Lodge 1 Parking Lot" -> R.drawable.ic_parking
						"Lodge 2 Parking Lot" -> R.drawable.ic_parking
						"Tubing Area" -> R.drawable.ic_missing // TODO Tubing area icon
						"Ski School" -> R.drawable.ic_missing // TODO Ski school icon
						else -> {
							Log.w(tag, "${names[it]} does not have an icon")
							null
						}
					}

					UIMapItem(names[it], hashmap[names[it]]?.get(0), icon)
				}

				Log.v(tag, "Finished loading other polygons")
			}.start()

			// Load the chairlift polygons file.
			viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
				Log.v(tag, "Started loading chairlift polygons")
				MapHandler.loadPolygons(this@DebugViewModel.map!!, R.raw.lift_polygons, activity,
					R.color.chairlift_polygon, true)
				Log.v(tag, "Finished loading chairlift polygons")
			}.start()

			// Load the easy polygons file.
			viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
				Log.v(tag, "Started loading easy polygons")
				MapHandler.loadPolygons(this@DebugViewModel.map!!, R.raw.easy_polygons, activity,
					R.color.easy_polygon, true)
				Log.v(tag, "Finished loading easy polygons")
			}.start()

			// Load the  moderate polygons file.
			viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
				Log.v(tag, "Started loading moderate polygons")
				MapHandler.loadPolygons(this@DebugViewModel.map!!, R.raw.moderate_polygons, activity,
					R.color.moderate_polygon, true)
				Log.v(tag, "Finished loading moderate polygons")
			}.start()

			// Load the difficult polygons file.
			viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
				Log.v(tag, "Started loading difficult polygons")
				MapHandler.loadPolygons(this@DebugViewModel.map!!, R.raw.difficult_polygons, activity,
					R.color.difficult_polygon, true)
				Log.v(tag, "Finished loading difficult polygons")
			}.start()
		}.start()
	}
}