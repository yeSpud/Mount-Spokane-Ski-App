package com.mtspokane.skiapp.activitysummary

import android.content.Context
import android.location.Location
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mtspokane.skiapp.databinding.SkiingActivityLayoutBinding


class SkiingActivityLayout(context: Context, name: String, location: Location): LinearLayout(context) {

	val icon: ImageView

	val name: TextView

	val time: TextView

	init {

		val layoutInflater: LayoutInflater = LayoutInflater.from(this.context)
		val binding = SkiingActivityLayoutBinding.inflate(layoutInflater)

		this.icon = binding.icon

		this.name = binding.name
		this.name.text = name

		this.time = binding.time
		this.time.text = " - ${location.time}"
	}

}