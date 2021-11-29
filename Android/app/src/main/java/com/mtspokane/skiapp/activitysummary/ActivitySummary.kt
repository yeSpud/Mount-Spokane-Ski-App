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

	private lateinit var binding: ActivitySummaryBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		this.binding = ActivitySummaryBinding.inflate(this.layoutInflater)
		this.setContentView(this.binding.root)

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
		this.binding.container.addView(view)
	}

	private fun createActivityView(activity: SkiingActivity): LinearLayout {

		val linearLayout = LinearLayout(this)
		linearLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
		linearLayout.orientation = LinearLayout.HORIZONTAL
		linearLayout.setPadding(10)


		val imageView = ImageView(this)
		val imageLayoutParams = this.createLayoutParameters(ViewGroup.MarginLayoutParams.MATCH_PARENT,
			ViewGroup.MarginLayoutParams.WRAP_CONTENT, 5)
		imageView.layoutParams = imageLayoutParams
		imageView.contentDescription = this.getText(R.string.icon_description)
		if (activity.icon != null) {
			imageView.setImageDrawable(AppCompatResources.getDrawable(this, activity.icon))
		}
		linearLayout.addView(imageView)


		val nameView = TextView(this)
		val nameLayoutParams = this.createLayoutParameters(ViewGroup.MarginLayoutParams.WRAP_CONTENT,
			45, 5)
		nameView.layoutParams = nameLayoutParams
		nameView.maxWidth = 249
		nameView.textSize = 20F
		nameView.text = activity.name
		linearLayout.addView(nameView)


		val timeView = TextView(this)
		val timeLayoutParams = this.createLayoutParameters(0, 45, 5)
		val weightLayoutParams = TableRow.LayoutParams(timeLayoutParams)
		weightLayoutParams.weight = 10F
		timeView.layoutParams = weightLayoutParams
		timeView.minWidth = 64
		timeView.textSize = 12F
		timeView.text = " - ${activity.location.time}" // TODO Convert this from unix epoch to actual time
		linearLayout.addView(timeView)


		val arrowTextView = TextView(this)
		val arrowLayoutParams = this.createLayoutParameters(ViewGroup.MarginLayoutParams.WRAP_CONTENT,
			45, 0)
		arrowTextView.layoutParams = arrowLayoutParams
		arrowTextView.text = this.getText(R.string.arrow)
		arrowTextView.textSize = 35F
		linearLayout.addView(arrowTextView)

		return linearLayout
	}

	private fun createLayoutParameters(width: Int, height: Int, marginEnd: Int): ViewGroup.MarginLayoutParams {
		val layoutParameter: ViewGroup.MarginLayoutParams = ViewGroup.MarginLayoutParams(width, height)
		layoutParameter.marginEnd = marginEnd
		return layoutParameter
	}
}