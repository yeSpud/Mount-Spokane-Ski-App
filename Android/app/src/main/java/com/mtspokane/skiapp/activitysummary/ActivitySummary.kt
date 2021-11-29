package com.mtspokane.skiapp.activitysummary

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.setPadding
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding

class ActivitySummary: Activity() {

	private lateinit var container: LinearLayout

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val binding: ActivitySummaryBinding = ActivitySummaryBinding.inflate(this.layoutInflater)
		this.setContentView(binding.root)

		this.container = binding.container

		// Be sure to show the action bar.
		this.actionBar!!.setDisplayShowTitleEnabled(true)

		if (this.intent.extras != null) {
			// TODO Load activities from file passed in as an extra.
		} else {
			this.addAllActivities(SkiingActivity.Activities.toTypedArray())
		}
	}

	private fun addAllActivities(activities: Array<SkiingActivity>) {
		activities.forEach {
			this.addActivity(it)
		}
	}

	private fun addActivity(activity: SkiingActivity) {

		val view = createActivityView(activity)
		this.container.addView(view)
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
		//val nameView = this.createTextView(nameLayoutParams, maxWidth = 249, textSize = 20F, text = activity.name)
		val nameView = this.createTextView(nameLayoutParams, 20F, activity.name)
		linearLayout.addView(nameView)


		val timeLayoutParams = this.createLayoutParameters(ViewGroup.MarginLayoutParams.WRAP_CONTENT,
			ViewGroup.MarginLayoutParams.WRAP_CONTENT, 5)
		val weightLayoutParams = TableRow.LayoutParams(timeLayoutParams)
		weightLayoutParams.weight = 10F
		//val timeView = this.createTextView(weightLayoutParams, minWidth = 64, textSize = 12F, text = " - ${activity.location.time}")
		val timeView = this.createTextView(weightLayoutParams, 12F, " - ${activity.location.time}") // TODO Convert this from unix epoch to actual time
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
}