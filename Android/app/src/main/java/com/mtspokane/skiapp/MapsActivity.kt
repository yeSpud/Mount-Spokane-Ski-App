package com.mtspokane.skiapp

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.SupportMapFragment
import com.mtspokane.skiapp.databinding.ActivityMapsBinding
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapsActivity : FragmentActivity() {

	private val map = MapHandler(this)

	lateinit var locationPopupDialog: AlertDialog

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Setup data binding.
		val binding = ActivityMapsBinding.inflate(layoutInflater)
		setContentView(binding.root)

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
		mapFragment!!.getMapAsync(this.map)

		// Setup the location popup dialog.
		val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
		alertDialogBuilder.setTitle(R.string.alert_title)
		alertDialogBuilder.setMessage(R.string.alert_message)
		alertDialogBuilder.setPositiveButton(R.string.alert_ok) { _, _ -> ActivityCompat.
		requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), permissionValue) }
		this.locationPopupDialog = alertDialogBuilder.create()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		this.menuInflater.inflate(R.menu.menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {

		val checked = !item.isChecked

		item.isChecked = checked

		when (item.itemId) {
			R.id.chairlift -> this.map.chairlifts.forEach{it.value.togglePolyLineVisibility(checked)}
			R.id.easy -> this.map.easyRuns.forEach{it.value.togglePolyLineVisibility(checked)}
			R.id.moderate -> this.map.moderateRuns.forEach{it.value.togglePolyLineVisibility(checked)}
			R.id.difficult -> this.map.difficultRuns.forEach{it.value.togglePolyLineVisibility(checked)}
			R.id.night -> {
				this.map.chairlifts.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
				this.map.easyRuns.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
				this.map.moderateRuns.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
				this.map.difficultRuns.forEach{ it.value.togglePolyLineVisibility(it.value.defaultVisibility, checked) }
			}
		}

		return super.onOptionsItemSelected(item)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		when (requestCode) {

			// If request is cancelled, the result arrays are empty.
			permissionValue -> {
				if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					this.lifecycleScope.launch(Dispatchers.Main, CoroutineStart.LAZY) {
						this@MapsActivity.map.setupLocation()
					}.start()
				}
			}
		}
	}

	companion object{

		const val permissionValue = 29500

	}
}