package com.mtspokane.skiapp

import android.graphics.Color
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.GoogleMap
import android.os.Bundle
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.mtspokane.skiapp.databinding.ActivityMapsBinding

class MapsActivity : FragmentActivity(), OnMapReadyCallback {

	private val chairlifts: Array<Polyline?> = arrayOfNulls(6)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

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

		// Add a marker in Sydney and move the camera
		val cameraPosition = CameraPosition.Builder().target(LatLng(47.924006680198424, -117.10511684417725))
			.tilt(45F)
			.bearing(317.50552F)
			.zoom(14F)
			.build()
		googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
		googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
		googleMap.setLatLngBoundsForCameraTarget(LatLngBounds(LatLng(47.912728, -117.133402),
			LatLng(47.943674, -117.092470)))

		this.chairlifts[0] = addChairLift(47.91606715553383, -117.099266845541,
			47.92294353366535, -117.1126129810919, "Chair 1", googleMap)

		this.chairlifts[1] = addChairLift(47.92221929989261, -117.098637384573,
			47.9250541084338, -117.1119355485026, "Chair 2", googleMap)

		this.chairlifts[2] = addChairLift(47.92301666633388, -117.0966530617209,
			47.93080242968863, -117.1039234488206, "Chair 3", googleMap)

		this.chairlifts[3] = addChairLift(47.94163035979481, -117.1005550502552,
			47.9323389155571, -117.1067590655054, "Chair 4", googleMap)

		this.chairlifts[4] = addChairLift(47.92175256734555, -117.0954266523773,
			47.92292940522836, -117.0989319661659, "Chair 5", googleMap)

		this.chairlifts[5] = addChairLift(47.92891149682423, -117.1299404320796,
			47.92339173840757, -117.112973282171, "Chair 6", googleMap)
	}

	private fun addChairLift(startLatitude: Double, startLongitude: Double, endLatitude: Double,
	                         endLongitude: Double, name: String, map: GoogleMap): Polyline {
		return createPolyline(LatLng(startLatitude, startLongitude), LatLng(endLatitude, endLongitude),
			color = Color.RED, name = name, map = map)
	}

	private fun addEasyRun(vararg coordinates: LatLng, name: String, map: GoogleMap): Polyline {
		return createPolyline(*coordinates, color = Color.GREEN, name = name, map = map)
	}

	private fun createPolyline(vararg coordinates: LatLng, color: Int, name: String, map: GoogleMap): Polyline {
		val polyline = map.addPolyline(PolylineOptions()
			.add(*coordinates)
			.color(color)
			.geodesic(true)
			.startCap(RoundCap())
			.endCap(RoundCap())
			.clickable(false)
			.width(8F)
			.visible(true))
		polyline.tag = name
		return polyline
	}
}