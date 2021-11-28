package com.mtspokane.skiapp

import android.app.Activity
import android.os.Bundle
import com.mtspokane.skiapp.databinding.ActivitySummaryBinding

class ActivitySummary: Activity() {

	lateinit var binding: ActivitySummaryBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		this.binding = ActivitySummaryBinding.inflate(this.layoutInflater)
		this.setContentView(this.binding.root)

		this.actionBar!!.setTitle(R.string.activity_summary_title)

		// TODO
	}

}