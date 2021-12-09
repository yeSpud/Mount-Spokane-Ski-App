package com.mtspokane.skiapp.mapactivity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
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
import com.mtspokane.skiapp.skierlocation.InAppSkierLocation
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class MapsActivity : FragmentActivity() {

	// Handler for managing the map object.
	private var mapHandler: MapHandler? = null

	// Handler for managing the users location while within the app.
	private var inAppLocationHandler: InAppSkierLocation? = null

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

		// Setup the map handler and the in app skier location handler.
		this.mapHandler = MapHandler(this)
		this.inAppLocationHandler = InAppSkierLocation(this.mapHandler!!, this)

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
		mapFragment!!.getMapAsync(this.mapHandler!!)
	}

	override fun onDestroy() {
		Log.v("MapsActivity", "onDestroy has been called!")
		super.onDestroy()

		// Reset the in app location handler.
		this.inAppLocationHandler!!.destroy()
		this.inAppLocationHandler = null

		// Reset the map handler.
		this.mapHandler!!.destroy()
		this.mapHandler = null
		
		// Remove UI items from MtSpokaneMapItems.
		MtSpokaneMapItems.destroyUIItems()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		this.menuInflater.inflate(R.menu.maps_menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {

		// Only execute the following checks if the map handler is not null.
		if (this.mapHandler != null) {

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
						this@MapsActivity.mapHandler!!.setupLocation()
					}.start()
				}
			}
		}
	}

	@SuppressLint("MissingPermission")
	fun showLocation() {

		val locationManager: LocationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
				2F, this.inAppLocationHandler!!)
		}
	}

	companion object {
		const val permissionValue = 29500
	}
}