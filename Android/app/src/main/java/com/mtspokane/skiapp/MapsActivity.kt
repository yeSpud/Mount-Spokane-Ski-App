package com.mtspokane.skiapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.SupportMapFragment
import com.mtspokane.skiapp.databinding.ActivityMapsBinding
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class MapsActivity : FragmentActivity() {

	private var mapHandler: MapHandler? = null

	private var inAppLocationHandler: InAppSkierLocation? = null

	lateinit var locationPopupDialog: AlertDialog

	private var nightRunsOnly = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Setup data binding.
		val binding = ActivityMapsBinding.inflate(this.layoutInflater)
		this.setContentView(binding.root)

		this.mapHandler = MapHandler(this)
		this.inAppLocationHandler = InAppSkierLocation(this.mapHandler!!, this)

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
		mapFragment!!.getMapAsync(this.mapHandler!!)

		// Setup the location popup dialog.
		val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
		alertDialogBuilder.setTitle(R.string.alert_title)
		alertDialogBuilder.setMessage(R.string.alert_message)
		alertDialogBuilder.setPositiveButton(R.string.alert_ok) { _, _ -> ActivityCompat.
		requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), permissionValue) }
		this.locationPopupDialog = alertDialogBuilder.create()
	}

	override fun onDestroy() {
		super.onDestroy()

		this.inAppLocationHandler!!.destroy()
		this.inAppLocationHandler = null
		this.mapHandler!!.destroy()
		this.mapHandler = null
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		this.menuInflater.inflate(R.menu.menu, menu)
		if (BuildConfig.DEBUG) {
			val activitySummary = menu.findItem(R.id.activity_summary)
			activitySummary.isVisible = true
			activitySummary.isEnabled = true
		}
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {

		if (this.mapHandler != null) {

			val checked = !item.isChecked
			item.isChecked = checked
			Log.d("onOptionsItemSelected", "Option selected: $checked")

			when (item.itemId) {
				R.id.chairlift -> MtSpokaneMapItems.chairlifts.forEach{it.togglePolyLineVisibility(checked, this.nightRunsOnly)}
				R.id.easy -> MtSpokaneMapItems.easyRuns.forEach{it.togglePolyLineVisibility(checked, this.nightRunsOnly)}
				R.id.moderate -> MtSpokaneMapItems.moderateRuns.forEach{it.togglePolyLineVisibility(checked, this.nightRunsOnly)}
				R.id.difficult -> MtSpokaneMapItems.difficultRuns.forEach{it.togglePolyLineVisibility(checked, this.nightRunsOnly)}
				R.id.night -> {
					MtSpokaneMapItems.chairlifts.forEach{ it.togglePolyLineVisibility(it.defaultVisibility, checked) }
					MtSpokaneMapItems.easyRuns.forEach{ it.togglePolyLineVisibility(it.defaultVisibility, checked) }
					MtSpokaneMapItems.moderateRuns.forEach{ it.togglePolyLineVisibility(it.defaultVisibility, checked) }
					MtSpokaneMapItems.difficultRuns.forEach{ it.togglePolyLineVisibility(it.defaultVisibility, checked) }
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

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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