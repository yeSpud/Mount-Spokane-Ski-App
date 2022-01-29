package com.mtspokane.skiapp.databases

import androidx.room.Entity

@Entity(tableName = "Elevation", primaryKeys = ["latitude", "longitude"])
data class Elevation(val latitude: Double, val longitude: Double, val elevation: Double)
