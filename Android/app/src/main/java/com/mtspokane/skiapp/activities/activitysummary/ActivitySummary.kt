package com.mtspokane.skiapp.activities.activitysummary

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isNotEmpty
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.SupportMapFragment
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding
import com.mtspokane.skiapp.maphandlers.ActivitySummaryMap
import com.mtspokane.skiapp.skierlocation.SkierLocationService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

class ActivitySummary : FragmentActivity() {

	private var mapHandler: ActivitySummaryMap? = null

	private lateinit var container: LinearLayout

	private lateinit var fileSelectionDialog: FileSelectionDialog

	private lateinit var creditDialog: AlertDialog

	var loadedFile: String = "${SkiingActivity.getDate()}.json"

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val binding: ActivitySummaryBinding = ActivitySummaryBinding.inflate(this.layoutInflater)
		this.setContentView(binding.root)

		this.container = binding.container

		this.fileSelectionDialog = FileSelectionDialog(this)

		val creditDialogBuilder = AlertDialog.Builder(this)
		creditDialogBuilder.setView(R.layout.icon_credits)
		creditDialogBuilder.setPositiveButton(R.string.close_button) { dialog, _ -> dialog.dismiss() }
		this.creditDialog = creditDialogBuilder.create()

		// Be sure to show the action bar.
		this.actionBar!!.setDisplayShowTitleEnabled(true)

		// Setup the map handler.
		this.mapHandler = ActivitySummaryMap(this)

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.activity_map) as SupportMapFragment
		mapFragment.getMapAsync(this.mapHandler!!)

		if (this.intent.extras != null) {

			val filename: String? = this.intent.extras!!.getString("file")

			if (filename != null) {
				val notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE)
						as NotificationManager
				notificationManager.cancel(SkierLocationService.ACTIVITY_SUMMARY_ID)

				val activities: Array<SkiingActivity> = SkiingActivity
					.readSkiingActivitiesFromFile(this, filename)
				this.loadActivities(activities)
				this.loadedFile = filename
				return
			}
		}

		// If all else fails just load from the current activities array.
		this.loadActivities(SkiingActivity.Activities.toTypedArray())
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		this.menuInflater.inflate(R.menu.summary_menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {

		val shareMenu: MenuItem = menu.findItem(R.id.share)
		val exportMenu: MenuItem = menu.findItem(R.id.export)

		shareMenu.isEnabled = this.container.isNotEmpty()
		exportMenu.isEnabled = this.container.isNotEmpty()

		return super.onPrepareOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {

		when (item.itemId) {
			R.id.open -> this.fileSelectionDialog.showDialog()
			R.id.export_json -> SkiingActivity.createNewFileSAF(this, this.loadedFile,
				JSON_MIME_TYPE, WRITE_JSON_CODE)
			R.id.export_geojson -> SkiingActivity.createNewFileSAF(
				this, this.loadedFile.replace("json", "geojson"),
				GEOJSON_MIME_TYPE, WRITE_GEOJSON_CODE)
			R.id.share_json -> {
				val file = File(this.filesDir, this.loadedFile)
				SkiingActivity.shareFile(this, file, JSON_MIME_TYPE)
			}
			R.id.share_geojson -> {

				val json: JSONObject = SkiingActivity.readJsonFromFile(this, this.loadedFile)
				val geojson: JSONObject = SkiingActivity.convertJsonToGeoJson(json)

				val tmpFileName = this.loadedFile.replace("json", "geojson")
				this.openFileOutput(tmpFileName, Context.MODE_PRIVATE).use {
					it.write(geojson.toString(4).toByteArray())
				}

				val tmpFile = File(this.filesDir, tmpFileName)

				SkiingActivity.shareFile(this, tmpFile, JSON_MIME_TYPE)

				tmpFile.delete()
			}
			R.id.credits -> this.creditDialog.show()
		}

		return super.onOptionsItemSelected(item)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		if (resultCode == RESULT_OK) {

			if (data == null) {
				return
			}

			val fileUri: Uri = data.data ?: return
			val json: JSONObject = SkiingActivity.readJsonFromFile(this, this.loadedFile)

			when (requestCode) {
				WRITE_JSON_CODE -> SkiingActivity.writeToExportFile(this.contentResolver, fileUri,
					json.toString(4))
				WRITE_GEOJSON_CODE -> {
					val geoJson: JSONObject = SkiingActivity.convertJsonToGeoJson(json)
					SkiingActivity.writeToExportFile(this.contentResolver, fileUri, geoJson.toString(4))
				}
				else -> Log.w("onActivityResult", "Unaccounted for code: $resultCode")
			}
		}
	}

	fun loadActivities(activities: Array<SkiingActivity>) {

		this.container.removeAllViews()

		if (this.mapHandler != null) {
			this.mapHandler!!.locationMarkers.forEach {
				it.remove()
			}
		}

		var startingActivity: SkiingActivity? = null
		var endingActivity: SkiingActivity? = null

		activities.forEach { skiingActivity: SkiingActivity ->

			if (this.mapHandler != null) {
				this.mapHandler!!.addMarker(skiingActivity.latitude, skiingActivity.longitude)
			}

			if (startingActivity == null) {
				startingActivity = skiingActivity
			} else {

				if (skiingActivity.name == startingActivity!!.name) {
					endingActivity = skiingActivity
				} else {
					if (endingActivity != null) {
						this.container.addView(this.createActivityView(startingActivity!!.icon,
								startingActivity!!.name, startingActivity!!.time, endingActivity!!.time))
						endingActivity = null
					} else {
						this.container.addView(this.createActivityView(startingActivity!!.icon,
							startingActivity!!.name, startingActivity!!.time, startingActivity!!.time))
					}

					startingActivity = skiingActivity
				}
			}
		}

		if (startingActivity != null) {
			if (endingActivity != null) {
				this.container.addView(this.createActivityView(startingActivity!!.icon, startingActivity!!.name,
					startingActivity!!.time, endingActivity!!.time))
			} else {
				this.container.addView(this.createActivityView(startingActivity!!.icon, startingActivity!!.name,
					startingActivity!!.time, null))
			}
		}
	}

	/*
	private fun createLayoutParameters(width: Int, height: Int, marginLeft: Int = 0, marginTop: Int = 0,
	                                   marginRight: Int = 0, marginBottom: Int = 0): TableRow.LayoutParams {
		val layoutParameter: TableRow.LayoutParams = TableRow.LayoutParams(width, height)
		layoutParameter.setMargins(marginLeft, marginTop, marginRight, marginBottom)
		return layoutParameter
	}*/

	private fun getTimeFromLong(time: Long): String {
		val timeFormatter = SimpleDateFormat("h:mm:ss", Locale.US)
		val date = Date(time)
		return timeFormatter.format(date)
	}

	private fun createActivityView(@DrawableRes icon: Int?, titleText: String,
	                               startTime: Long, endTime: Long?): ActivityView {

		val activityView = ActivityView(this)

		if (icon != null) {
			activityView.icon.setImageDrawable(AppCompatResources.getDrawable(this, icon))
		} else {
			activityView.icon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_missing))
		}

		activityView.title.text = titleText
		activityView.startTime.text = this.getTimeFromLong(startTime)

		if (endTime != null) {
			activityView.endTime.text = this.getTimeFromLong(endTime)
		}

		return activityView
		/*
		val activityContainer = ConstraintLayout(this)
		activityContainer.layoutParams = this.createLayoutParameters(ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT, marginTop = 10, marginBottom = 10)
		activityContainer.id = activityContainer.hashCode()

		/*
		val oldActivityContainer = LinearLayout(this)
		oldActivityContainer.layoutParams = this.createLayoutParameters(ViewGroup.LayoutParams.MATCH_PARENT,
			50, marginTop = 5, marginBottom = 5)
		oldActivityContainer.orientation = LinearLayout.HORIZONTAL*/

		val imageContainer = ImageView(this)
		val imageLayoutParameters = this.createLayoutParameters(50, 50, marginLeft = 10)
		imageContainer.layoutParams = imageLayoutParameters
		imageContainer.contentDescription = this.getString(R.string.icon_description)
		if (icon != null) {
			imageContainer.setImageDrawable(AppCompatResources.getDrawable(this, icon))
		} else {
			imageContainer.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_missing))
		}
		imageContainer.id = imageContainer.hashCode()

		val imageConstraintSet = ConstraintSet()
		imageConstraintSet.clone(activityContainer)
		imageConstraintSet.connect(activityContainer.id, ConstraintSet.START, imageContainer.id, ConstraintSet.START)
		imageConstraintSet.connect(activityContainer.id, ConstraintSet.TOP, imageContainer.id, ConstraintSet.TOP)
		imageConstraintSet.applyTo(activityContainer)

		//oldActivityContainer.addView(imageContainer)

		// Time container
		val timeContainer = LinearLayout(this)
		val timeContainerLayoutParameters = this.createLayoutParameters(ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT, marginRight = 10)
		timeContainer.layoutParams = timeContainerLayoutParameters
		timeContainer.orientation = LinearLayout.VERTICAL
		timeContainer.id = timeContainer.hashCode()

		val timeContainerConstraintSet = ConstraintSet()
		timeContainerConstraintSet.clone(activityContainer)
		timeContainerConstraintSet.connect(activityContainer.id, ConstraintSet.END, timeContainer.id, ConstraintSet.END)
		timeContainerConstraintSet.connect(activityContainer.id, ConstraintSet.TOP, timeContainer.id, ConstraintSet.TOP)
		timeContainerConstraintSet.applyTo(activityContainer)

		// Start time
		val startTimeTextView = TextView(this)
		startTimeTextView.layoutParams = this.createLayoutParameters(ViewGroup.LayoutParams.WRAP_CONTENT,
			22, marginBottom = 6)
		startTimeTextView.textSize = 11.0F
		startTimeTextView.text = this.getTimeFromLong(startTime)
		timeContainer.addView(startTimeTextView)

		// End time
		if (endTime != null) {
			val endTimeTextView = TextView(this)
			endTimeTextView.layoutParams = this.createLayoutParameters(ViewGroup.LayoutParams.WRAP_CONTENT,
				22)
			endTimeTextView.textSize = 11.0F
			endTimeTextView.text = this.getTimeFromLong(endTime)
			timeContainer.addView(endTimeTextView)
		}

		val titleContainer = HorizontalScrollView(this)
		val titleContainerLayoutParameters = this.createLayoutParameters(0, 50,
			marginLeft = 10, marginRight = 10)
		titleContainer.layoutParams = titleContainerLayoutParameters
		titleContainer.id = titleContainer.hashCode()

		val title = TextView(this)
		title.layoutParams = this.createLayoutParameters(ViewGroup.LayoutParams.WRAP_CONTENT, 50)
		title.textSize = 30.0F
		title.textAlignment = View.TEXT_ALIGNMENT_CENTER
		title.gravity = Gravity.CENTER_VERTICAL
		title.text = titleText
		titleContainer.addView(title)
		//oldActivityContainer.addView(titleContainer)

		val horizontalContainerSet = ConstraintSet()
		horizontalContainerSet.clone(activityContainer)
		horizontalContainerSet.connect(activityContainer.id, ConstraintSet.LEFT, titleContainer.id,
			ConstraintSet.RIGHT)
		horizontalContainerSet.connect(activityContainer.id, ConstraintSet.RIGHT, titleContainer.id,
			ConstraintSet.LEFT)
		horizontalContainerSet.connect(activityContainer.id, ConstraintSet.TOP, titleContainer.id,
			ConstraintSet.TOP)
		horizontalContainerSet.applyTo(activityContainer)

		// oldActivityContainer.addView(timeContainer)

		return activityContainer */
	}

	companion object {

		private const val JSON_MIME_TYPE = "application/json"

		private const val GEOJSON_MIME_TYPE = "application/geojson"

		private const val WRITE_JSON_CODE = 509

		private const val WRITE_GEOJSON_CODE = 666
	}
}