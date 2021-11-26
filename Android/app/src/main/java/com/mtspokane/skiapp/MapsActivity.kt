package com.mtspokane.skiapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.Build
import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.SupportMapFragment
import com.mtspokane.skiapp.databinding.ActivityMapsBinding
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class MapsActivity : FragmentActivity() {

	private var mapHandler: MapHandler? = null

	private var inAppLocationHandler: InAppSkierLocation? = null

	lateinit var locationPopupDialog: AlertDialog

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Setup data binding.
		val binding = ActivityMapsBinding.inflate(layoutInflater)
		setContentView(binding.root)

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
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {

		if (this.mapHandler != null) {

			val checked = !item.isChecked

			item.isChecked = checked

			when (item.itemId) {
				R.id.chairlift -> this.mapHandler!!.chairlifts.forEach{it.value.togglePolyLineVisibility(checked)}
				R.id.easy -> this.mapHandler!!.easyRuns.forEach{it.value.togglePolyLineVisibility(checked)}
				R.id.moderate -> this.mapHandler!!.moderateRuns.forEach{it.value.togglePolyLineVisibility(checked)}
				R.id.difficult -> this.mapHandler!!.difficultRuns.forEach{it.value.togglePolyLineVisibility(checked)}
				R.id.night -> {
					this.mapHandler!!.chairlifts.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
					this.mapHandler!!.easyRuns.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
					this.mapHandler!!.moderateRuns.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
					this.mapHandler!!.difficultRuns.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
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

		// Check if the location service has already been started.
		if (!SkierLocationService.checkIfRunning(this)) {

			val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				Notification.Builder(this, NotificationChannel("Location", "Location", NotificationManager.IMPORTANCE_DEFAULT).id)
					.setSmallIcon(R.drawable.icon_fg)
					.setShowWhen(false)
					.build()
			} else {
				Notification()
			}

			val service = SkierLocationService()
			val serviceIntent = Intent()
			//serviceIntent.putExtra("ski area bounds", this.mapHandler!!.skiAreaBounds.)
			serviceIntent.putExtra("other", this.mapHandler!!.other)
			serviceIntent.putExtra("chairlifts", this.mapHandler!!.chairlifts.values.toTypedArray())


			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				service.startForeground(SkierLocationService.foregroundId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
			} else {
				service.startForeground(SkierLocationService.foregroundId, notification)
			}
		}
	}

	companion object {
		const val permissionValue = 29500
	}
}