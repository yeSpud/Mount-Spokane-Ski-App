package com.mtspokane.skiapp.debugview

import android.location.Location
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
import com.mtspokane.skiapp.skierlocation.VisibleLocationUpdate

class DebugActivity : FragmentActivity() {

	private var locationChangeCallback: VisibleLocationUpdate? = null

	lateinit var viewModel: DebugViewModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		this.viewModel = ViewModelProvider(this).get(DebugViewModel::class.java)

		val binding = ActivityDebugBinding.inflate(this.layoutInflater)
		this.setContentView(binding.root)

		this.actionBar!!.setDisplayShowTitleEnabled(true)


		this.locationChangeCallback = object : VisibleLocationUpdate {
			override fun updateLocation(location: Location, locationString: String) {

				if (this@DebugActivity.viewModel.map != null) {

					// If the marker hasn't been added to the map create a new one.
					if (this@DebugActivity.viewModel.locationMarker == null) {
						this@DebugActivity.viewModel.locationMarker = this@DebugActivity.viewModel.map!!.addMarker {
							position(LatLng(location.latitude, location.longitude))
							title(this@DebugActivity.resources.getString(R.string.your_location))
						}
					} else {

						// Otherwise just update the LatLng location.
						this@DebugActivity.viewModel.locationMarker!!.position = LatLng(location.latitude, location.longitude)
					}
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