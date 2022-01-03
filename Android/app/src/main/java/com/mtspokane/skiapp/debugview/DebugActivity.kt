package com.mtspokane.skiapp.debugview

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.ktx.addMarker
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databinding.ActivityDebugBinding
import com.mtspokane.skiapp.skierlocation.Locations

class DebugActivity : FragmentActivity() {

	private var locationChangeCallback: Locations.VisibleLocationUpdate? = null

	lateinit var viewModel: DebugViewModel

	private var previousLocationName: CharSequence = ""

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		this.viewModel = ViewModelProvider(this).get(DebugViewModel::class.java)

		val binding = ActivityDebugBinding.inflate(this.layoutInflater)
		this.setContentView(binding.root)

		this.actionBar!!.setDisplayShowTitleEnabled(true)

		this.locationChangeCallback = object : Locations.VisibleLocationUpdate {
			override fun updateLocation(locationString: String) {

				if (Locations.currentLocation == null) {
					return
				}
				this@DebugActivity.previousLocationName = binding.currentLocationName.text
				binding.currentLocationName.text = locationString
				binding.currentLocationAccuracy.text = this@DebugActivity.getString(R.string.current_location_accuracy, Locations.currentLocation!!.accuracy)
				binding.currentLocationAltitude.text = this@DebugActivity.getString(R.string.current_location_altitude, Locations.currentLocation!!.altitude)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					binding.currentLocationAltitudeAccuracy.text = this@DebugActivity.getString(R.string.current_location_altitude_accuracy, Locations.currentLocation!!.verticalAccuracyMeters)
				} else {
					binding.currentLocationAltitudeAccuracy.text = this@DebugActivity.getString(R.string.current_location_altitude_accuracy, 0)
				}

				binding.currentLocationLatitude.text = this@DebugActivity.getString(R.string.current_location_latitude, Locations.currentLocation!!.latitude)
				binding.currentLocationLongitude.text = this@DebugActivity.getString(R.string.current_location_longitude, Locations.currentLocation!!.longitude)
				binding.currentLocationSpeed.text = this@DebugActivity.getString(R.string.current_location_speed, Locations.currentLocation!!.speed)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					binding.currentLocationSpeedAccuracy.text = this@DebugActivity.getString(R.string.current_location_speed_accuracy, Locations.currentLocation!!.speedAccuracyMetersPerSecond)
				} else {
					binding.currentLocationSpeedAccuracy.text = this@DebugActivity.getString(R.string.current_location_speed_accuracy, 0)
				}

				if (this@DebugActivity.viewModel.map != null) {

					// If the marker hasn't been added to the map create a new one.
					if (this@DebugActivity.viewModel.locationMarker == null) {
						this@DebugActivity.viewModel.locationMarker = this@DebugActivity.viewModel.map!!.addMarker {
							position(LatLng(Locations.currentLocation!!.latitude, Locations.currentLocation!!.longitude))
							title(this@DebugActivity.resources.getString(R.string.your_location))
						}
					} else {

						// Otherwise just update the LatLng location.
						this@DebugActivity.viewModel.locationMarker!!.position = LatLng(Locations.currentLocation!!.latitude, Locations.currentLocation!!.longitude)
					}
				}

				binding.verticalDirection.text = this@DebugActivity.getString(R.string.vertical_direction, Locations.getVerticalDirection().name)
				binding.chairliftConfidence.text = this@DebugActivity.getString(R.string.chairlift_confidence, Locations.chairliftConfidence, 6)

				if (Locations.previousLocation == null) {
					return
				}
				binding.previousLocationName.text = this@DebugActivity.previousLocationName
				binding.previousLocationAccuracy.text = this@DebugActivity.getString(R.string.previous_location_accuracy, Locations.previousLocation!!.accuracy)
				binding.previousLocationAltitude.text = this@DebugActivity.getString(R.string.previous_location_altitude, Locations.previousLocation!!.altitude)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					binding.previousLocationAltitudeAccuracy.text = this@DebugActivity.getString(R.string.previous_location_altitude_accuracy, Locations.previousLocation!!.verticalAccuracyMeters)
				} else {
					binding.previousLocationAltitudeAccuracy.text = this@DebugActivity.getString(R.string.previous_location_altitude_accuracy, 0)
				}

				binding.previousLocationLatitude.text = this@DebugActivity.getString(R.string.previous_location_latitude, Locations.previousLocation!!.latitude)
				binding.previousLocationLongitude.text = this@DebugActivity.getString(R.string.previous_location_longitude, Locations.previousLocation!!.longitude)
				binding.previousLocationSpeed.text = this@DebugActivity.getString(R.string.previous_location_speed, Locations.previousLocation!!.speed)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					binding.previousLocationSpeedAccuracy.text = this@DebugActivity.getString(R.string.previous_location_speed_accuracy, Locations.previousLocation!!.speedAccuracyMetersPerSecond)
				} else {
					binding.previousLocationSpeedAccuracy.text = this@DebugActivity.getString(R.string.previous_location_speed_accuracy, 0)
				}
			}
		}

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.debug_map) as SupportMapFragment
		this.lifecycleScope.launchWhenCreated {
			this@DebugActivity.viewModel.mapCoroutine(mapFragment, this@DebugActivity)
		}

		// Add listener for map for a location change.
		if (this.locationChangeCallback != null) {
			if (!Locations.visibleLocationUpdates.contains(this.locationChangeCallback!!)) {
				Locations.visibleLocationUpdates.add(this.locationChangeCallback!!)
			}
		}
	}

	override fun onDestroy() {
		super.onDestroy()

		// Remove callback from locations.
		Locations.visibleLocationUpdates.remove(this.locationChangeCallback)
		this.locationChangeCallback = null

		if (this.viewModel.locationMarker != null) {
			this.viewModel.locationMarker!!.remove()
			this.viewModel.locationMarker = null
		}
		this.viewModel.map = null
	}

}