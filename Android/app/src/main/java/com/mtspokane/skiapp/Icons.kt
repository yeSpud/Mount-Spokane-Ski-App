package com.mtspokane.skiapp

import android.util.Log
import xyz.thespud.skimap.locationmanager.CustomIcons

class Icons : CustomIcons {

	override fun getOtherIcon(name: String): Int {
		Log.d("getOtherIcon", "Getting icon for $name")
		val icon = when (name) {
			"Lodge 1" -> R.drawable.ic_lodge
			"Lodge 2" -> R.drawable.ic_lodge
			"Yurt" -> R.drawable.ic_yurt
			"Vista House" -> R.drawable.ic_vista_house
			"Ski Patrol Building" -> R.drawable.ic_ski_patrol_icon
			"Lodge 1 Parking Lot" -> R.drawable.ic_parking
			"Lodge 2 Parking Lot" -> R.drawable.ic_parking
			"Tubing Area" -> R.drawable.ic_missing // Todo Tubing area icon
			"Ski School" -> R.drawable.ic_ski_school
			"Learning Area" -> R.drawable.ic_ski_school
			"Top of 1, 6", "Top of 2", "Top of 3", "Top of 4", "Top of 5" -> R.drawable.ic_chairlift
			else -> {
				Log.w("getOtherIcon", "$name does not have an icon")
				R.drawable.ic_missing
			}
		}

		return icon
	}
}