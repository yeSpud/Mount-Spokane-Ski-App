package com.mtspokane.skiapp

import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.GoogleMap
import android.os.Bundle
import android.util.Log
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.mtspokane.skiapp.databinding.ActivityMapsBinding

class MapsActivity : FragmentActivity(), OnMapReadyCallback {

	private var mMap: GoogleMap? = null

	private var binding: ActivityMapsBinding? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMapsBinding.inflate(layoutInflater)
		setContentView(binding!!.root)

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
		mMap = googleMap

		// Add a marker in Sydney and move the camera
		val cameraPosition = CameraPosition.Builder().target(LatLng(47.924006680198424, -117.10511684417725))
			.tilt(45F)
			.bearing(317.50552F)
			.zoom(14F)
			.build()
		mMap!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
		mMap!!.mapType = GoogleMap.MAP_TYPE_SATELLITE
		mMap!!.setLatLngBoundsForCameraTarget(LatLngBounds(LatLng(47.912728, -117.133402), LatLng(47.943674, -117.092470)))
		mMap!!.setOnCameraIdleListener {
			val position = mMap!!.cameraPosition
			Log.i("Camera Positions", "Location: ${position.target.latitude}, ${position.target.longitude}; Tilt: ${position.tilt}; Bearing: ${position.bearing}; Zoom: ${position.zoom}")
		}
	}
}