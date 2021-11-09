package com.mtspokane.skiapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.RawRes
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.maps.android.data.Geometry
import com.google.maps.android.data.kml.KmlLayer
import com.google.maps.android.data.kml.KmlLineString
import com.google.maps.android.data.kml.KmlPlacemark
import com.google.maps.android.data.kml.KmlPolygon

class MapHandler(val activity: MapsActivity): OnMapReadyCallback {

	lateinit var map: GoogleMap

	val chairlifts: HashMap<String, MapItem> = HashMap(6)

	val easyRuns: HashMap<String, MapItem> = HashMap(22)

	val moderateRuns: HashMap<String, MapItem> = HashMap(19)

	val difficultRuns: HashMap<String, MapItem> = HashMap(25)

	private val location = SkierLocation(this)

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

		// Move the camera.
		val cameraPosition = CameraPosition.Builder()
			.target(LatLng(47.924006680198424, -117.10511684417725))
			.tilt(45F)
			.bearing(317.50552F)
			.zoom(14F)
			.build()
		this.map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
		this.map.setLatLngBoundsForCameraTarget(LatLngBounds(LatLng(47.912728, -117.133402),
			LatLng(47.943674, -117.092470)))

		// Set the map to use satellite view.
		this.map.mapType = GoogleMap.MAP_TYPE_SATELLITE

		// Add the chairlifts to the map.
		this.createChairLifts()

		// Add the easy runs to the map.
		this.createEasyRuns()

		// Add the moderate runs to the map.
		this.createModerateRuns()

		// Add the difficult runs to the map.
		this.createDifficultRuns()

		// Request location permission, so that we can get the location of the device.
		// The result of the permission request is handled by a callback, onRequestPermissionsResult.
		// If this permission isn't granted then that's fine too.
		if (activity.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(),
				Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
			this.setupLocation()
		} else {
			ActivityCompat.requestPermissions(this.activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
				this.activity.permissionValue
			)
		}
	}

	private fun parseKmlFile(@RawRes file: Int): Iterable<KmlPlacemark> {
		val kml = KmlLayer(this.map, file, this.activity)
		return kml.placemarks
	}

	private fun createChairLifts() {

		// Load in the chairlift kml file, and iterate though each placemark.
		parseKmlFile(R.raw.lifts).forEach {

			// Get the name of the lift.
			val name: String = getPlacemarkName(it)

			// Get polyline.
			val polyline: Polyline = getPlacemarkPolyline(it, R.color.chairlift, 4F)

			// Check if the map item is already in the hashmap.
			if (this.chairlifts[name] == null) {

				// Create a map item and add it to the hashmap.
				this.chairlifts[name] = createMapItem(it, name, polyline)

			} else {

				// Add the polyline to the map item.
				this.chairlifts[name]!!.addPolyLine(polyline)
			}
		}
	}

	private fun createEasyRuns() {

		// Load in the easy runs kml file, and iterate though each placemark.
		parseKmlFile(R.raw.easy).forEach {

			// Get the name of the easy run.
			val name: String = getPlacemarkName(it)

			// Get the polyline.
			val polyline = getPlacemarkPolyline(it, R.color.easy, 3F)

			// Check if the map item is already in the hashmap.
			if (this.easyRuns[name] == null) {

				// Create a map item and add it to the hashmap.
				this.easyRuns[name] = createMapItem(it, name, polyline)

			} else {

				// Add the polyline to the map item.
				this.easyRuns[name]!!.addPolyLine(polyline)
			}
		}
	}

	private fun createModerateRuns() {

		// Load in the moderate runs kml file, and iterate though each placemark.
		parseKmlFile(R.raw.moderate).forEach {

			// Get the name of the moderate run.
			val name: String = getPlacemarkName(it)

			// Get the polyline.
			val polyline: Polyline = getPlacemarkPolyline(it, R.color.moderate, 2F)

			// Check if the map item is already in the hashmap.
			if (this.moderateRuns[name] == null) {

				// Create a map item and add it to the hashmap.
				this.moderateRuns[name] = createMapItem(it, name, polyline)

			} else {

				// Add the polyline to the map item.
				this.moderateRuns[name]!!.addPolyLine(polyline)
			}
		}
	}

	private fun createDifficultRuns() {

		// Load in the difficult runs kml file, and iterate though each placemark.
		parseKmlFile(R.raw.difficult).forEach {

			// Get the name of the difficult run.
			val name: String = getPlacemarkName(it)

			// Get the name and polyline.
			val polyline: Polyline = getPlacemarkPolyline(it, R.color.difficult, 1F)

			// Check if the map item is already in the hashmap.
			if (this.difficultRuns[name] == null) {

				// Create a map item and add it to the hashmap.
				this.difficultRuns[name] = createMapItem(it, name, polyline)

			} else {

				// Add the polyline to the map item.
				this.difficultRuns[name]!!.addPolyLine(polyline)
			}
		}
	}

	private fun createPolyline(vararg coordinates: LatLng, @ColorRes color: Int, zIndex: Float): Polyline {

		val argb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			this.activity.getColor(color)
		} else {
			ResourcesCompat.getColor(this.activity.resources, color, null)
		}

		return this.map.addPolyline(
			PolylineOptions()
			.add(*coordinates)
			.color(argb)
			.geodesic(true)
			.startCap(RoundCap())
			.endCap(RoundCap())
			.clickable(false)
			.width(8F)
			.zIndex(zIndex)
			.visible(true))
	}

	private fun getPlacemarkPolyline(placemark: KmlPlacemark, @ColorRes color: Int, zIndex: Float): Polyline {

		// Get the LatLng coordinates of the placemark.
		val lineString: KmlLineString = placemark.geometry as KmlLineString
		val coordinates: Array<LatLng> = lineString.geometryObject.toTypedArray()

		// Create the polyline using the coordinates.
		return createPolyline(*coordinates, color = color, zIndex = zIndex)
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

	private fun createMapItem(placemark: KmlPlacemark, name: String, polyline: Polyline): MapItem {

		// Check if this is a night item. Its a night item if the property contains a description.
		val night = placemark.hasProperty("description")

		// Create a new map item for the polyline (since its not in the hashmap).
		val mapItem = MapItem(name, night)
		mapItem.addPolyLine(polyline)

		return mapItem
	}

	private fun createChairliftPolygons() {
		// TODO
	}

	private fun createEasyPolygons() {

		// Load the easy polygons file.
		parseKmlFile(R.raw.easy_polygons).forEach {

			val polygon: Polygon = createPolygon(it.geometry, R.color.easy_polygon)

			val name: String = getPlacemarkName(it)

			// Try to find the MapItem with the name of the polygon.
			if (this.easyRuns[name] != null) {

				// Add the polygon to the MapItem.
				this.easyRuns[name]!!.addPolygon(polygon)
			}
		}
	}

	private fun createModeratePolygons() {
		// TODO
	}

	private fun createDifficultPolygons() {
		// TODO
	}

	private fun createPolygon(kmlPolygon: Geometry<Any>, @ColorRes color: Int): Polygon {

		val polygon: KmlPolygon = kmlPolygon as KmlPolygon

		val argb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			this.activity.getColor(color)
		} else {
			ResourcesCompat.getColor(this.activity.resources, color, null)
		}

		return this.map.addPolygon(PolygonOptions()
			.add(*polygon.outerBoundaryCoordinates.toTypedArray())
			.clickable(false)
			.geodesic(true).zIndex(0F)
			.fillColor(argb)
			.strokeColor(argb)
			.strokeWidth(8F)
			.visible(BuildConfig.DEBUG))

	}

	fun setupLocation() {
		this.createChairliftPolygons()
		this.createEasyPolygons()
		this.createModeratePolygons()
		this.createDifficultPolygons()

		this.showLocation()
	}

	@SuppressLint("MissingPermission")
	fun showLocation() {

		val locationManager = this.activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager

		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
				2F, this.location)
		}
	}
}