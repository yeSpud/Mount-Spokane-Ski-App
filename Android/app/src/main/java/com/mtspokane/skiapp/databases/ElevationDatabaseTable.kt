package com.mtspokane.skiapp.databases

import androidx.room.Entity

@Entity(tableName = "elevation", primaryKeys = ["latitude", "longitude"])
data class ElevationDatabaseTable(val latitude: Double, val longitude: Double, val elevation: Double)
