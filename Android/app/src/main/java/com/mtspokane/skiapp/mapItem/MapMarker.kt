package com.mtspokane.skiapp.mapItem

import androidx.annotation.DrawableRes
import com.google.android.gms.maps.model.BitmapDescriptor
import com.mtspokane.skiapp.SkiingActivity

// todo Add equals operator(?)
data class MapMarker(val name: String, val skiingActivity: SkiingActivity, @DrawableRes val icon: Int,
                     val markerColor: BitmapDescriptor, val color: Int)