package com.mtspokane.skiapp.debugview

import android.graphics.Color
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
import com.google.maps.android.data.kml.KmlPolygon
import com.google.maps.android.ktx.awaitMap
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.mapItem.UIMapItem
import com.mtspokane.skiapp.mapactivity.MapHandler
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

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
		this.map!!.setLatLngBoundsForCameraTarget(
			LatLngBounds(
				LatLng(47.912728,
			-117.133402), LatLng(47.943674, -117.092470)
			)
		)
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

				var otherIndex = 0

				// Load the other polygons file.
				MapHandler.parseKmlFile(this@DebugViewModel.map!!, R.raw.other, activity).forEach {

					// Get the name of the other polygon.
					val name: String = MapHandler.getPlacemarkName(it)

					// Get the polygon from the file.
					val kmlPolygon: KmlPolygon = it.geometry as KmlPolygon

					// If the polygon is the ski area bounds then add it to the map as its own object.
					withContext(Dispatchers.Main) {
						if (name == "Ski Area Bounds") {
							Log.d(tag, "Adding bounds to map")

							val skiAreaBoundsPolygon: Polygon = MapHandler.addPolygonToMap(this@DebugViewModel.map!!,
								kmlPolygon.outerBoundaryCoordinates, 0.0F, Color.TRANSPARENT,
								Color.MAGENTA, 1.0F)

							UIMapItem("Ski Area Bounds", skiAreaBoundsPolygon)

						} else {

							// Load the other polygons as normal.
							val polygon: Polygon = MapHandler.addPolygonToMap(this@DebugViewModel.map!!,
								kmlPolygon.outerBoundaryCoordinates, 0.5F, R.color.other_polygon_fill,
								Color.MAGENTA, 8F)

							val item = UIMapItem(name, polygon)

							val icon: Int? = when (name) {
								"Lodge 1" -> R.drawable.ic_missing // TODO Lodge icon
								"Lodge 2" -> R.drawable.ic_missing // TODO Lodge icon
								"Yurt" -> R.drawable.ic_yurt
								"Vista House" -> R.drawable.ic_missing // TODO Vista house icon
								"Ski Patrol" -> R.drawable.ic_ski_patrol_icon
								"Lodge 1 Parking Lot" -> R.drawable.ic_parking
								"Lodge 2 Parking Lot" -> R.drawable.ic_parking
								"Tubing Area" -> R.drawable.ic_missing // TODO Tubing area icon
								else -> {
									Log.w(tag, "$name does not have an icon")
									null
								}
							}

							if (icon != null) {
								item.setIcon(icon)
							}
							otherIndex++

							Log.i(tag, "Added item: $name")
						}
					}
				}
				Log.v(tag, "Finished loading other polygons")
			}.start()

			// Load the chairlift polygons file.
			viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
				Log.v(tag, "Started loading chairlift polygons")
				MapHandler.loadPolygons(this@DebugViewModel.map!!, R.raw.lift_polygons, activity,
					R.color.chairlift_polygon, emptyArray())
				Log.v(tag, "Finished loading chairlift polygons")
			}.start()

			// Load the easy polygons file.
			viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
				Log.v(tag, "Started loading easy polygons")
				MapHandler.loadPolygons(this@DebugViewModel.map!!, R.raw.easy_polygons, activity,
					R.color.easy_polygon, emptyArray())
				Log.v(tag, "Finished loading easy polygons")
			}.start()

			// Load the  moderate polygons file.
			viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
				Log.v(tag, "Started loading moderate polygons")
				MapHandler.loadPolygons(this@DebugViewModel.map!!, R.raw.moderate_polygons, activity,
					R.color.moderate_polygon, emptyArray())
				Log.v(tag, "Finished loading moderate polygons")
			}.start()

			// Load the difficult polygons file.
			viewModelScope.async(Dispatchers.IO, CoroutineStart.LAZY) {
				Log.v(tag, "Started loading difficult polygons")
				MapHandler.loadPolygons(this@DebugViewModel.map!!, R.raw.difficult_polygons, activity,
					R.color.difficult_polygon, emptyArray())
				Log.v(tag, "Finished loading difficult polygons")
			}.start()
		}.start()
	}

}