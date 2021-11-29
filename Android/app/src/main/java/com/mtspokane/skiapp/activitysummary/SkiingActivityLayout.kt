package com.mtspokane.skiapp.activitysummary

import android.content.Context
import android.location.Location
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mtspokane.skiapp.databinding.SkiingActivityLayoutBinding

@Deprecated("Build linear layout like a normal person")
class SkiingActivityLayout(context: Context, attributes: AttributeSet?, style: Int): LinearLayout(context, attributes, style, 0) {

	constructor(context: Context): this(context, null, 0)

	constructor(context: Context, attributes: AttributeSet?): this(context, attributes, 0)

	constructor(context: Context, name: String, location: Location) : this(context, null, 0) {
		this.name.text = name
		this.time.text = " - ${location.time}"
	}

	val icon: ImageView

	val name: TextView

	val time: TextView

	init {

		val layoutInflater: LayoutInflater = LayoutInflater.from(this.context)
		val binding = SkiingActivityLayoutBinding.inflate(layoutInflater)

		this.icon = binding.icon
		this.name = binding.name
		this.time = binding.time
	}

}