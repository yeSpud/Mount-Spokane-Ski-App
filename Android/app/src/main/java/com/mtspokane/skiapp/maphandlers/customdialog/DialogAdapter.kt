package com.mtspokane.skiapp.maphandlers.customdialog

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.lifecycle.lifecycleScope
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummary
import com.mtspokane.skiapp.activities.mainactivity.MapsActivity
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import com.mtspokane.skiapp.mapItem.PolylineMapItem
import com.mtspokane.skiapp.maphandlers.MapHandler
import kotlinx.coroutines.launch

class DialogAdapter(private val mapHandler: MapHandler, private val count: Int) : BaseAdapter() {

	private val layoutInflater: LayoutInflater = LayoutInflater.from(mapHandler.activity)

	private var showChairliftImage: CustomDialogEntry? = null

	private var showEasyRunsImage: CustomDialogEntry? = null

	private var showModerateRunsImage: CustomDialogEntry? = null

	private var showDifficultRunsImage: CustomDialogEntry? = null

	private var showNightRunsButton: CustomDialogEntry? = null

	private var launchActivitySummaryButton: CustomDialogEntry? = null

	override fun getCount(): Int {
		return this.count
	}

	override fun getItem(position: Int): Any {
		return position
	}

	override fun getItemId(position: Int): Long {
		return position.toLong()
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

		val view: View = convertView ?: layoutInflater.inflate(R.layout.custom_menu_v3, parent, false)

		if (MtSpokaneMapItems.chairlifts == null) {
			this.mapHandler.activity.lifecycleScope.launch {
				MtSpokaneMapItems.initializeChairliftsAsync(this@DialogAdapter.mapHandler.activity::class,
					this@DialogAdapter.mapHandler).start()
			}
		}

		if (MtSpokaneMapItems.easyRuns == null) {
			this.mapHandler.activity.lifecycleScope.launch {
				MtSpokaneMapItems.initializeEasyRunsAsync(this@DialogAdapter.mapHandler.activity::class,
					this@DialogAdapter.mapHandler).start()
			}
		}

		if (MtSpokaneMapItems.moderateRuns == null) {
			this.mapHandler.activity.lifecycleScope.launch {
				MtSpokaneMapItems.initializeModerateRunsAsync(this@DialogAdapter.mapHandler.activity::class,
					this@DialogAdapter.mapHandler).start()
			}
		}

		if (MtSpokaneMapItems.difficultRuns == null) {
			this.mapHandler.activity.lifecycleScope.launch {
				MtSpokaneMapItems.initializeDifficultRunsAsync(this@DialogAdapter.mapHandler.activity::class,
					this@DialogAdapter.mapHandler).start()
			}
		}

		if (this.showChairliftImage == null) {
			this.showChairliftImage = view.findViewById(R.id.show_chairlift)
			this.showChairliftImage!!.setOnClickListener(CustomOnClickListener(MtSpokaneMapItems.chairlifts!!))
		}

		if (this.showEasyRunsImage == null) {
			this.showEasyRunsImage = view.findViewById(R.id.show_easy_runs)
			this.showEasyRunsImage!!.setOnClickListener(CustomOnClickListener(MtSpokaneMapItems.easyRuns!!))
		}

		if (this.showModerateRunsImage == null) {
			this.showModerateRunsImage = view.findViewById(R.id.show_moderate_runs)
			this.showModerateRunsImage!!.setOnClickListener(CustomOnClickListener(MtSpokaneMapItems.moderateRuns!!))
		}


		if (this.showDifficultRunsImage == null) {
			this.showDifficultRunsImage = view.findViewById(R.id.show_difficult_runs)
			this.showDifficultRunsImage!!.setOnClickListener(CustomOnClickListener(MtSpokaneMapItems.difficultRuns!!))
		}

		if (this.showNightRunsButton == null) {
			this.showNightRunsButton = view.findViewById(R.id.show_night_runs)
			this.showNightRunsButton!!.setOnClickListener(NightRunOnClickListener())
		}


		if (this.launchActivitySummaryButton == null) {
			this.launchActivitySummaryButton = view.findViewById(R.id.launch_activity_summary)
			this.launchActivitySummaryButton!!.setOnClickListener {
				if (this.mapHandler.activity is MapsActivity) {
					this.mapHandler.mapOptionsDialog.dismiss()
					this.mapHandler.activity.launchingFromWithin = true
					val intent = Intent(this.mapHandler.activity, ActivitySummary::class.java)
					this.mapHandler.activity.startActivity(intent)
				}
			}
		}

		return view
	}

	companion object {
		private var isNightOnly: Boolean = false
	}

	private class CustomOnClickListener(val runs: List<PolylineMapItem>): View.OnClickListener {

		override fun onClick(v: View?) {

			Log.v("CustomOnClickListener", "Custom button has been clicked!")
			for (run in this.runs) {
				run.togglePolyLineVisibility(!run.defaultVisibility, isNightOnly)
			}
		}
	}

	private class NightRunOnClickListener: View.OnClickListener {

		private var nightOnly = false
		override fun onClick(view: View?) {

			if (view == null) {
				return
			}

			if (view !is CustomDialogEntry) {
				return
			}

			Log.v("onClickListener", "Show night runs has been clicked!")

			nightOnly = !nightOnly

			MtSpokaneMapItems.chairlifts!!.forEach {
				it.togglePolyLineVisibility(it.defaultVisibility, nightOnly)
			}

			MtSpokaneMapItems.easyRuns!!.forEach {
				it.togglePolyLineVisibility(it.defaultVisibility, nightOnly)
			}

			MtSpokaneMapItems.moderateRuns!!.forEach {
				it.togglePolyLineVisibility(it.defaultVisibility, nightOnly)
			}

			MtSpokaneMapItems.difficultRuns!!.forEach {
				it.togglePolyLineVisibility(it.defaultVisibility, nightOnly)
			}
		}
	}
}