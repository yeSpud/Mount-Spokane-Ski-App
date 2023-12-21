package com.mtspokane.skiapp.activities

import android.Manifest
import android.annotation.SuppressLint
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
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addMarker
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databinding.ActivityMapsBinding
import com.mtspokane.skiapp.mapItem.MapItem
import com.mtspokane.skiapp.mapItem.MapMarker
import com.mtspokane.skiapp.mapItem.PolylineMapItem
import com.mtspokane.skiapp.maphandlers.MapHandler
import com.mtspokane.skiapp.maphandlers.CustomDialogEntry
import com.orhanobut.dialogplus.DialogPlus
import kotlinx.coroutines.*

class MapsActivity : FragmentActivity(), SkierLocationService.ServiceCallbacks {

	private lateinit var map: Map
	private var isMapSetup = false

	private var locationChangeCallback: InAppLocations.VisibleLocationUpdate? = null

	private var skierLocationService: SkierLocationService? = null
	private var bound = false

	// Boolean used to determine if the user's precise location is enabled (and therefore accessible).
	var locationEnabled = false
	private set

	private lateinit var optionsView: DialogPlus

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

		locationChangeCallback = object : InAppLocations.VisibleLocationUpdate {
			override fun updateLocation(locationString: String) {

				if (InAppLocations.currentLocation == null) {
					return
				}

				map.updateMarkerLocation(InAppLocations.currentLocation!!)
			}
		}
	}

	override fun onDestroy() {
		Log.v("MapsActivity", "onDestroy has been called!")
		super.onDestroy()

		// Remove callback from locations.
		InAppLocations.visibleLocationUpdates.remove(this.locationChangeCallback)
		locationChangeCallback = null

		map.destroy()
	}

	private val serviceConnection = object : ServiceConnection {

		override fun onServiceConnected(name: ComponentName?, service: IBinder) {
			val binder = service as SkierLocationService.LocalBinder
			skierLocationService = binder.getService()
			bound = true
			skierLocationService!!.setCallbacks(this@MapsActivity)
		}

		override fun onServiceDisconnected(name: ComponentName?) {
			bound = false
		}
	}

	override fun onStart() {
		super.onStart()
		val intent = Intent(this, SkierLocationService::class.java)
		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
	}

	override fun onStop() {
		super.onStop()
		if (bound) {
			skierLocationService!!.setCallbacks(null)
			unbindService(serviceConnection)
			bound = false
		}
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

	@SuppressLint("MissingPermission")
	fun launchLocationService() {

		val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

			val serviceIntent = Intent(this, SkierLocationService::class.java)

			// Check if the service has already been started and is running...
			val activityManager: ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
			for (runningServices in activityManager.getRunningServices(Int.MAX_VALUE)) {
				if (SkierLocationService::class.java.name == runningServices.service.className) {
					if (runningServices.foreground) {
						return
					}
				}
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				startForegroundService(serviceIntent)
			} else {
				startService(serviceIntent)
			}

			// Add listener for map for a location change.
			if (this.locationChangeCallback != null) {
				if (!InAppLocations.visibleLocationUpdates.contains(locationChangeCallback!!)) {
					InAppLocations.visibleLocationUpdates.add(locationChangeCallback!!)
				}
			}
		}
	}

	override fun isInBounds(location: Location): Boolean {
		return map.skiAreaBounds.locationInsidePoints(location)
	}

	override fun getOnLocation(location: Location): MapMarker? {

		var mapMarker = InAppLocations.checkIfIOnChairlift(map.startingChairliftTerminals,
			map.endingChairliftTerminals)
		if (mapMarker != null) {
			return mapMarker
		}

		mapMarker = InAppLocations.checkIfOnRun(map.easyRunsBounds, map.moderateRunsBounds,
			map.difficultRunsBounds)
		if (mapMarker != null) {
			return mapMarker
		}

		return null
	}

	override fun getInLocation(location: Location): MapMarker? {
		return InAppLocations.checkIfOnOther(map.otherBounds)
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

		fun updateMarkerLocation(location: Location) {

			if (locationMarker == null) {
				locationMarker = googleMap.addMarker {
					position(LatLng(location.latitude, location.longitude))
					title(resources.getString(R.string.your_location))
				}
			} else {

				// Otherwise just update the LatLng location.
				this.locationMarker!!.position = LatLng(location.latitude, location.longitude)
			}
		}
	}

	private inner class OptionsDialog : BaseAdapter() {

		private var showChairliftImage: CustomDialogEntry? = null

		private var showEasyRunsImage: CustomDialogEntry? = null

		private var showModerateRunsImage: CustomDialogEntry? = null

		private var showDifficultRunsImage: CustomDialogEntry? = null

		private var showNightRunsImage: CustomDialogEntry? = null

		private var launchActivitySummaryImage: CustomDialogEntry? = null
		override fun getCount(): Int {
			return 1
		}

		override fun getItem(position: Int): Any {
			return position // Todo properly implement me?
		}

		override fun getItemId(position: Int): Long {
			return position.toLong() // Todo properly implement me?
		}

		override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

			val view: View = convertView
					?: layoutInflater.inflate(R.layout.main_options, parent, false)

			if (showChairliftImage == null) {
				val chairliftImage: CustomDialogEntry = view.findViewById(R.id.show_chairlift)
				chairliftImage.setOnClickListener(CustomOnClickListener(map.chairliftPolylines))
				chairliftImage.setGlowing(map.chairliftPolylines[0].polylines[0].isVisible)
				showChairliftImage = chairliftImage
			}

			if (showEasyRunsImage == null) {
				val easyRunImage: CustomDialogEntry = view.findViewById(R.id.show_easy_runs)
				easyRunImage.setOnClickListener(CustomOnClickListener(map.easyRunsPolylines))
				easyRunImage.setGlowing(map.easyRunsPolylines[0].polylines[0].isVisible)
				showEasyRunsImage = easyRunImage
			}

			if (showModerateRunsImage == null) {
				val moderateRunImage: CustomDialogEntry = view.findViewById(R.id.show_moderate_runs)
				moderateRunImage.setOnClickListener(CustomOnClickListener(map.moderateRunsPolylines))
				moderateRunImage.setGlowing(map.moderateRunsPolylines[0].polylines[0].isVisible)
				showModerateRunsImage = moderateRunImage
			}

			if (showDifficultRunsImage == null) {
				val difficultRunImage: CustomDialogEntry = view.findViewById(R.id.show_difficult_runs)
				difficultRunImage.setOnClickListener(CustomOnClickListener(map.difficultRunsPolylines))
				difficultRunImage.setGlowing(map.difficultRunsPolylines[0].polylines[0].isVisible)
				showDifficultRunsImage = difficultRunImage
			}

			if (showNightRunsImage == null) {
				val nightRunImage: CustomDialogEntry = view.findViewById(R.id.show_night_runs)
				nightRunImage.setOnClickListener {
					if (it == null || it !is CustomDialogEntry) {
						return@setOnClickListener
					}

					with(map) {

						isNightOnly = !isNightOnly

						for (chairliftPolyline in chairliftPolylines) {
							chairliftPolyline.togglePolyLineVisibility(chairliftPolyline.defaultVisibility, isNightOnly)
						}

						for (easyRunPolyline in easyRunsPolylines) {
							easyRunPolyline.togglePolyLineVisibility(easyRunPolyline.defaultVisibility, isNightOnly)
						}

						for (moderateRunPolyline in moderateRunsPolylines) {
							moderateRunPolyline.togglePolyLineVisibility(moderateRunPolyline.defaultVisibility, isNightOnly)
						}

						for (difficultRunPolyline in difficultRunsPolylines) {
							difficultRunPolyline.togglePolyLineVisibility(difficultRunPolyline.defaultVisibility, isNightOnly)
						}

						it.setGlowing(isNightOnly)
					}
				}

				nightRunImage.setGlowing(map.isNightOnly)
				showNightRunsImage = nightRunImage
			}

			if (launchActivitySummaryImage == null) {
				val activitySummaryImage: CustomDialogEntry = view.findViewById(R.id.launch_activity_summary)
				activitySummaryImage.setOnClickListener {
					optionsView.dismiss()
					startActivity(Intent(this@MapsActivity, ActivitySummary::class.java))
				}
				launchActivitySummaryImage = activitySummaryImage
			}

			return view
		}
	}

	private inner class CustomOnClickListener(val polylineMapItems: List<PolylineMapItem>): View.OnClickListener {

		override fun onClick(v: View?) {

			if (v == null || v !is CustomDialogEntry) {
				return
			}

			for (polylineMapItem in polylineMapItems) {
				polylineMapItem.togglePolyLineVisibility(!polylineMapItem.defaultVisibility, map.isNightOnly)
			}

			v.setGlowing(polylineMapItems[0].polylines[0].isVisible)
		}
	}
}