package com.mtspokane.skiapp.activities.activitysummary

import android.app.AlertDialog
import android.view.ViewGroup
import android.widget.TextView
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.ActivitySummary
import com.mtspokane.skiapp.databases.ActivityDatabase
import com.mtspokane.skiapp.databases.SkiingActivityManager
import com.mtspokane.skiapp.databinding.FileSelectionBinding

class FileSelectionDialog(private val activity: ActivitySummary) : AlertDialog(activity) {

	fun showDialog() {

		val binding: FileSelectionBinding = FileSelectionBinding.inflate(this.layoutInflater)

		val alertDialogBuilder = Builder(this.context)
		alertDialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
		alertDialogBuilder.setView(binding.root)

		val dialog: AlertDialog = alertDialogBuilder.create()

		val db = ActivityDatabase(this.activity)
		val dates: Array<String> = ActivityDatabase.getTables(db.readableDatabase)
		db.close()

		dates.forEach { date: String ->

			val textView = TextView(this.context)
			textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT)
			textView.text = date
			textView.textSize = 25.0F
			textView.setOnClickListener {

				val database = ActivityDatabase(this.activity)
				SkiingActivityManager.FinishedAndLoadedActivities = ActivityDatabase
					.readSkiingActivesFromDatabase(date, database.readableDatabase)
				database.close()

				if (SkiingActivityManager.FinishedAndLoadedActivities != null) {
					this.activity.loadActivities(SkiingActivityManager.FinishedAndLoadedActivities!!)
				}

				dialog.dismiss()
			}

			binding.files.addView(textView)
		}

		dialog.show()
	}
}