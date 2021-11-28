package com.mtspokane.skiapp.activitysummary

import android.location.Location
import java.io.File

class SkiingActivity(val name: String, val location: Location, val icon: Int?) {

	companion object  {

		val Activities: ArrayList<SkiingActivity> = ArrayList(0)

		fun writeToFile() {
			// TODO
		}

		fun readFromFile(file: File): Array<SkiingActivity> {
			// TODO

			return arrayOf(SkiingActivity("foo", Location("bar"), null))
		}
	}
}