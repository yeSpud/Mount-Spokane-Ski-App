package com.mtspokane.skiapp.activitysummary

import android.app.Activity
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
import androidx.core.view.isNotEmpty
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding
import com.mtspokane.skiapp.skierlocation.SkierLocationService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

class ActivitySummary : Activity() {

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

		var startingActivity: SkiingActivity? = null
		var endingActivity: SkiingActivity? = null

		activities.forEach { skiingActivity: SkiingActivity ->

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
	}

	private fun createLayoutParameters(width: Int, height: Int, marginLeft: Int = 0, marginTop: Int = 0,
	                                   marginRight: Int = 0, marginBottom: Int = 0): TableRow.LayoutParams {
		val layoutParameter: TableRow.LayoutParams = TableRow.LayoutParams(width, height)
		layoutParameter.setMargins(marginLeft, marginTop, marginRight, marginBottom)
		return layoutParameter
	}

	private fun getTimeFromLong(time: Long): String {
		val timeFormatter = SimpleDateFormat("h:mm:ss", Locale.US)
		val date = Date(time)
		return timeFormatter.format(date)
	}

	private fun createActivityView(@DrawableRes icon: Int?, titleText: String,
	                               startTime: Long, endTime: Long?): LinearLayout {

		val activityContainer = LinearLayout(this)
		activityContainer.layoutParams = this.createLayoutParameters(ViewGroup.LayoutParams.MATCH_PARENT,
			50, marginTop = 5, marginBottom = 5)
		activityContainer.orientation = LinearLayout.HORIZONTAL

		val imageContainer = ImageView(this)
		val imageLayoutParameters = this.createLayoutParameters(50, 50, marginRight = 10,
			marginLeft = 10)
		imageLayoutParameters.weight = 0.0F
		imageContainer.layoutParams = imageLayoutParameters
		imageContainer.contentDescription = this.getString(R.string.icon_description)
		if (icon != null) {
			imageContainer.setImageDrawable(AppCompatResources.getDrawable(this, icon))
		} else {
			imageContainer.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_missing))
		}
		activityContainer.addView(imageContainer)

		val titleContainer = HorizontalScrollView(this)
		val titleContainerLayoutParameters = this.createLayoutParameters(ViewGroup.LayoutParams.WRAP_CONTENT,
			50)
		titleContainerLayoutParameters.weight = 1.0F
		titleContainer.layoutParams = titleContainerLayoutParameters

		val title = TextView(this)
		title.layoutParams = this.createLayoutParameters(ViewGroup.LayoutParams.WRAP_CONTENT,
			50)
		title.textSize = 27.0F
		title.textAlignment = View.TEXT_ALIGNMENT_CENTER
		title.gravity = Gravity.TOP
		title.text = titleText
		titleContainer.addView(title)
		activityContainer.addView(titleContainer)

		// Time container
		val timeContainer = LinearLayout(this)
		val timeContainerLayoutParameters = this.createLayoutParameters(ViewGroup.LayoutParams.WRAP_CONTENT,
			50, marginLeft = 10, marginRight = 10)
		timeContainerLayoutParameters.weight = 0.0F
		timeContainer.layoutParams = timeContainerLayoutParameters
		timeContainer.orientation = LinearLayout.VERTICAL

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
		activityContainer.addView(timeContainer)

		return activityContainer
	}

	companion object {

		private const val JSON_MIME_TYPE = "application/json"

		private const val GEOJSON_MIME_TYPE = "application/geojson"

		private const val WRITE_JSON_CODE = 509

		private const val WRITE_GEOJSON_CODE = 666
	}
}