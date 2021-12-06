package com.mtspokane.skiapp.activitysummary

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.setPadding
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ActivitySummary: Activity() {

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

	override fun onOptionsItemSelected(item: MenuItem): Boolean {

		when (item.itemId) {
			R.id.open -> this.fileSelectionDialog.showDialog()
			R.id.export_json -> SkiingActivity.createNewFileSAF(this, this.loadedFile,
				"application/json", WRITE_JSON_CODE)
			R.id.export_geojson -> SkiingActivity.createNewFileSAF(this, this.loadedFile
				.replace("json", "geojson"), "application/geojson",
				WRITE_GEOJSON_CODE)
			R.id.export_kml -> SkiingActivity.createNewFileSAF(this, this.loadedFile
				.replace("json", "kml"), "application/vnd.google-earth.kml+xml",
				WRITE_KML_CODE)
			R.id.share_json -> SkiingActivity.shareJsonFile(this, this.loadedFile)
			R.id.share_geojson -> SkiingActivity.shareGeoJsonFile(this, this.loadedFile)
			R.id.share_kml -> SkiingActivity.shareKmlFile(this, this.loadedFile)
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

			when (requestCode) {
				WRITE_JSON_CODE -> {
					val json: JSONObject = SkiingActivity.readJsonFromFile(this, this.loadedFile)
					SkiingActivity.writeToExportFile(this.contentResolver, fileUri, json.toString(4))
				}
				WRITE_GEOJSON_CODE -> {
					val fileJson: JSONObject = SkiingActivity.readJsonFromFile(this, this.loadedFile)
					val geoJson: JSONObject = SkiingActivity.convertJsonToGeoJson(fileJson)
					SkiingActivity.writeToExportFile(this.contentResolver, fileUri, geoJson.toString(4))
				}
				WRITE_KML_CODE -> {
					// TODO Write kml to file
				}
				else -> Log.w("onActivityResult", "Unaccounted for code: $resultCode")
			}

		}
	}

	fun loadActivities(activities: Array<SkiingActivity>) {
		this.container.removeAllViews()
		activities.forEach {
			val view: LinearLayout = createActivityView(it)
			this.container.addView(view)
		}
	}

	private fun createActivityView(activity: SkiingActivity): LinearLayout {

		val linearLayout = LinearLayout(this)
		linearLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
		linearLayout.orientation = LinearLayout.HORIZONTAL
		linearLayout.setPadding(10)


		val imageView = ImageView(this)
		val imageLayoutParams = this.createLayoutParameters(45, 45, 5)
		imageView.layoutParams = imageLayoutParams
		imageView.contentDescription = this.getText(R.string.icon_description)
		if (activity.icon != null) {
			imageView.setImageDrawable(AppCompatResources.getDrawable(this, activity.icon))
		}
		linearLayout.addView(imageView)


		val nameLayoutParams = this.createLayoutParameters(ViewGroup.MarginLayoutParams.WRAP_CONTENT,
			ViewGroup.MarginLayoutParams.WRAP_CONTENT, 5)
		val nameView = this.createTextView(nameLayoutParams, 20F, activity.name)
		linearLayout.addView(nameView)


		val timeLayoutParams = this.createLayoutParameters(ViewGroup.MarginLayoutParams.WRAP_CONTENT,
			ViewGroup.MarginLayoutParams.WRAP_CONTENT, 5)
		val weightLayoutParams = TableRow.LayoutParams(timeLayoutParams)
		weightLayoutParams.weight = 10F
		val time = convertMillisecondsToTime(activity.time)
		val timeView = this.createTextView(weightLayoutParams, 12F, time)
		linearLayout.addView(timeView)


		/*
		val arrowLayoutParams = this.createLayoutParameters(ViewGroup.MarginLayoutParams.WRAP_CONTENT,
			ViewGroup.MarginLayoutParams.WRAP_CONTENT, 0)
		//val arrowTextView = this.createTextView(arrowLayoutParams, textSize = 35F, text = this.getText(R.string.arrow))
		val arrowTextView = TextView(this)
		arrowTextView.layoutParams = arrowLayoutParams
		arrowTextView.textSize = 30F
		arrowTextView.text = this.getText(R.string.arrow)
		linearLayout.addView(arrowTextView) */ // TODO Disable this for now until a launcher is needed.

		return linearLayout
	}

	private fun createLayoutParameters(width: Int, height: Int, marginEnd: Int): ViewGroup.MarginLayoutParams {
		val layoutParameter: ViewGroup.MarginLayoutParams = ViewGroup.MarginLayoutParams(width, height)
		layoutParameter.marginEnd = marginEnd
		return layoutParameter
	}

	private fun createTextView(layoutParams: ViewGroup.LayoutParams, textSize: Float, text: CharSequence): TextView {
		val textView = TextView(this)
		textView.layoutParams = layoutParams
		textView.textSize = textSize
		textView.text = text
		return textView
	}

	companion object {

		const val WRITE_JSON_CODE = 509

		const val WRITE_GEOJSON_CODE = 666

		const val WRITE_KML_CODE = 1250

		private fun convertMillisecondsToTime(milliseconds: Long): String { // TODO Optimize
			return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss")
				val instant: Instant = Instant.ofEpochMilli(milliseconds)
				val date: LocalDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
				val timeString: String = formatter.format(date)
				/*
				if (timeString[0] == '0') {
					timeString.replaceFirst("0", "")
				} */
				timeString
			} else {
				"$milliseconds" // TODO Convert for apis less than Oreo
			}
		}
	}
}