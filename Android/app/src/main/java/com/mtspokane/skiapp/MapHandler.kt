package com.mtspokane.skiapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.MainThread
import androidx.annotation.RawRes
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.maps.android.data.kml.KmlLineString
import com.google.maps.android.data.kml.KmlPlacemark
import com.google.maps.android.data.kml.KmlPolygon
import com.google.maps.android.ktx.addPolygon
import com.google.maps.android.ktx.addPolyline
import com.google.maps.android.ktx.utils.kml.kmlLayer
import kotlinx.coroutines.*

class MapHandler(val activity: MapsActivity): OnMapReadyCallback {

	lateinit var map: GoogleMap

	val chairlifts: HashMap<String, MapItem> = HashMap(6)

	val easyRuns: HashMap<String, MapItem> = HashMap(22)

	val moderateRuns: HashMap<String, MapItem> = HashMap(19)

	val difficultRuns: HashMap<String, MapItem> = HashMap(25)

	val other: Array<MapItem?> = arrayOfNulls(6)

	lateinit var skiAreaBounds: Polygon

	private val inAppLocationHandler = InAppSkierLocation(this)

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

		this.map = googleMap

		val tag = "onMapReady"

		if (BuildConfig.DEBUG) {
			this.map.setOnCameraIdleListener {
				val cameraPosition: CameraPosition = this.map.cameraPosition

				Log.v("OnCameraIdle", "Bearing: ${cameraPosition.bearing}")
				Log.v("OnCameraIdle", "Target: ${cameraPosition.target}")
				Log.v("OnCameraIdle", "Tilt: ${cameraPosition.tilt}")
				Log.v("OnCameraIdle", "Zoom: ${cameraPosition.zoom}")
			}
		}

		// Move the camera.
		val cameraPosition = CameraPosition.Builder()
			.target(LatLng(47.92517834073426, -117.10480503737926))
			.tilt(45F)
			.bearing(317.50552F)
			.zoom(14.414046F)
			.build()
		this.map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
		this.map.setLatLngBoundsForCameraTarget(LatLngBounds(LatLng(47.912728, -117.133402),
			LatLng(47.943674, -117.092470)))
		this.map.setMaxZoomPreference(20F)
		this.map.setMinZoomPreference(13F)

		// Set the map to use satellite view.
		this.map.mapType = GoogleMap.MAP_TYPE_SATELLITE

		this.activity.lifecycleScope.async(Dispatchers.Main, CoroutineStart.LAZY) {

			val polylineLoads = listOf(

				// Add the chairlifts to the map.
				// Load in the chairlift kml file, and iterate though each placemark.
				async(Dispatchers.Main, CoroutineStart.DEFAULT) {
					Log.v(tag, "Started loading chairlift polylines")
					loadPolylines(this@MapHandler.map, R.raw.lifts, this@MapHandler.activity,
						R.color.chairlift, 4f, this@MapHandler.chairlifts)
					Log.v(tag, "Finished loading chairlift polylines")},

				// Load in the easy runs kml file, and iterate though each placemark.
				async(Dispatchers.Main, CoroutineStart.DEFAULT) {
					Log.v(tag, "Started loading easy polylines")
					loadPolylines(this@MapHandler.map, R.raw.easy, this@MapHandler.activity,
						R.color.easy, 3f, this@MapHandler.easyRuns)
					Log.v(tag, "Finished loading easy run polylines")},

				// Load in the moderate runs kml file, and iterate though each placemark.
				async(Dispatchers.Main, CoroutineStart.DEFAULT) {
					Log.v(tag, "Started loading moderate polylines")
					loadPolylines(this@MapHandler.map, R.raw.moderate, this@MapHandler.activity,
						R.color.moderate, 2f, this@MapHandler.moderateRuns)
					Log.v(tag, "Finished loading moderate run polylines")},

				// Load in the difficult runs kml file, and iterate though each placemark.
				async(Dispatchers.Main, CoroutineStart.DEFAULT) {
					Log.v(tag, "Started loading difficult polylines")
					loadPolylines(this@MapHandler.map, R.raw.difficult, this@MapHandler.activity,
						R.color.difficult, 1f, this@MapHandler.difficultRuns)
					Log.v(tag, "Finished loading difficult polylines")}
			)

			// Wait for all the polylines to load before checking permissions.
			polylineLoads.awaitAll()

			// Request location permission, so that we can get the location of the device.
			// The result of the permission request is handled by a callback, onRequestPermissionsResult.
			// If this permission isn't granted then that's fine too.
			Log.v("onMapReady", "Checking location permissions...")
			if (this@MapHandler.activity.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION,
					Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
				this@MapHandler.setupLocation()
			} else {

				// Show the info popup about location.
				this@MapHandler.activity.locationPopupDialog.show()
			}
		}.start()
	}

	suspend fun setupLocation() = coroutineScope {

		val tag = "setupLocation"

		val polygonLoads = listOf(

			// Other polygons (lodges, parking lots, vista house, tubing area, yurt, ski patrol building, and ski area bounds)
			async(Dispatchers.Main) {
				Log.v(tag, "Started loading other polygons")

				var otherIndex = 0

				// Load the other polygons file.
				parseKmlFile(this@MapHandler.map, R.raw.other, this@MapHandler.activity).forEach {

					// Get the name of the other polygon.
					val name: String = getPlacemarkName(it)

					// Get the polygon from the file.
					val kmlPolygon: KmlPolygon = it.geometry as KmlPolygon

					// If the polygon is the ski area bounds then add it to the map as its own object.
					if (name == "Ski Area Bounds") {
						Log.d(tag, "Adding bounds to map")
						this@MapHandler.skiAreaBounds = addPolygonToMap(this@MapHandler.map,
							kmlPolygon.outerBoundaryCoordinates, 0.0F, 0x59CCCCCC,
							Color.MAGENTA, 1.0F)
					} else {

						// Load the other polygons as normal.

						val item = MapItem(name)

						val polygon: Polygon = addPolygonToMap(map, kmlPolygon.outerBoundaryCoordinates,
							0.5F, 0x59CCCCCC, Color.MAGENTA, 8F)

						item.addPolygon(polygon)

						this@MapHandler.other[otherIndex] = item
						otherIndex++

						Log.i(tag, "Added item: $name")
					}
				}
				Log.v(tag, "Finished loading other polygons")
			},

			// TODO Chairlift polygons

			// Load the easy polygons file.
			async(Dispatchers.Main) {
				Log.v(tag, "Started loading easy polygons")
				loadPolygons(this@MapHandler.map, R.raw.easy_polygons, this@MapHandler.activity,
					R.color.easy_polygon, this@MapHandler.easyRuns)
				Log.v(tag, "Finished loading easy polygons")
			},

			// Load the  moderate polygons file.
			async(Dispatchers.Main) {
				Log.v(tag, "Started loading moderate polygons")
				loadPolygons(this@MapHandler.map, R.raw.moderate_polygons, this@MapHandler.activity,
					R.color.moderate_polygon, this@MapHandler.moderateRuns)
				Log.v(tag, "Finished loading moderate polygons")
			}

			// TODO Difficult polygons

		)

		polygonLoads.awaitAll() // Wait for all loads to have finished...

		Log.v(tag, "Showing location on map...")
		this@MapHandler.showLocation()
	}

	@SuppressLint("MissingPermission")
	fun showLocation() { // TODO Run this as a foreground service.

		val locationManager = this.activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager

		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
				2F, this.inAppLocationHandler)
		}
	}

	companion object {

		private fun parseKmlFile(map: GoogleMap, @RawRes file: Int, activity: MapsActivity): Iterable<KmlPlacemark> {
			val kml = kmlLayer(map, file, activity)
			return kml.placemarks
		}

		@MainThread
		private fun loadPolylines(map: GoogleMap, @RawRes file: Int, activity: MapsActivity,
		                          @ColorRes color: Int, zIndex: Float, hashMap: HashMap<String, MapItem>) {

			// Load in the polyline's kml file, and iterate though each placemark.
			parseKmlFile(map, file, activity).forEach {

				// Get the name of the polyline.
				val name: String = getPlacemarkName(it)

				// Get the LatLng coordinates of the placemark.
				val lineString: KmlLineString = it.geometry as KmlLineString
				val coordinates = lineString.geometryObject

				// Get the color of the polyline.
				val argb = getARGB(activity, color)

				// Create the polyline using the coordinates and other options.
				val polyline: Polyline = map.addPolyline {
					addAll(coordinates)
					color(argb)
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

					// Check if this is a night item. Its a night item if the property contains a description.
					val night = it.hasProperty("description")

					// Create a new map item for the polyline (since its not in the hashmap).
					val mapItem = MapItem(name, night)
					mapItem.addPolyLine(polyline)

					// Add the map item to the hashmap.
					hashMap[name] = mapItem

				} else {

					// Add the polyline to the map item.
					hashMap[name]!!.addPolyLine(polyline)
				}
			}
		}

		private fun getARGB(activity: MapsActivity, @ColorRes color: Int): Int {
			return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				activity.getColor(color)
			} else {
				ResourcesCompat.getColor(activity.resources, color, null)
			}
		}

		private fun getPlacemarkName(placemark: KmlPlacemark): String {

			return if (placemark.hasProperty("name")) {
				placemark.getProperty("name")
			} else {

				// If the name wasn't found in the properties return an empty string.
				Log.w("getPlacemarkName", "Placemark is missing name!")
				""
			}
		}

		@MainThread
		private fun loadPolygons(map: GoogleMap, @RawRes file: Int, activity: MapsActivity,
		                         @ColorRes color: Int, hashMap: HashMap<String, MapItem>) {

			// Load the polygons file.
			parseKmlFile(map, file, activity).forEach {

				val kmlPolygon: KmlPolygon = it.geometry as KmlPolygon

				val argb = getARGB(activity, color)

				val polygon: Polygon = addPolygonToMap(map, kmlPolygon.outerBoundaryCoordinates,
					0.5F, argb, argb, 8F)

				val name: String = getPlacemarkName(it)

				// Try to find the MapItem with the name of the polygon.
				if (hashMap[name] != null) {

					// Add the polygon to the MapItem.
					hashMap[name]!!.addPolygon(polygon)
				}
			}
		}

		@MainThread
		private fun addPolygonToMap(map: GoogleMap, points: Iterable<LatLng>, zIndex: Float,
		                            fillColor: Int, strokeColor: Int, strokeWidth: Float): Polygon {
			return map.addPolygon {
				addAll(points)
				clickable(false)
				geodesic(true)
				zIndex(zIndex)
				fillColor(fillColor)
				strokeColor(strokeColor)
				strokeWidth(strokeWidth)
				visible(BuildConfig.DEBUG)
			}
		}
	}
}