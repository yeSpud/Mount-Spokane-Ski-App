package com.mtspokane.skiapp.activitysummary

import android.app.Activity
import android.os.Bundle
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding

class ActivitySummary: Activity() {

	lateinit var binding: ActivitySummaryBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		this.binding = ActivitySummaryBinding.inflate(this.layoutInflater)
		this.setContentView(this.binding.root)

		// Be sure to show the action bar.
		this.actionBar!!.setDisplayShowTitleEnabled(true)

		// TODO
	}

}