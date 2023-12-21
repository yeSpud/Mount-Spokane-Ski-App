package com.mtspokane.skiapp.activities.activitysummary

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.ktx.addCircle
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

@UiThread
class ActivitySummaryCircles(map: GoogleMap, val mapMarker: MapMarker) {

    var circle: Circle? = null
        private set

    fun destroy() {

        if (circle != null) {
            circle!!.remove()
            circle = null
        }
    }

    init {

        val location = LatLng(mapMarker.skiingActivity.latitude, mapMarker.skiingActivity.longitude)

        circle = map.addCircle {
            center(location)
            strokeColor(mapMarker.circleColor)
            fillColor(mapMarker.circleColor)
            clickable(true)
            radius(3.0)
            zIndex(50.0F)
            visible(true)
        }

        circle!!.tag = this
    }
}

data class ActivitySummaryEntry(val mapMarker: MapMarker, val maxSpeed: Float, val averageSpeed: Float, val endTime: Long?)