package com.mtspokane.skiapp.activities

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.mapItem.MapMarker

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

        icon = findViewById(R.id.icon)
        title = findViewById(R.id.title_view)
        maxSpeed = findViewById(R.id.max_speed)
        averageSpeed = findViewById(R.id.average_speed)
        startTime = findViewById(R.id.start_time)
        endTime = findViewById(R.id.end_time)
    }
}

data class ActivitySummaryEntry(val mapMarker: MapMarker, val maxSpeed: Float, val averageSpeed: Float, val endTime: Long?)