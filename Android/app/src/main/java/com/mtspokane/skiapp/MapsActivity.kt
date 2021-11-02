package com.mtspokane.skiapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.GoogleMap
import android.os.Bundle
import android.os.Process
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.mtspokane.skiapp.databinding.ActivityMapsBinding

class MapsActivity : FragmentActivity(), OnMapReadyCallback {

	private val viewModel: MapsViewModel by viewModels()

	private lateinit var map: GoogleMap

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Setup data binding.
		val binding = ActivityMapsBinding.inflate(layoutInflater)
		setContentView(binding.root)

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
		mapFragment!!.getMapAsync(this)
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
		if (this.checkPermission(
				Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(),
				Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
			showLocation()
		} else {
			ActivityCompat.requestPermissions(
				this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
				PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
			)
		}
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