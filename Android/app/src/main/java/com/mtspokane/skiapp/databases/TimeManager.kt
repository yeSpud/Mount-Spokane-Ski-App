package com.mtspokane.skiapp.databases

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Deprecated("To be removed")
object TimeManager {

	fun getTodaysDate(): String {
		val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
		return dateFormat.format(Date())
	}

	fun isValidDateFormat(date: String): Boolean {
		return date.matches(Regex("\\d\\d\\d\\d-\\d\\d-\\d\\d"))
	}

	fun getTimeFromLong(time: Long): String {
		val timeFormatter = SimpleDateFormat("h:mm:ss", Locale.US)
		val date = Date(time)
		return timeFormatter.format(date)
	}

	fun getDateFromLong(time: Long): String {
		val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
		val date = Date(time)
		return dateFormat.format(date)
	}
}