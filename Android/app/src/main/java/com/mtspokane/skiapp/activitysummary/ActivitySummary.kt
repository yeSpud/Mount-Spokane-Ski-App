package com.mtspokane.skiapp.activitysummary

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding

class ActivitySummary: Activity() {

	lateinit var binding: ActivitySummaryBinding

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

	private fun createActivityView(activity: SkiingActivity): SkiingActivityLayout {

		val view = SkiingActivityLayout(this, activity.name, activity.location)

		if (activity.icon != null) {
			view.icon.setImageDrawable(AppCompatResources.getDrawable(this, activity.icon))
		}

		return view
	}

}