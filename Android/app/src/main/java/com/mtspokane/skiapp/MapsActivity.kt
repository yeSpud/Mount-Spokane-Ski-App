package com.mtspokane.skiapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.GoogleMap
import android.os.Bundle
import android.os.Process
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.ColorRes
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.data.kml.KmlLayer
import com.google.maps.android.data.kml.KmlLineString
import com.google.maps.android.data.kml.KmlPlacemark
import com.mtspokane.skiapp.databinding.ActivityMapsBinding

class MapsActivity : FragmentActivity(), OnMapReadyCallback {

	private lateinit var map: GoogleMap

	private val chairlifts: HashMap<String, MapItem> = HashMap(6)

	private val easyRuns: HashMap<String, MapItem> = HashMap(22)

	private val moderateRuns: HashMap<String, MapItem> = HashMap(19)

	private val difficultRuns: HashMap<String, MapItem> = HashMap(25)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Setup data binding.
		val binding = ActivityMapsBinding.inflate(layoutInflater)
		setContentView(binding.root)

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
		mapFragment!!.getMapAsync(this)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		this.menuInflater.inflate(R.menu.menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {

		val checked = !item.isChecked

		item.isChecked = checked

		when (item.itemId) {
			R.id.chairlift -> this.chairlifts.forEach{it.value.togglePolyLineVisibility(checked)}
			R.id.easy -> this.easyRuns.forEach{it.value.togglePolyLineVisibility(checked)}
			R.id.moderate -> this.moderateRuns.forEach{it.value.togglePolyLineVisibility(checked)}
			R.id.difficult -> this.difficultRuns.forEach{it.value.togglePolyLineVisibility(checked)}
			R.id.night -> {
				this.chairlifts.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
				this.easyRuns.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
				this.moderateRuns.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
				this.difficultRuns.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
			}
		}

		return super.onOptionsItemSelected(item)
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
		if (this.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(),
				Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
			showLocation()
		} else {
			ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
				PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
		}
	}

	private fun createChairLifts() {

		// Load in the chairlift kml file.
		val kml = KmlLayer(this.map, R.raw.lifts, this)

		// Iterate though each placemark...
		kml.placemarks.forEach {

			// Get the name and polyline.
			val hashPair = getHashmapPair(it, R.color.chairlift, 4, this.map)

			// Check if the map item is already in the hashmap.
			if (this.chairlifts[hashPair.first] == null) {

				// Create a map item and add it to the hashmap.
				this.chairlifts[hashPair.first] = createMapItem(it.properties, hashPair.first, hashPair.second)

			} else {

				// Add the polyline to the map item.
				this.chairlifts[hashPair.first]!!.addPolyLine(hashPair.second)
			}
		}
	}

	private fun createEasyRuns() {

		// Load in the easy runs kml file.
		val kml = KmlLayer(this.map, R.raw.easy, this)

		// Iterate though each placemark...
		kml.placemarks.forEach {

			// Get the name and polyline.
			val hashPair = getHashmapPair(it, R.color.easy, 3, this.map)

			// Check if the map item is already in the hashmap.
			if (this.easyRuns[hashPair.first] == null) {

				// Create a map item and add it to the hashmap.
				this.easyRuns[hashPair.first] = createMapItem(it.properties, hashPair.first, hashPair.second)

			} else {

				// Add the polyline to the map item.
				this.easyRuns[hashPair.first]!!.addPolyLine(hashPair.second)
			}
		}
	}

	private fun createModerateRuns() {

		// Load in the moderate runs kml file.
		val kml = KmlLayer(this.map, R.raw.moderate, this)

		// Iterate though each placemark...
		kml.placemarks.forEach {

			// Get the name and polyline.
			val hashPair = getHashmapPair(it, R.color.moderate, 2, this.map)

			// Check if the map item is already in the hashmap.
			if (this.moderateRuns[hashPair.first] == null) {

				// Create a map item and add it to the hashmap.
				this.moderateRuns[hashPair.first] = createMapItem(it.properties, hashPair.first, hashPair.second)

			} else {

				// Add the polyline to the map item.
				this.moderateRuns[hashPair.first]!!.addPolyLine(hashPair.second)
			}
		}
	}

	private fun createDifficultRuns() {

		// Load in the difficult runs kml file.
		val kml = KmlLayer(this.map, R.raw.difficult, this)

		// Iterate though each placemark...
		kml.placemarks.forEach {

			// Get the name and polyline.
			val hashPair = getHashmapPair(it, R.color.difficult, 1, this.map)

			// Check if the map item is already in the hashmap.
			if (this.difficultRuns[hashPair.first] == null) {

				// Create a map item and add it to the hashmap.
				this.difficultRuns[hashPair.first] = createMapItem(it.properties, hashPair.first, hashPair.second)

			} else {

				// Add the polyline to the map item.
				this.difficultRuns[hashPair.first]!!.addPolyLine(hashPair.second)
			}
		}
	}

	private fun createPolyline(vararg coordinates: LatLng, @ColorRes color: Int, zIndex: Short,
	                           map: GoogleMap): Polyline {

		val argb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			this.getColor(color)
		} else {
			ResourcesCompat.getColor(this.resources, color, null)
		}

		return map.addPolyline(PolylineOptions()
			.add(*coordinates)
			.color(argb)
			.geodesic(true)
			.startCap(RoundCap())
			.endCap(RoundCap())
			.clickable(false)
			.width(8F)
			.zIndex(zIndex.toFloat())
			.visible(true))
	}

	private fun getHashmapPair(placemark: KmlPlacemark, @ColorRes color: Int, zIndex: Short,
	                           map: GoogleMap): Pair<String, Polyline> {

		// Get the name of the placemark.
		@Suppress("UNCHECKED_CAST")
		val name = (placemark.properties.elementAt(0) as Map.Entry<String, String>).value

		// Get the LatLng coordinates of the placemark.
		val lineString: KmlLineString = placemark.geometry as KmlLineString
		val coordinates: Array<LatLng> = lineString.geometryObject.toTypedArray()

		// Create the polyline using the coordinates.
		val polyline = createPolyline(*coordinates, color = color, zIndex = zIndex, map = map)

		// Return the name and polyline as a pair for the hashmap.
		return Pair(name, polyline)
	}

	private fun createMapItem(properties: MutableIterable<Any?>, name: String, polyline: Polyline): MapItem {

		// Check if this is a night item.
		var night = false
		if (properties.count() == 2) {

			// Its a night item if it the 2nd property contains "night run".
			@Suppress("UNCHECKED_CAST")
			night = (properties.elementAt(1) as Map.Entry<String, String>).value.contains("night run")
		}

		// Create a new map item for the polyline (since its not in the hashmap).
		val mapItem = MapItem(name, night)
		mapItem.addPolyLine(polyline)

		return mapItem
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		when (requestCode) {

			// If request is cancelled, the result arrays are empty.
			PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
				if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					showLocation()
				}
			}
		}
	}

	@SuppressLint("MissingPermission")
	private fun showLocation() {

		val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
				2F, SkierLocation(this.map, this.resources))
		}

	}

	companion object {
		private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 29500
	}
}