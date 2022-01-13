package com.mtspokane.skiapp.activities

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.ktx.addMarker
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databinding.ActivityDebugBinding
import com.mtspokane.skiapp.maphandlers.DebugMap
import com.mtspokane.skiapp.skierlocation.Locations

class DebugActivity : FragmentActivity() {

	private var mapHandler: DebugMap? = null

	private var locationChangeCallback: Locations.VisibleLocationUpdate? = null

	private var previousLocationName: CharSequence = ""

	private lateinit var binding: ActivityDebugBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		this.binding = ActivityDebugBinding.inflate(this.layoutInflater)
		this.setContentView(this.binding.root)

		this.actionBar!!.setDisplayShowTitleEnabled(true)

		this.locationChangeCallback = object : Locations.VisibleLocationUpdate {
			override fun updateLocation(locationString: String) {
				this@DebugActivity.updateText(locationString)
			}
		}

		this.mapHandler = DebugMap(this)

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.debug_map) as SupportMapFragment
		mapFragment.getMapAsync(this.mapHandler!!)

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
		this.binding.currentLocationName.text = this.getString(R.string.current_location_name, locationString)
		this.binding.currentLocationAccuracy.text = this.getString(R.string.current_location_accuracy, Locations.currentLocation!!.accuracy)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.binding.currentLocationAltitude.text = this.getString(R.string.current_location_altitude, Locations.currentLocation!!.altitude, Locations.currentLocation!!.verticalAccuracyMeters)
		} else {
			this.binding.currentLocationAltitude.text = this.getString(R.string.current_location_altitude, Locations.currentLocation!!.altitude, 0)
		}

		this.binding.currentLocationLatitude.text = this.getString(R.string.current_location_latitude, Locations.currentLocation!!.latitude)
		this.binding.currentLocationLongitude.text = this.getString(R.string.current_location_longitude, Locations.currentLocation!!.longitude)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.binding.currentLocationSpeed.text = this.getString(R.string.current_location_speed, Locations.currentLocation!!.speed, Locations.currentLocation!!.speedAccuracyMetersPerSecond)
		} else {
			this.binding.currentLocationSpeed.text = this.getString(R.string.current_location_speed, Locations.currentLocation!!.speed, 0)
		}

		if (this.mapHandler != null && this.mapHandler!!.map != null) {

			// If the marker hasn't been added to the map create a new one.
			if (this.mapHandler!!.locationMarker == null) {
				this.mapHandler!!.locationMarker = this.mapHandler!!.map!!.addMarker {
					position(LatLng(Locations.currentLocation!!.latitude, Locations.currentLocation!!.longitude))
					title(this@DebugActivity.getString(R.string.your_location))
				}
			} else {

				// Otherwise just update the LatLng location.
				this.mapHandler!!.locationMarker!!.position = LatLng(Locations.currentLocation!!.latitude,
					Locations.currentLocation!!.longitude)
			}
		}

		this.binding.altitudeConfidence.text = this.getString(R.string.altitude_confidence, Locations.altitudeConfidence.toString())
		this.binding.speedConfidence.text = this.getString(R.string.altitude_confidence, Locations.speedConfidence.toString())
		this.binding.verticalDirection.text = this.getString(R.string.vertical_direction, Locations.getVerticalDirection().name)
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.binding.previousLocationAltitude.text = this.getString(R.string.previous_location_altitude, Locations.previousLocation!!.altitude, Locations.previousLocation!!.verticalAccuracyMeters)
		} else {
			this.binding.previousLocationAltitude.text = this.getString(R.string.previous_location_altitude, Locations.previousLocation!!.altitude, 0)
		}

		this.binding.previousLocationLatitude.text = this.getString(R.string.previous_location_latitude, Locations.previousLocation!!.latitude)
		this.binding.previousLocationLongitude.text = this.getString(R.string.previous_location_longitude, Locations.previousLocation!!.longitude)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.binding.previousLocationSpeed.text = this.getString(R.string.previous_location_speed, Locations.previousLocation!!.speed, Locations.previousLocation!!.speedAccuracyMetersPerSecond)
		} else {
			this.binding.previousLocationSpeed.text = this.getString(R.string.previous_location_speed, Locations.previousLocation!!.speed, 0)
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

		if (this.mapHandler != null) {
			this.mapHandler!!.destroy()
			this.mapHandler = null
		}
	}

}