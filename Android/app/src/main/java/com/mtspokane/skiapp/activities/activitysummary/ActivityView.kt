package com.mtspokane.skiapp.activities.activitysummary

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.mtspokane.skiapp.R

class ActivityView : ConstraintLayout {

	val icon: ImageView

	val title: TextView

	val maxSpeed: TextView

	val averageSpeed: TextView

	val startTime: TextView

	val endTime: TextView

	constructor(context: Context): this(context, null)

	constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)

	constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context,
		attributeSet, defStyleAttr) {

		inflate(context, R.layout.activity_view, this)

		this.icon = findViewById(R.id.icon)
		this.title = findViewById(R.id.title_view)
		this.maxSpeed = findViewById(R.id.max_speed)
		this.averageSpeed = findViewById(R.id.average_speed)
		this.startTime = findViewById(R.id.start_time)
		this.endTime = findViewById(R.id.end_time)
	}
}