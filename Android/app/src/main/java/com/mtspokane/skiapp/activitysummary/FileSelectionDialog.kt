package com.mtspokane.skiapp.activitysummary

import android.app.AlertDialog
import android.view.ViewGroup
import android.widget.TextView
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databinding.FileSelectionBinding
import java.io.File

class FileSelectionDialog(private val activity: ActivitySummary) : AlertDialog(activity) {

	fun showDialog() {

		val binding: FileSelectionBinding = FileSelectionBinding.inflate(this.layoutInflater)

		val alertDialogBuilder = Builder(this.context)
		alertDialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
		alertDialogBuilder.setView(binding.root)

		val dialog: AlertDialog = alertDialogBuilder.create()

		val files: Array<File> = this.context.filesDir.listFiles()!!
		files.forEach { file ->

			if (file.name.matches(fileRegex)) {

				val textView = TextView(this.context)
				textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT)
				textView.text = file.name
				textView.textSize = 25.0F
				textView.setOnClickListener {

					val activities: Array<SkiingActivity> = SkiingActivity.readFromFile(this.context, file.name)

					this.activity.loadActivities(activities)

					dialog.dismiss()
				}

				binding.files.addView(textView)
			}
		}

		dialog.show()
	}

	companion object {

		val fileRegex: Regex = Regex("\\d\\d\\d\\d-\\d\\d-\\d\\d.json")

	}
}