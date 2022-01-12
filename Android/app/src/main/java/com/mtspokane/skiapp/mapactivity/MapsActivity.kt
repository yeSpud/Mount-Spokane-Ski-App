package com.mtspokane.skiapp.mapactivity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.SupportMapFragment
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activitysummary.ActivitySummary
import com.mtspokane.skiapp.databinding.ActivityMapsBinding
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.maphandlers.MainMap
import com.mtspokane.skiapp.skierlocation.Locations
import com.mtspokane.skiapp.skierlocation.SkierLocationService
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class MapsActivity : FragmentActivity() {

	private var map: MainMap? = null

	private var locationChangeCallback: Locations.VisibleLocationUpdate? = null

	// Boolean used to determine if only night runs are to be shown.
	private var nightRunsOnly = false

	// Boolean used to determine if the user's precise location is enabled (and therefore accessible).
	var locationEnabled = false
		private set

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Setup data binding.
		val binding = ActivityMapsBinding.inflate(this.layoutInflater)
		this.setContentView(binding.root)

		// Determine if the user has enabled location permissions.
		this.locationEnabled = this.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(),
			Process.myUid()) == PackageManager.PERMISSION_GRANTED

		// Be sure to show the action bar.
		this.actionBar!!.setDisplayShowTitleEnabled(true)

		// Setup the map handler.
		this.map = MainMap(this)

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
		mapFragment.getMapAsync(this.map!!)

		this.locationChangeCallback = object : Locations.VisibleLocationUpdate {
			override fun updateLocation(locationString: String) {

				if (Locations.currentLocation == null) {
					return
				}

				if (this@MapsActivity.map != null) {
					this@MapsActivity.map!!.updateMarkerLocation(Locations.currentLocation!!)
				}

				this@MapsActivity.actionBar!!.title = locationString
			}
		}
	}

	override fun onDestroy() {
		Log.v("MapsActivity", "onDestroy has been called!")
		super.onDestroy()

		// Remove callback from locations.
		Locations.visibleLocationUpdates.remove(this.locationChangeCallback)
		this.locationChangeCallback = null

		// Reset the map handler.
		if (this.map != null) {
			this.map!!.destroy()
			this.map = null
		}
		
		// Remove UI items from MtSpokaneMapItems.
		MtSpokaneMapItems.destroyUIItems()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		this.menuInflater.inflate(R.menu.maps_menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {

		// Only execute the following checks if the map handler is not null.
		if (this.map != null) {

			val checked = !item.isChecked
			item.isChecked = checked
			Log.d("onOptionsItemSelected", "Option selected: $checked")

			when (item.itemId) {
				R.id.chairlift -> MtSpokaneMapItems.chairlifts.forEach {
					it.togglePolyLineVisibility(checked, this.nightRunsOnly)
				}
				R.id.easy -> MtSpokaneMapItems.easyRuns.forEach {
					it.togglePolyLineVisibility(checked, this.nightRunsOnly)
				}
				R.id.moderate -> MtSpokaneMapItems.moderateRuns.forEach {
					it.togglePolyLineVisibility(checked, this.nightRunsOnly)
				}
				R.id.difficult -> MtSpokaneMapItems.difficultRuns.forEach {
					it.togglePolyLineVisibility(checked, this.nightRunsOnly)
				}
				R.id.night -> {
					MtSpokaneMapItems.chairlifts.forEach {
						it.togglePolyLineVisibility(it.defaultVisibility, checked)
					}
					MtSpokaneMapItems.easyRuns.forEach {
						it.togglePolyLineVisibility(it.defaultVisibility, checked)
					}
					MtSpokaneMapItems.moderateRuns.forEach {
						it.togglePolyLineVisibility(it.defaultVisibility, checked)
					}
					MtSpokaneMapItems.difficultRuns.forEach {
						it.togglePolyLineVisibility(it.defaultVisibility, checked)
					}
					this.nightRunsOnly = checked
				}
				R.id.activity_summary -> {
					val intent = Intent(this, ActivitySummary::class.java)
					this.startActivity(intent)
				}
			}
		}

		return super.onOptionsItemSelected(item)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
	                                        grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		when (requestCode) {

			// If request is cancelled, the result arrays are empty.
			permissionValue -> {
				if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					this.lifecycleScope.async(Dispatchers.Main, CoroutineStart.LAZY) {
						this@MapsActivity.map!!.setupLocation()
					}.start()
				}
			}
		}
	}

	@SuppressLint("MissingPermission")
	fun setupLocationService() {

		val locationManager: LocationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

			// Check if the location service has already been started.
			if (!SkierLocationService.checkIfRunning(this)) {

				val serviceIntent = Intent(this, SkierLocationService::class.java)

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					this.startForegroundService(serviceIntent)
				} else {
					this.startService(serviceIntent)
				}
			}

			// Add listener for map for a location change.
			if (this.locationChangeCallback != null) {
				if (!Locations.visibleLocationUpdates.contains(this.locationChangeCallback!!)) {
					Locations.visibleLocationUpdates.add(this.locationChangeCallback!!)
				}
			}
		}
	}

	companion object {
		const val permissionValue = 29500
	}
}