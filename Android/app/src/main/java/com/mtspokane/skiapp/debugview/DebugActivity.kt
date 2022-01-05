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

	private lateinit var binding: ActivityDebugBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		this.viewModel = ViewModelProvider(this).get(DebugViewModel::class.java)

		this.binding = ActivityDebugBinding.inflate(this.layoutInflater)
		this.setContentView(this.binding.root)

		this.actionBar!!.setDisplayShowTitleEnabled(true)

		this.locationChangeCallback = object : Locations.VisibleLocationUpdate {
			override fun updateLocation(locationString: String) {

				this@DebugActivity.updateText(locationString)
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

	private fun updateText(locationString: String) {

		if (Locations.currentLocation == null) {
			return
		}

		this.previousLocationName = this.binding.currentLocationName.text
		this.binding.currentLocationName.text = locationString
		this.binding.currentLocationAccuracy.text = this.getString(R.string.current_location_accuracy, Locations.currentLocation!!.accuracy)
		this.binding.currentLocationAltitude.text = this.getString(R.string.current_location_altitude, Locations.currentLocation!!.altitude)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.binding.currentLocationAltitudeAccuracy.text = this.getString(R.string.current_location_altitude_accuracy, Locations.currentLocation!!.verticalAccuracyMeters)
		} else {
			this.binding.currentLocationAltitudeAccuracy.text = this.getString(R.string.current_location_altitude_accuracy, 0)
		}

		this.binding.currentLocationLatitude.text = this.getString(R.string.current_location_latitude, Locations.currentLocation!!.latitude)
		this.binding.currentLocationLongitude.text = this.getString(R.string.current_location_longitude, Locations.currentLocation!!.longitude)
		this.binding.currentLocationSpeed.text = this.getString(R.string.current_location_speed, Locations.currentLocation!!.speed)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.binding.currentLocationSpeedAccuracy.text = this.getString(R.string.current_location_speed_accuracy, Locations.currentLocation!!.speedAccuracyMetersPerSecond)
		} else {
			this.binding.currentLocationSpeedAccuracy.text = this.getString(R.string.current_location_speed_accuracy, 0)
		}

		if (this.viewModel.map != null) {

			// If the marker hasn't been added to the map create a new one.
			if (this.viewModel.locationMarker == null) {
				this.viewModel.locationMarker = this.viewModel.map!!.addMarker {
					position(LatLng(Locations.currentLocation!!.latitude, Locations.currentLocation!!.longitude))
					title(this@DebugActivity.getString(R.string.your_location))
				}
			} else {

				// Otherwise just update the LatLng location.
				this.viewModel.locationMarker!!.position = LatLng(Locations.currentLocation!!.latitude, Locations.currentLocation!!.longitude)
			}
		}

		this.binding.verticalDirection.text = this.getString(R.string.vertical_direction, Locations.getVerticalDirection().name)
		this.binding.chairliftConfidence.text = this.getString(R.string.chairlift_confidence, Locations.chairliftConfidence, Locations.numberOfChairliftChecks)
		if (Locations.mostLikelyChairlift != null) {
			this.binding.chairlift.text = this.getString(R.string.most_likely_chairlift, Locations.mostLikelyChairlift!!.name)
		} else {
			binding.chairlift.text = this.getString(R.string.most_likely_chairlift, "null")
		}

		if (Locations.previousLocation == null) {
			return
		}
		this.binding.previousLocationName.text = this.previousLocationName
		this.binding.previousLocationAccuracy.text = this.getString(R.string.previous_location_accuracy, Locations.previousLocation!!.accuracy)
		this.binding.previousLocationAltitude.text = this.getString(R.string.previous_location_altitude, Locations.previousLocation!!.altitude)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.binding.previousLocationAltitudeAccuracy.text = this.getString(R.string.previous_location_altitude_accuracy, Locations.previousLocation!!.verticalAccuracyMeters)
		} else {
			this.binding.previousLocationAltitudeAccuracy.text = this.getString(R.string.previous_location_altitude_accuracy, 0)
		}

		this.binding.previousLocationLatitude.text = this.getString(R.string.previous_location_latitude, Locations.previousLocation!!.latitude)
		this.binding.previousLocationLongitude.text = this.getString(R.string.previous_location_longitude, Locations.previousLocation!!.longitude)
		this.binding.previousLocationSpeed.text = this.getString(R.string.previous_location_speed, Locations.previousLocation!!.speed)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.binding.previousLocationSpeedAccuracy.text = this.getString(R.string.previous_location_speed_accuracy, Locations.previousLocation!!.speedAccuracyMetersPerSecond)
		} else {
			this.binding.previousLocationSpeedAccuracy.text = this.getString(R.string.previous_location_speed_accuracy, 0)
		}
	}

	override fun onResume() {
		super.onResume()

		this.updateText("null")
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