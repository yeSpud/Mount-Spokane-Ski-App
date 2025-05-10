package com.mtspokane.skiapp

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.GridView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
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
import xyz.thespud.skimap.activities.LiveMapOptionsDialog
import xyz.thespud.skimap.activities.MapOptionItem
import xyz.thespud.skimap.services.SkiingNotification
import java.util.Date

class MapsActivity : FragmentActivity() {

	private lateinit var map: Map

	private lateinit var database: Database
	private lateinit var databaseDao: SkiingActivityDao
	private lateinit var skiingDate: SkiingDate

	private lateinit var optionsView: DialogPlus

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)

		// Setup data binding.
		val binding = ActivityMapsBinding.inflate(layoutInflater)
		setContentView(binding.root)

		// Fix edge to edge behavior
		var lpad = 0
		var tpad = 0
		var rpad = 0
		var bpad = 0
		ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			lpad = systemBars.left
			tpad = systemBars.top
			rpad = systemBars.right
			bpad = systemBars.bottom

			val params: ViewGroup.MarginLayoutParams = binding.optionsButton.getLayoutParams() as ViewGroup.MarginLayoutParams
			params.setMargins(params.leftMargin + lpad, params.topMargin + tpad,
				params.rightMargin + rpad, params.bottomMargin + bpad)
			binding.optionsButton.layoutParams = params

			insets
		}

		val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
				as NotificationManager
		notificationManager.cancel(SkiingNotification.ACTIVITY_SUMMARY_ID)

		// Be sure to hide the action bar.
		if (actionBar != null) {
			actionBar!!.setDisplayShowTitleEnabled(false)
			actionBar!!.hide()
		}

		database = Room.databaseBuilder(this, Database::class.java, Database.NAME).allowMainThreadQueries().build()
		databaseDao = database.skiingActivityDao()

		val todaysDate: String = Database.getTodaysDate()
		val longDate: String = Database.getLongDateFromLong(Date().time)

		// Set the skiing date to today's date once its in the database.
		// If its not in the database yet try adding it.
		var skiingDateAndActivities = databaseDao.getSkiingDateWithActivitiesByShortDate(todaysDate)
		while (skiingDateAndActivities == null) {
			databaseDao.addSkiingDate(LongAndShortDate(longDate, todaysDate))
			skiingDateAndActivities = databaseDao.getSkiingDateWithActivitiesByShortDate(todaysDate)
		}
		skiingDate = skiingDateAndActivities.skiingDate

		// Setup the map handler.
		map = Map(lpad, tpad, rpad, bpad)

		optionsView = DialogPlus.newDialog(this)
			.setAdapter(OptionsDialog())
			.setExpanded(false)
			.setContentBackgroundResource(R.color.dark_blue)
			.create()

		binding.optionsButton.setOnClickListener {
			optionsView.holderView.setPadding(lpad, 0, rpad, bpad)
			optionsView.show()
		}

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
		mapFragment.getMapAsync(map)
	}

	override fun onDestroy() {
		Log.v("MapsActivity", "onDestroy has been called!")
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

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
	                                        grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		when (requestCode) {

			// If request is cancelled, the result arrays are empty.
			LiveMapActivity.permissionValue -> {
				if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					map.launchLocationService()
				}
			}
		}
	}

	private inner class Map(leftPadding: Int, topPadding: Int, rightPadding: Int, bottomPadding: Int):
		LiveMapActivity(
			this@MapsActivity,
			leftPadding, topPadding, rightPadding, bottomPadding,
			CameraPosition.Builder().target(LatLng(47.92517834073426,
				-117.10480503737926)).tilt(45F).bearing(317.50552F).zoom(14.414046F).build(),
			LatLngBounds(LatLng(47.912728, -117.133402), LatLng(47.943674, -117.092470)),
			R.raw.lifts, R.raw.easy, R.raw.moderate, R.raw.difficult, null,
			R.raw.starting_lift_polygons, R.raw.ending_lift_polygons, R.raw.easy_polygons,
			R.raw.moderate_polygons, R.raw.difficult_polygons, null, R.raw.other) {

			override fun getOtherIcon(name: String): Int? {
				Log.d("getOtherIcon", "Getting icon for $name")
				val icon: Int? = when (name) {
					"Lodge 1" -> R.drawable.ic_lodge
					"Lodge 2" -> R.drawable.ic_lodge
					"Yurt" -> R.drawable.ic_yurt
					"Vista House" -> R.drawable.ic_vista_house
					"Ski Patrol Building" -> R.drawable.ic_ski_patrol_icon
					"Lodge 1 Parking Lot" -> R.drawable.ic_parking
					"Lodge 2 Parking Lot" -> R.drawable.ic_parking
					"Tubing Area" -> R.drawable.ic_missing // Todo Tubing area icon
					"Ski School" -> R.drawable.ic_ski_school
					"Learning Area" -> R.drawable.ic_ski_school
					"Top of 1, 6", "Top of 2", "Top of 3", "Top of 4", "Top of 5" -> R.drawable.ic_chairlift
					else -> {
						Log.w("getOtherIcon", "$name does not have an icon")
						null
					}
				}

				return icon
			}

		override fun onLocationUpdated(location: Location) {
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
				skiingDate.id
			)

			databaseDao.addSkiingActivity(skiingActivity)
		}
	}

	private inner class OptionsDialog : LiveMapOptionsDialog(map) {

		private var launchActivitySummaryImage: MapOptionItem? = null

		override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
			val view = super.getView(position, convertView, parent)

			if (view !is GridLayout) {
				return view
			}

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