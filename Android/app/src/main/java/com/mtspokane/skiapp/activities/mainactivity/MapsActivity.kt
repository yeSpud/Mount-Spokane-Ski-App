package com.mtspokane.skiapp.activities.mainactivity

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
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
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummary
import com.mtspokane.skiapp.databinding.ActivityMapsBinding
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.maphandlers.MainMap
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MapsActivity : FragmentActivity() {

	private var map: MainMap? = null

	private var locationChangeCallback: InAppLocations.VisibleLocationUpdate? = null

	// Boolean used to determine if only night runs are to be shown.
	private var nightRunsOnly = false

	// Boolean used to determine if the user's precise location is enabled (and therefore accessible).
	var locationEnabled = false
		private set

	var launchingFromWithin = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Setup data binding.
		val binding = ActivityMapsBinding.inflate(this.layoutInflater)
		this.setContentView(binding.root)

		val notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE)
				as NotificationManager
		notificationManager.cancel(SkierLocationService.ACTIVITY_SUMMARY_ID)

		// Determine if the user has enabled location permissions.
		this.locationEnabled = this.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(),
			Process.myUid()) == PackageManager.PERMISSION_GRANTED

		// Be sure to hide the action bar.
		this.actionBar!!.setDisplayShowTitleEnabled(false)
		this.actionBar!!.hide()

		// Setup the map handler.
		this.map = MainMap(this)

		MtSpokaneMapItems.checkoutObject(this::class)

		binding.optionsButton.setOnClickListener {
			if (map != null) {
				this.map!!.mapOptionsDialog.show()
			}
		}

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
		mapFragment.getMapAsync(this.map!!)

		this.locationChangeCallback = object : InAppLocations.VisibleLocationUpdate {
			override fun updateLocation(locationString: String) {

				if (InAppLocations.currentLocation == null) {
					return
				}

				if (this@MapsActivity.map != null) {
					this@MapsActivity.map!!.updateMarkerLocation(InAppLocations.currentLocation!!)
				}

				//this@MapsActivity.actionBar!!.title = locationString
			}
		}
	}

	override fun onDestroy() {
		Log.v("MapsActivity", "onDestroy has been called!")
		super.onDestroy()

		// Remove callback from locations.
		InAppLocations.visibleLocationUpdates.remove(this.locationChangeCallback)
		this.locationChangeCallback = null

		// Reset the map handler.
		if (this.map != null) {
			this.map!!.destroy()
			this.map = null
		}
		
		// Remove UI items from MtSpokaneMapItems.
		MtSpokaneMapItems.destroyUIItems(this::class)
	}

	override fun onResume() {
		super.onResume()

		this.launchingFromWithin = false
	}

	override fun onPause() {
		super.onPause()

		if (!this.launchingFromWithin) {
			if (MtSpokaneMapItems.checkedOutCount <= 1) {
				this.finish()
			}
		}
	}

	/*
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		this.menuInflater.inflate(R.menu.maps_menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {

		// Only execute the following checks if the map handler is not null.
		if (this.map != null) {

			if (MtSpokaneMapItems.chairlifts == null) {
				lifecycleScope.launch { MtSpokaneMapItems.initializeChairliftsAsync(this@MapsActivity::class,
					this@MapsActivity.map!!).start() }
			}

			if (MtSpokaneMapItems.easyRuns == null) {
				lifecycleScope.launch { MtSpokaneMapItems.initializeEasyRunsAsync(this@MapsActivity::class,
					this@MapsActivity.map!!).start() }
			}

			if (MtSpokaneMapItems.moderateRuns == null) {
				lifecycleScope.launch { MtSpokaneMapItems.initializeModerateRunsAsync(this@MapsActivity::class,
					this@MapsActivity.map!!).start() }
			}

			if (MtSpokaneMapItems.difficultRuns == null) {
				lifecycleScope.launch { MtSpokaneMapItems.initializeDifficultRunsAsync(this@MapsActivity::class,
					this@MapsActivity.map!!).start() }
			}

			val checked = !item.isChecked
			item.isChecked = checked
			Log.d("onOptionsItemSelected", "Option selected: $checked")

			when (item.itemId) {
				R.id.chairlift -> MtSpokaneMapItems.chairlifts!!.forEach {
					it.togglePolyLineVisibility(checked, this.nightRunsOnly)
				}
				R.id.easy -> MtSpokaneMapItems.easyRuns!!.forEach {
					it.togglePolyLineVisibility(checked, this.nightRunsOnly)
				}
				R.id.moderate -> MtSpokaneMapItems.moderateRuns!!.forEach {
					it.togglePolyLineVisibility(checked, this.nightRunsOnly)
				}
				R.id.difficult -> MtSpokaneMapItems.difficultRuns!!.forEach {
					it.togglePolyLineVisibility(checked, this.nightRunsOnly)
				}
				R.id.night -> {
					MtSpokaneMapItems.chairlifts!!.forEach {
						it.togglePolyLineVisibility(it.defaultVisibility, checked)
					}
					MtSpokaneMapItems.easyRuns!!.forEach {
						it.togglePolyLineVisibility(it.defaultVisibility, checked)
					}
					MtSpokaneMapItems.moderateRuns!!.forEach {
						it.togglePolyLineVisibility(it.defaultVisibility, checked)
					}
					MtSpokaneMapItems.difficultRuns!!.forEach {
						it.togglePolyLineVisibility(it.defaultVisibility, checked)
					}
					this.nightRunsOnly = checked
				}
				R.id.activity_summary -> {
					this.launchingFromWithin = true
					val intent = Intent(this, ActivitySummary::class.java)
					this.startActivity(intent)
				}
			}
		}

		return super.onOptionsItemSelected(item)
	}
	 */

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

			val serviceIntent = Intent(this, SkierLocationService::class.java)

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				this.startForegroundService(serviceIntent)
			} else {
				this.startService(serviceIntent)
			}

			// Add listener for map for a location change.
			if (this.locationChangeCallback != null) {
				if (!InAppLocations.visibleLocationUpdates.contains(this.locationChangeCallback!!)) {
					InAppLocations.visibleLocationUpdates.add(this.locationChangeCallback!!)
				}
			}
		}
	}

	companion object {
		const val permissionValue = 29500
	}
}