package com.mtspokane.skiapp.skierlocation

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activitysummary.ActivitySummary
import com.mtspokane.skiapp.activitysummary.SkiingActivity
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.mapactivity.MapsActivity
import kotlin.reflect.KClass

class SkierLocationService : Service(), LocationListener {

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Log.v("SkierLocationService", "onStartCommand called!")
		super.onStartCommand(intent, flags, startId)

		val notification: Notification = createPersistentNotification("", null)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			this.startForeground(TRACKING_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
		} else {
			this.startForeground(TRACKING_SERVICE_ID, notification)
		}

		Log.d("SkierLocationService", "Started foreground service")
		return START_NOT_STICKY
	}

	@SuppressLint("MissingPermission")
	override fun onCreate() {
		Log.v("SkierLocationService", "onCreate called!")
		super.onCreate()

		SkiingActivity.populateActivitiesArray(this)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.createNotificationChannels()
		}

		val locationManager: LocationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
				2F, this)
		}
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private fun createNotificationChannels() {

		val trackingNotificationChannel = NotificationChannel(TRACKING_SERVICE_CHANNEL_ID,
			this.getString(R.string.tracking_notification_channel_name), NotificationManager.IMPORTANCE_LOW)

		val progressNotificationChannel = NotificationChannel(ACTIVITY_SUMMARY_CHANNEL_ID,
			this.getString(R.string.activity_summary_notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT)

		val notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.createNotificationChannels(listOf(trackingNotificationChannel,
				progressNotificationChannel))
		Log.v("createNotificatnChnnls", "Created new notification channel")
	}

	@SuppressLint("MissingPermission")
	override fun onDestroy() {
		Log.v("SkierLocationService", "onDestroy has been called!")
		super.onDestroy()

		Locations.visibleLocationUpdates.clear()

		val locationManager: LocationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		locationManager.removeUpdates(this)

		val notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.cancel(TRACKING_SERVICE_ID)

		val file: String = SkiingActivity.writeActivitiesToFile(this)

		val pendingIntent: PendingIntent = this.createPendingIntent(ActivitySummary::class, file)

		val builder: NotificationCompat.Builder = this.getNotificationBuilder(ACTIVITY_SUMMARY_CHANNEL_ID,
			true, R.string.activity_notification_text, pendingIntent)

		val notification: Notification = builder.build()

		notificationManager.notify(ACTIVITY_SUMMARY_ID, notification)

		MtSpokaneMapItems.destroyUIItems()
	}

	override fun onLocationChanged(location: Location) {

		Locations.updateLocations(location)

		// If we are not on the mountain stop the tracking.
		if (MtSpokaneMapItems.skiAreaBounds.points.isEmpty()) {
			this.stopSelf()
		} else if (!MtSpokaneMapItems.skiAreaBounds.locationInsidePoints(location)) {
			this.stopSelf()
		}

		val chairliftTerminal = Locations.checkIfAtChairliftTerminals()
		if (chairliftTerminal != null) {
			val chairliftText: String = this.getString(R.string.current_chairlift, chairliftTerminal.name)
			Locations.visibleLocationUpdates.forEach { it.updateLocation(chairliftText) }
			this.updateNotification(chairliftText, chairliftTerminal.getIcon())
			SkiingActivity.Activities.add(SkiingActivity(chairliftTerminal.name, location, chairliftTerminal.getIcon()))
			return
		}

		val chairliftConfidencePercentage = Locations.getChairliftConfidencePercentage()
		if (chairliftConfidencePercentage >= 0.5F && Locations.mostLikelyChairlift != null) {
			val chairliftText: String = this.getString(R.string.current_chairlift, Locations.mostLikelyChairlift!!.name)
			Locations.visibleLocationUpdates.forEach { it.updateLocation(chairliftText) }
			this.updateNotification(chairliftText, Locations.mostLikelyChairlift!!.getIcon())
			SkiingActivity.Activities.add(SkiingActivity(Locations.mostLikelyChairlift!!.name, location, Locations.mostLikelyChairlift!!.getIcon()))
			return
		}

		val other = Locations.checkIfOnOther()
		if (other != null) {
			val otherText: String = this.getString(R.string.current_other, other.name)
			Locations.visibleLocationUpdates.forEach { it.updateLocation(otherText) }
			this.updateNotification(otherText, other.getIcon())
			SkiingActivity.Activities.add(SkiingActivity(other.name, location, other.getIcon()))
			return
		}

		val run = Locations.checkIfOnRun()
		if (run != null) {
			val runText: String = this.getString(R.string.current_run, run.name)
			Locations.visibleLocationUpdates.forEach { it.updateLocation(runText) }
			this.updateNotification(runText, run.getIcon())
			SkiingActivity.Activities.add(SkiingActivity(run.name, location, run.getIcon()))
			return
		}

		Locations.visibleLocationUpdates.forEach { it.updateLocation(this.getString(R.string.app_name)) }
		this.updateNotification(this.getString(R.string.tracking_notice), null)
	}

	private fun updateNotification(title: String, @DrawableRes icon: Int?) {

		val bitmap: Bitmap? = if (icon != null) {
			drawableToBitmap(AppCompatResources.getDrawable(this, icon)!!)
		} else {
			null
		}

		val notification: Notification = createPersistentNotification(title, bitmap)

		val notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(TRACKING_SERVICE_ID, notification)
	}

	@SuppressLint("UnspecifiedImmutableFlag")
	private fun createPendingIntent(`class`: KClass<*>, filename: String? = null): PendingIntent {
		val notificationIntent = Intent(this, `class`.java)

		if (filename != null) {
			notificationIntent.putExtra("file", filename)
		}

		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
		} else {
			PendingIntent.getActivity(this, 0, notificationIntent, 0)
		}
	}

	private fun createPersistentNotification(title: String, iconBitmap: Bitmap?): Notification {

		val pendingIntent: PendingIntent = this.createPendingIntent(MapsActivity::class)

		val builder: NotificationCompat.Builder = this.getNotificationBuilder(TRACKING_SERVICE_CHANNEL_ID,
			false, R.string.tracking_notice, pendingIntent)
		builder.setContentText(title)

		if (iconBitmap != null) {
			builder.setLargeIcon(iconBitmap)
		}

		return builder.build()
	}

	private fun getNotificationBuilder(channelId: String, showTime: Boolean, @StringRes titleText: Int,
		pendingIntent: PendingIntent): NotificationCompat.Builder {

		return NotificationCompat.Builder(this, channelId)
			.setSmallIcon(R.drawable.icon_fg)
			.setShowWhen(showTime)
			.setContentTitle(this.getString(titleText))
			.setContentIntent(pendingIntent)
	}

	override fun onBind(intent: Intent?): IBinder? {

		// We don't provide binding, so return null
		return null
	}

	companion object {

		const val TRACKING_SERVICE_ID = 69

		const val ACTIVITY_SUMMARY_ID = 420

		const val TRACKING_SERVICE_CHANNEL_ID = "skiAppTracker"

		const val ACTIVITY_SUMMARY_CHANNEL_ID = "skiAppProgress"

		@Deprecated("Sniffing for running services is discouraged.")
		fun checkIfRunning(activity: MapsActivity): Boolean {

			val activityManager: ActivityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
			activityManager.getRunningServices(Int.MAX_VALUE).forEach {
				if (it.service.className == SkierLocationService::class.java.name) {
					return true
				}
			}

			return false
		}

		/**
		 * @author https://studiofreya.com/2018/08/15/android-notification-large-icon-from-vector-xml/
		 */
		private fun drawableToBitmap(drawable: Drawable): Bitmap? {

			if (drawable is BitmapDrawable) {
				return drawable.bitmap
			}

			val bitmap: Bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight,
				Bitmap.Config.ARGB_8888)

			val canvas = Canvas(bitmap)
			drawable.setBounds(0, 0, canvas.width, canvas.height)
			drawable.draw(canvas)

			return bitmap
		}
	}
}