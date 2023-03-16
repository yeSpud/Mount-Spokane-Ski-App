package com.mtspokane.skiapp.maphandlers

import android.os.Build
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.data.kml.KmlLineString
import com.google.maps.android.data.kml.KmlPlacemark
import com.google.maps.android.data.kml.KmlPolygon
import com.google.maps.android.ktx.addPolygon
import com.google.maps.android.ktx.addPolyline
import com.google.maps.android.ktx.utils.kml.kmlLayer
import com.mtspokane.skiapp.BuildConfig
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.MtSpokaneMapBounds
import com.mtspokane.skiapp.mapItem.PolylineMapItem

abstract class MapHandler(internal val activity: FragmentActivity) : OnMapReadyCallback {

	internal lateinit var map: GoogleMap

	var isNightOnly = false

	abstract val additionalCallback: OnMapReadyCallback

	lateinit var chairliftPolylines: List<PolylineMapItem>

	lateinit var easyRunsPolylines: List<PolylineMapItem>

	lateinit var moderateRunsPolylines: List<PolylineMapItem>

	lateinit var difficultRunsPolylines: List<PolylineMapItem>

	open fun destroy() {

		for (chairliftPolyline in chairliftPolylines) {
			chairliftPolyline.destroyUIItems()
		}

		for (easyRunPolyline in easyRunsPolylines) {
			easyRunPolyline.destroyUIItems()
		}

		for (moderateRunPolyline in moderateRunsPolylines) {
			moderateRunPolyline.destroyUIItems()
		}

		for (difficultRunsPolyline in difficultRunsPolylines) {
			difficultRunsPolyline.destroyUIItems()
		}

		// Clear the map if its not null.
		Log.v("MapHandler", "Clearing map.")
		this.map.clear()
	}

	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	override fun onMapReady(googleMap: GoogleMap) {

		val tag = "onMapReady"

		Log.v(tag, "Setting up map for the first time...")
		map = googleMap

		// Setup camera view logging.
		if (BuildConfig.DEBUG) {
			map.setOnCameraIdleListener {
				val cameraPosition: CameraPosition = map.cameraPosition

				val cameraTag = "OnCameraIdle"
				Log.d(cameraTag, "Bearing: ${cameraPosition.bearing}")
				Log.d(cameraTag, "Target: ${cameraPosition.target}")
				Log.d(cameraTag, "Tilt: ${cameraPosition.tilt}")
				Log.d(cameraTag, "Zoom: ${cameraPosition.zoom}")
			}
		}

		// Move the map camera view and set the view restrictions.
		map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
				.target(LatLng(47.92517834073426, -117.10480503737926)).tilt(45F)
				.bearing(317.50552F).zoom(14.414046F).build()))
		map.setLatLngBoundsForCameraTarget(CAMERA_BOUNDS)
		map.setMinZoomPreference(MINIMUM_ZOOM)
		map.setMaxZoomPreference(MAXIMUM_ZOOM)

		// Set the map view type to satellite.
		map.mapType = GoogleMap.MAP_TYPE_SATELLITE

		// Load the various polylines onto the map.
		Log.d(tag, "Loading polylines...")
		chairliftPolylines = loadPolylines(R.raw.lifts, R.color.chairlift, 4f, R.drawable.ic_chairlift)
		easyRunsPolylines = loadPolylines(R.raw.easy, R.color.easy, 3f, R.drawable.ic_easy)
		moderateRunsPolylines = loadPolylines(R.raw.moderate, R.color.moderate, 2f, R.drawable.ic_moderate)
		difficultRunsPolylines = loadPolylines(R.raw.difficult, R.color.difficult, 1f, R.drawable.ic_difficult)

		if (MtSpokaneMapBounds.other.isEmpty() || MtSpokaneMapBounds.skiAreaBounds == null) {
			Log.d(tag, "Adding other bounds...")
			val otherBounds = loadPolygons(R.raw.other, R.color.other_polygon_fill)
			for (name in otherBounds.keys) {
				val values = otherBounds[name]!!
				val polygonPoints: MutableList<List<LatLng>> = mutableListOf()
				for (value in values) {
					polygonPoints.add(value.points)
				}

				if (name == "Ski Area Bounds") {
					values[0].remove()
					MtSpokaneMapBounds.skiAreaBounds = MapItem(name, polygonPoints)
					break
				}

				val icon: Int? = when (name) {
					"Lodge 1" -> R.drawable.ic_lodge
					"Lodge 2" -> R.drawable.ic_lodge
					"Yurt" -> R.drawable.ic_yurt
					"Vista House" -> R.drawable.ic_vista_house
					"Ski Patrol Building" -> R.drawable.ic_ski_patrol_icon
					"Lodge 1 Parking Lot" -> R.drawable.ic_parking
					"Lodge 2 Parking Lot" -> R.drawable.ic_parking
					"Tubing Area" -> R.drawable.ic_missing // TODO Tubing area icon
					"Ski School" -> R.drawable.ic_missing // TODO Ski school icon
					else -> {
						Log.w(tag, "$name does not have an icon")
						null
					}
				}

				MtSpokaneMapBounds.other.add(MapItem(name, polygonPoints, icon))
			}
			Log.v(tag, "Finished adding other bounds")
		}

		if (MtSpokaneMapBounds.chairliftTerminals.isEmpty()) {
			Log.d(tag, "Adding chairlift terminals...")
			val chairliftTerminals = loadPolygons(R.raw.lift_terminal_polygons, R.color.chairlift_polygon)
			for (name in chairliftTerminals.keys) {
				val values = chairliftTerminals[name]!!
				val polygonPoints: MutableList<List<LatLng>> = mutableListOf()
				for (value in values) {
					polygonPoints.add(value.points)
				}
				MtSpokaneMapBounds.chairliftTerminals.add(MapItem(name, polygonPoints, R.drawable.ic_chairlift))
			}
			Log.v(tag, "Finished adding chairlift terminals")
		}

		if (MtSpokaneMapBounds.chairliftsBounds.isEmpty()) {
			Log.d(tag, "Adding chairlift bounds...")
			val chairliftBounds = loadPolygons(R.raw.lift_polygons, R.color.chairlift_polygon)
			for (name in chairliftBounds.keys) {
				val values = chairliftBounds[name]!!
				val polygonPoints: MutableList<List<LatLng>> = mutableListOf()
				for (value in values) {
					polygonPoints.add(value.points)
				}
				MtSpokaneMapBounds.chairliftsBounds.add(MapItem(name, polygonPoints, R.drawable.ic_chairlift))
			}
			Log.v(tag, "Finished adding chairlift bounds")
		}

		if (MtSpokaneMapBounds.easyRunsBounds.isEmpty()) {
			Log.d(tag, "Adding easy bounds...")
			val easyBounds = loadPolygons(R.raw.easy_polygons, R.color.easy_polygon)
			for (name in easyBounds.keys) {
				val values = easyBounds[name]!!
				val polygonPoints: MutableList<List<LatLng>> = mutableListOf()
				for (value in values) {
					polygonPoints.add(value.points)
				}
				MtSpokaneMapBounds.easyRunsBounds.add(MapItem(name, polygonPoints, R.drawable.ic_easy))
			}
			Log.v(tag, "Finished adding easy bounds")
		}

		if (MtSpokaneMapBounds.moderateRunsBounds.isEmpty()) {
			Log.d(tag, "Adding moderate bounds...")
			val moderateBounds = loadPolygons(R.raw.moderate_polygons, R.color.moderate_polygon)
			for (name in moderateBounds.keys) {
				val values = moderateBounds[name]!!
				val polygonPoints: MutableList<List<LatLng>> = mutableListOf()
				for (value in values) {
					polygonPoints.add(value.points)
				}
				MtSpokaneMapBounds.moderateRunsBounds.add(MapItem(name, polygonPoints, R.drawable.ic_moderate))
			}
			Log.v(tag, "Finished adding moderate bounds")
		}

		if (MtSpokaneMapBounds.difficultRunsBounds.isEmpty()) {
			Log.d(tag, "Adding difficult bounds...")
			val difficultBounds = loadPolygons(R.raw.difficult_polygons, R.color.difficult_polygon)
			for (name in difficultBounds.keys) {
				val values = difficultBounds[name]!!
				val polygonPoints: MutableList<List<LatLng>> = mutableListOf()
				for (value in values) {
					polygonPoints.add(value.points)
				}
				MtSpokaneMapBounds.difficultRunsBounds.add(MapItem(name, polygonPoints, R.drawable.ic_difficult))
			}
			Log.v(tag, "Finished adding difficult bounds")
		}

		Log.v("onMapReady", "Running additional setup steps...")
		additionalCallback.onMapReady(map)

		Log.v("onMapReady", "Finished setting up map.")
	}

	private fun parseKmlFile(@RawRes file: Int): Iterable<KmlPlacemark> {
		val kml = kmlLayer(map, file, activity)
		return kml.placemarks
	}

	@AnyThread
	//@Throws(NullPointerException::class)
	private fun loadPolylines(@RawRes fileRes: Int, @ColorRes color: Int, zIndex: Float,
	                          @DrawableRes icon: Int? = null): List<PolylineMapItem> {

		/*
		if (map == null) {
			throw NullPointerException("Map has not been setup yet!")
		}*/

		val hashMap: HashMap<String, PolylineMapItem> = HashMap()

		// Load the polyline from the file, and iterate though each placemark.
		for (placemark in parseKmlFile(fileRes)) {

			// Get the name of the polyline.
			val name: String = getPlacemarkName(placemark)

			// Get the LatLng coordinates of the placemark.
			val lineString: KmlLineString = placemark.geometry as KmlLineString
			val coordinates: ArrayList<LatLng> = lineString.geometryObject

			// Get the color of the polyline.
			val argb = this@MapHandler.getARGB(color)

			// Get the properties of the polyline.
			val polylineProperties: List<String>? = if (placemark.hasProperty(PROPERTY_KEY)) {
				placemark.getProperty(PROPERTY_KEY).split('\n')
			} else {
				null
			}

			// Check if the polyline is an easy way down polyline.
			val easiestWayDown = polylineHasProperty(polylineProperties, "easiest way down")

			// Create the polyline using the coordinates and other options.
			val polyline = map.addPolyline {
				addAll(coordinates)
				color(argb)
				if (easiestWayDown) {
					pattern(listOf(Gap(2.0F), Dash(8.0F)))
				}
				geodesic(true)
				startCap(RoundCap())
				endCap(RoundCap())
				clickable(false)
				width(8.0F)
				zIndex(zIndex)
				visible(true)
			}

			// Check if the map item is already in the hashmap.
			if (hashMap[name] == null) {

				// Check if this is a night item.
				val night = polylineHasProperty(polylineProperties, "night run")

				// Create a new map item for the polyline (since its not in the hashmap).
				val mapItem = PolylineMapItem(name, MutableList(1) { polyline }, isNightRun = night, icon = icon)

				// Add the map item to the hashmap.
				hashMap[name] = mapItem

			} else {

				// Add the polyline to the map item.
				hashMap[name]!!.polylines.add(polyline)
			}
		}

		return hashMap.values.toList()
	}

	@AnyThread
	//@Throws(NullPointerException::class)
	fun loadPolygons(@RawRes fileRes: Int, @ColorRes color: Int, visible: Boolean = BuildConfig.DEBUG):
			HashMap<String, List<Polygon>> {

		/*
		if (this@MapHandler.map == null) {
			throw NullPointerException("Map has not been setup yet!")
		}*/

		val hashMap: HashMap<String, List<Polygon>> = HashMap() // TODO Consider making this a set or regular map..

		// Load the polygons file.
		for (placemark in parseKmlFile(fileRes)) {

			val kmlPolygon: KmlPolygon = placemark.geometry as KmlPolygon

			val argb = getARGB(color)

			val polygon = map.addPolygon {
				addAll(kmlPolygon.outerBoundaryCoordinates)
				clickable(false)
				geodesic(true)
				zIndex(0.5F)
				fillColor(argb)
				strokeColor(argb)
				strokeWidth(8.0F)
				visible(visible)
			}

			val name: String = getPlacemarkName(placemark)
			Log.d("loadPolygons", "Loading polygon for $name")

			if (hashMap[name] == null) {
				hashMap[name] = List(1) { polygon }
			} else {
				val list: MutableList<Polygon> = hashMap[name]!!.toMutableList()
				list.add(polygon)
				hashMap[name] = list.toList()
			}
		}

		return hashMap
	}

	fun getARGB(@ColorRes color: Int): Int {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			activity.getColor(color)
		} else {
			ResourcesCompat.getColor(activity.resources, color, null)
		}
	}

	companion object {

		private val CAMERA_BOUNDS = LatLngBounds(LatLng(47.912728, -117.133402),
			LatLng(47.943674, -117.092470))

		private const val MINIMUM_ZOOM = 13.0F

		private const val MAXIMUM_ZOOM = 20.0F

		private const val PROPERTY_KEY = "description"

		private fun getPlacemarkName(placemark: KmlPlacemark): String {

			return if (placemark.hasProperty("name")) {
				placemark.getProperty("name")
			} else {

				// If the name wasn't found in the properties return an empty string.
				Log.w("getPlacemarkName", "Placemark is missing name!")
				""
			}
		}

		private fun polylineHasProperty(polylineProperties: List<String>?, propertyKey: String): Boolean {

			if (polylineProperties == null) {
				return false
			}

			for (property in polylineProperties) {
				Log.v("polylineHasProperty", "Checking if property \"$property\" matches property key \"$propertyKey\"")
				if (property.contains(propertyKey)) {
					return true
				}
			}

			return false
		}
	}
}