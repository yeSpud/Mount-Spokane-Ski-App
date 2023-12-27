package com.mtspokane.skiapp.activities

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addMarker
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummary
import com.mtspokane.skiapp.mapItem.SkiingActivity
import com.mtspokane.skiapp.databinding.ActivityMapsBinding
import com.mtspokane.skiapp.mapItem.Locations
import com.mtspokane.skiapp.mapItem.MapMarker
import com.mtspokane.skiapp.maphandlers.MapHandler
import com.mtspokane.skiapp.maphandlers.MapOptionItem
import com.mtspokane.skiapp.maphandlers.MapOptionsDialog
import com.orhanobut.dialogplus.DialogPlus

class MapsActivity : FragmentActivity(), SkierLocationService.ServiceCallbacks {

	private lateinit var map: Map
	private var isMapSetup = false

	private var skierLocationService: SkierLocationService? = null
	private var bound = false

	// Boolean used to determine if the user's precise location is enabled (and therefore accessible).
	var locationEnabled = false
	private set

	private lateinit var optionsView: DialogPlus

	private val serviceConnection = object : ServiceConnection {

		override fun onServiceConnected(name: ComponentName?, service: IBinder) {
			val binder = service as SkierLocationService.LocalBinder
			skierLocationService = binder.getService()
			bound = true
			skierLocationService!!.setCallbacks(this@MapsActivity)
		}

		override fun onServiceDisconnected(name: ComponentName?) {
			skierLocationService!!.setCallbacks(null)
			unbindService(this)
			skierLocationService = null
			bound = false
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Setup data binding.
		val binding = ActivityMapsBinding.inflate(layoutInflater)
		setContentView(binding.root)

		val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
				as NotificationManager
		notificationManager.cancel(SkierLocationService.ACTIVITY_SUMMARY_ID)

		// Determine if the user has enabled location permissions.
		locationEnabled = checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(),
			Process.myUid()) == PackageManager.PERMISSION_GRANTED

		// Be sure to hide the action bar.
		if (actionBar != null) {
			actionBar!!.setDisplayShowTitleEnabled(false)
			actionBar!!.hide()
		}

		// Setup the map handler.
		map = Map()

		optionsView = DialogPlus.newDialog(this).setAdapter(OptionsDialog()).setExpanded(false).create()

		binding.optionsButton.setOnClickListener {
			optionsView.show()
		}

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
		mapFragment.getMapAsync(map)
	}

	override fun onDestroy() {
		Log.v("MapsActivity", "onDestroy has been called!")
		super.onDestroy()

		if (bound) {
			skierLocationService!!.setCallbacks(null)
			unbindService(serviceConnection)
			bound = false
		}

		map.destroy()
	}

	override fun onResume() {
		super.onResume()

		if (isMapSetup) {
			launchLocationService()
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
	                                        grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		when (requestCode) {

			// If request is cancelled, the result arrays are empty.
			permissionValue -> {
				if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					launchLocationService()
				}
			}
		}
	}

	fun launchLocationService() {

		val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

			val serviceIntent = Intent(this, SkierLocationService::class.java)

			// Check if the service has already been started and is running...
			val activityManager: ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

			// As of Build.VERSION_CODES.O, this method is no longer available to third party applications.
			// For backwards compatibility, it will still return the caller's own service.
			// Which is exactly what we want.
			@Suppress("DEPRECATION")
			for (runningServices in activityManager.getRunningServices(Int.MAX_VALUE)) {
				if (SkierLocationService::class.java.name == runningServices.service.className) {
					if (runningServices.foreground) {
						return
					}
				}
			}

			bindService(serviceIntent, serviceConnection, Context.BIND_NOT_FOREGROUND)
			startService(serviceIntent)
		}
	}

	override fun isInBounds(location: Location): Boolean {
		if (map.skiAreaBounds != null) {
			return map.skiAreaBounds!!.locationInsidePoints(location)
		}
		return false
	}

	override fun getOnLocation(location: Location): MapMarker? {

		var mapMarker = Locations.checkIfIOnChairlift(map.startingChairliftTerminals,
			map.endingChairliftTerminals)
		if (mapMarker != null) {
			return mapMarker
		}

		mapMarker = Locations.checkIfOnRun(map.easyRunsBounds, map.moderateRunsBounds,
			map.difficultRunsBounds)
		if (mapMarker != null) {
			return mapMarker
		}

		return null
	}

	override fun getInLocation(location: Location): MapMarker? {
		return Locations.checkIfOnOther(map.otherBounds)
	}

	override fun updateMapMarker(locationString: String) {
		if (Locations.currentLocation != null) {
			map.updateMarkerLocation(Locations.currentLocation!!)
		}
	}

	companion object {
		const val permissionValue = 29500
	}

	private inner class Map : MapHandler(this@MapsActivity) {

		private var locationMarker: Marker? = null

		override val additionalCallback: OnMapReadyCallback = OnMapReadyCallback {

			// Request location permission, so that we can get the location of the device.
			// The result of the permission request is handled by a callback, onRequestPermissionsResult.
			// If this permission isn't granted then that's fine too.
			Log.v("onMapReady", "Checking location permissions...")
			if (locationEnabled) {
				Log.v("onMapReady", "Location tracking enabled")
				launchLocationService()
			} else {

				// Setup the location popup dialog.
				val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this@MapsActivity)
				alertDialogBuilder.setTitle(R.string.alert_title)
				alertDialogBuilder.setMessage(R.string.alert_message)
				alertDialogBuilder.setPositiveButton(R.string.alert_ok) { _, _ ->
					ActivityCompat.requestPermissions(this@MapsActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), permissionValue)
				}

				// Show the info popup about location.
				alertDialogBuilder.create().show()
			}

			isMapSetup = true
		}

		override fun destroy() {

			if (locationMarker != null) {
				Log.v("MainMap", "Removing location marker")
				locationMarker!!.remove()
				locationMarker = null
			}

			super.destroy()
		}

		fun updateMarkerLocation(location: SkiingActivity) {

			if (locationMarker == null) {
				locationMarker = googleMap.addMarker {
					position(LatLng(location.latitude, location.longitude))
					title(resources.getString(R.string.your_location))
				}
			} else {

				// Otherwise just update the LatLng location.
				locationMarker!!.position = LatLng(location.latitude, location.longitude)
			}
		}
	}

	private inner class OptionsDialog : MapOptionsDialog(layoutInflater, R.layout.main_options, map) {

		private var launchActivitySummaryImage: MapOptionItem? = null

		override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
			val view = super.getView(position, convertView, parent)

			if (launchActivitySummaryImage != null) {
				return view
			}

			val activitySummaryImage: MapOptionItem? = view.findViewById(R.id.launch_activity_summary)
			if (activitySummaryImage == null) {
				Log.w("getView", "Unable to find activity summary launcher")
				return view
			}

			activitySummaryImage.setOnClickListener {
				optionsView.dismiss()
				startActivity(Intent(this@MapsActivity, ActivitySummary::class.java))
			}
			launchActivitySummaryImage = activitySummaryImage

			return view
		}
	}
}