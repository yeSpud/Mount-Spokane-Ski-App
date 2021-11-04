package com.mtspokane.skiapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style
import com.mtspokane.skiapp.databinding.ActivityMapsBinding

class MapsActivity : FragmentActivity() {

	private lateinit var viewModel: MapsViewModel

	private lateinit var mapView: MapView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Startup mapbox.
		Mapbox.getInstance(this.applicationContext, this.getString(R.string.mapbox_access_token))

		// Setup the viewmodel.
		this.viewModel = MapsViewModel(this)

		// Setup data binding.
		val binding = ActivityMapsBinding.inflate(layoutInflater)
		setContentView(binding.root)

		// Setup the mapview.
		this.mapView = binding.mapView
		this.mapView.getMapAsync {
			it.setStyle(Style.SATELLITE)
		}

		this.mapView.onCreate(savedInstanceState)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		this.menuInflater.inflate(R.menu.menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {

		val checked = !item.isChecked

		item.isChecked = checked

		/*
		when (item.itemId) {
			R.id.chairlift -> this.viewModel.chairlifts.forEach{it.value.togglePolyLineVisibility(checked)}
			R.id.easy -> this.viewModel.easyRuns.forEach{it.value.togglePolyLineVisibility(checked)}
			R.id.moderate -> this.viewModel.moderateRuns.forEach{it.value.togglePolyLineVisibility(checked)}
			R.id.difficult -> this.viewModel.difficultRuns.forEach{it.value.togglePolyLineVisibility(checked)}
			R.id.night -> {
				this.viewModel.chairlifts.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
				this.viewModel.easyRuns.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
				this.viewModel.moderateRuns.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
				this.viewModel.difficultRuns.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
			}
		}
		 */

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
	/*
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
		this.viewModel.createChairLifts(googleMap)

		// Add the easy runs to the map.
		this.viewModel.createEasyRuns(googleMap)

		// Add the moderate runs to the map.
		this.viewModel.createModerateRuns(googleMap)

		// Add the difficult runs to the map.
		this.viewModel.createDifficultRuns(googleMap)

		// Request location permission, so that we can get the location of the device.
		// The result of the permission request is handled by a callback, onRequestPermissionsResult.
		// If this permission isn't granted then that's fine too.
		if (this.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(),
				Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
			showLocation()
		} else {
			ActivityCompat.requestPermissions(
				this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
				PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
			)
		}
	} */

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

		/*
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
				2F, SkierLocation(this.map, this.resources))
		} */

	}

	companion object {
		private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 29500
	}
}