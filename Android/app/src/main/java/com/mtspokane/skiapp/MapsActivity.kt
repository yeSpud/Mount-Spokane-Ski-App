package com.mtspokane.skiapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteConstraintException
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.room.Room
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.mtspokane.skiapp.databinding.ActivityMapsBinding
import com.orhanobut.dialogplus.DialogPlus
import xyz.thespud.skimap.activities.LiveMapActivity
import xyz.thespud.skimap.dialogs.LiveMapOptionsDialog
import xyz.thespud.skimap.dialogs.MapOptionItem
import xyz.thespud.skimap.locationmanager.SkiAreaObjects
import xyz.thespud.skimap.services.SkierLocationService
import java.util.Date

class MapsActivity : FragmentActivity() {

	private lateinit var map: LiveMapActivity

	private lateinit var databaseDao: SkiingActivityDao
	private var skiingDate: SkiingDate? = null

	private lateinit var optionsView: DialogPlus

	private val startTrackingReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			Log.d("startTrackingReceiver", "Received broadcast to start tracking")
			val todaysDate: String = Database.getTodaysDate()
			val longDate: String = Database.getLongDateFromLong(Date().time)

			// Set the skiing date to today's date once its in the database.
			// If it's not in the database yet try adding it.
			var skiingDateAndActivities = databaseDao.getSkiingDateWithActivitiesByShortDate(todaysDate)
			while (skiingDateAndActivities == null) {
				databaseDao.addSkiingDate(LongAndShortDate(longDate, todaysDate))
				skiingDateAndActivities = databaseDao.getSkiingDateWithActivitiesByShortDate(todaysDate)
			}
			skiingDate = skiingDateAndActivities.skiingDate
		}
	}

	private val updateTrackingReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			val tag = "updateTrackingReceiver"
			Log.d(tag, "Received broadcast to update tracking")

			val location = map.locationManager.currentLocation
			if (location == null) {
				Log.w(tag, "Current locaiton is null")
				return
			}

			val dateId = skiingDate?.id
			if (dateId == null) {
				Log.w(tag, "Skiing date ID is null (was not created when tracking started?)")
				return
			}

			val skiingActivity = SkiingActivity(
				location.accuracy,
				location.altitude,
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
					location.mslAltitudeAccuracyMeters
				} else { null },
				location.latitude,
				location.longitude,
				location.speed,
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
					location.speedAccuracyMetersPerSecond
				} else { null },
				location.time,
				dateId
			)

			try {
				databaseDao.addSkiingActivity(skiingActivity)
			} catch (sqlError: SQLiteConstraintException) {
				Log.w(tag, "Unable to add skiing activity to database - time already exists?",
					sqlError)
			}
		}
	}

	private val stopTrackingReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			val tag = "stopTrackingReceiver"
			Log.d(tag, "Received broadcast to stop tracking")

			val dateId = skiingDate?.id
			if (dateId == null) {
				Log.w(tag, "Skiing date ID is null (was not created when tracking started?)")
				return
			}

			val todaysSkiingActivities = databaseDao.getActivitiesByDateId(dateId)
			if (todaysSkiingActivities.isEmpty()) { return }

			// Show activity summary notification
			val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
			val activitySummaryIntent = Intent(this@MapsActivity, ActivitySummary::class.java)
			activitySummaryIntent.putExtra(ActivitySummary.ACTIVITY_SUMMARY_LAUNCH_DATE, dateId)

			val pendingIntent = PendingIntent.getActivity(this@MapsActivity, 0,
				activitySummaryIntent, PendingIntent.FLAG_IMMUTABLE)

			val notification = NotificationCompat.Builder(this@MapsActivity, ActivitySummary.ACTIVITY_SUMMARY_CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_launcher_foreground)
				.setShowWhen(true)
				.setContentTitle(getString(R.string.activity_notification_text))
				.setContentIntent(pendingIntent)
				.build()
			notificationManager.notify(ActivitySummary.ACTIVITY_SUMMARY_ID, notification)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)

		// Setup data binding.
		val binding = ActivityMapsBinding.inflate(layoutInflater)
		setContentView(binding.root)

		// Fix edge to edge behavior
		ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			val lpad = systemBars.left
			val tpad = systemBars.top
			val rpad = systemBars.right
			val bpad = systemBars.bottom

			val params: ViewGroup.MarginLayoutParams = binding.optionsButton.getLayoutParams() as ViewGroup.MarginLayoutParams
			params.setMargins(params.leftMargin + lpad, params.topMargin + tpad,
				params.rightMargin + rpad, params.bottomMargin + bpad)
			binding.optionsButton.layoutParams = params

			binding.optionsButton.setOnClickListener {
				optionsView.holderView.setPadding(lpad, 0, rpad, bpad)
				optionsView.show()
			}

			insets
		}

		val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE)
				as NotificationManager
		val notificationChannel = NotificationChannel(ActivitySummary.ACTIVITY_SUMMARY_CHANNEL_ID,
			getString(R.string.activity_summary_title), NotificationManager.IMPORTANCE_DEFAULT)
		notificationManager.createNotificationChannel(notificationChannel)
		notificationManager.cancel(ActivitySummary.ACTIVITY_SUMMARY_ID)

		// Be sure to hide the action bar.
		if (actionBar != null) {
			actionBar!!.setDisplayShowTitleEnabled(false)
			actionBar!!.hide()
		}

		val cameraPosition = CameraPosition.Builder()
			.target(LatLng(47.92517834073426, -117.10480503737926))
			.tilt(45F)
			.bearing(317.50552F)
			.zoom(14.414046F).build()

		val cameraBounds = LatLngBounds(LatLng(47.912728, -117.133402), LatLng(47.943674, -117.092470))

		// Load the map polylines and polygons
		val skiAreaObjects = SkiAreaObjects(R.raw.bounds)
		skiAreaObjects.chairliftsPolylines = R.raw.lifts
		skiAreaObjects.greenRunPolylines = R.raw.easy
		skiAreaObjects.blueRunPolylines = R.raw.moderate
		skiAreaObjects.blackRunPolylines = R.raw.difficult
		skiAreaObjects.chairliftBounds = R.raw.lift_polygons
		skiAreaObjects.chairliftTerminals = R.raw.lift_terminals
		skiAreaObjects.greenRunBounds = R.raw.easy_polygons
		skiAreaObjects.blueRunBounds = R.raw.moderate_polygons
		skiAreaObjects.blackRunBounds = R.raw.difficult_polygons
		skiAreaObjects.other = R.raw.other

		// Setup the map handler.
		map = LiveMapActivity(this, binding.root, cameraPosition, cameraBounds, skiAreaObjects, Icons())

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
		mapFragment.getMapAsync(map)

		optionsView = DialogPlus.newDialog(this)
			.setAdapter(OptionsDialog())
			.setExpanded(false)
			.setContentBackgroundResource(R.color.dark_blue)
			.create()

		val database = Room.databaseBuilder(this, Database::class.java, Database.NAME)
			.allowMainThreadQueries().build()
		databaseDao = database.skiingActivityDao()

		// Create a database entry once tracking starts
		ContextCompat.registerReceiver(this, startTrackingReceiver,
			IntentFilter(SkierLocationService.START_TRACKING_BROADCAST),
			ContextCompat.RECEIVER_EXPORTED)

		// Add to database when tracking gets updated
		ContextCompat.registerReceiver(this, updateTrackingReceiver,
			IntentFilter(SkierLocationService.UPDATE_TRACKING_BROADCAST),
			ContextCompat.RECEIVER_EXPORTED)

		// Show a notification once tracking has stopped, and there is skiing data for today's date
		ContextCompat.registerReceiver(this, stopTrackingReceiver,
			IntentFilter(SkierLocationService.STOP_TRACKING_BROADCAST),
			ContextCompat.RECEIVER_EXPORTED)
	}

	override fun onDestroy() {
		Log.v("MapsActivity", "onDestroy has been called!")

		unregisterReceiver(startTrackingReceiver)
		unregisterReceiver(updateTrackingReceiver)
		unregisterReceiver(stopTrackingReceiver)

		map.destroy()
		super.onDestroy()
	}

	override fun onResume() {
		super.onResume()

		if (map.isMapSetup && !map.manuallyDisabled && !map.isTrackingLocation &&
			ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
			== PackageManager.PERMISSION_GRANTED) {
			map.launchLocationService()
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		when (requestCode) {

			// If request is canceled, the result arrays are empty.
			LiveMapActivity.permissionValue -> {
				if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					map.launchLocationService()
				}
			}
		}
	}

	private inner class OptionsDialog : LiveMapOptionsDialog(map) {

		private var launchActivitySummaryImage: MapOptionItem? = null

		override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
			val view = super.getView(position, convertView, parent)

			if (view !is GridLayout) { return view }

			// Hide double black runs button since Mt Spokane doesn't have any
			// FIXME
			/*
			val doubleBlackRuns: MapOptionItem? = view.findViewById(R.id.show_double_black_runs)
			if (doubleBlackRuns != null) {
				doubleBlackRuns.visibility = View.GONE
				// doubleBlackRuns.layoutParams = GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(0))
				view.removeView(doubleBlackRuns)
			}
			 */

			if (launchActivitySummaryImage == null) {
				val activitySummaryImage = MapOptionItem(this@MapsActivity,
					AppCompatResources.getDrawable(this@MapsActivity, R.drawable.summary_list)!!,
					AppCompatResources.getDrawable(this@MapsActivity, R.drawable.summary_list)!!,
					getString(R.string.activity_summary), getString(R.string.activity_summary))

				activitySummaryImage.setOnClickListener {
					optionsView.dismiss()
					startActivity(Intent(this@MapsActivity, ActivitySummary::class.java))
				}

				activitySummaryImage.gravity = Gravity.CENTER
				activitySummaryImage.weightSum = 1f

				view.addView(activitySummaryImage)
				launchActivitySummaryImage = activitySummaryImage
			}

			return view
		}
	}
}