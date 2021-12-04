package com.mtspokane.skiapp.activitysummary

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.mtspokane.skiapp.R
import java.io.File

class FileSelectionDialog : DialogFragment() {

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

		val builder: AlertDialog.Builder = AlertDialog.Builder(this.activity)

		val layoutInflater: LayoutInflater = LayoutInflater.from(this.activity)

		val view: View = layoutInflater.inflate(R.layout.file_selection, null)

		val filesListView: LinearLayout = view.findViewById(R.id.files)

		val files: Array<File> = this.requireActivity().filesDir.listFiles()!!
		files.forEach {

			if (it.name.matches(fileRegex)) {

				val fileEntry: TextView = createFileView(it)
				filesListView.addView(fileEntry)
			}
		}

		builder.setView(view)
		builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
		return builder.create()
	}

	private fun createFileView(file: File): TextView {

		val textView = TextView(this.activity)
		textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
		textView.text = file.name
		textView.textSize = 25.0F
		textView.setOnClickListener {

			val activities: Array<SkiingActivity> = SkiingActivity.readFromFile(this.requireActivity(), file.name)

			(this.requireActivity() as ActivitySummary).loadActivities(activities)

			this.dismiss()
		}

		return textView
	}

	companion object {

		val fileRegex: Regex = Regex("\\d\\d\\d\\d-\\d\\d-\\d\\d.json")

	}
}