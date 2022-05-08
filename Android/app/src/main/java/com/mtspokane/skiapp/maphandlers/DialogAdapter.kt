package com.mtspokane.skiapp.maphandlers

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.mtspokane.skiapp.R
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummary
import com.mtspokane.skiapp.activities.mainactivity.MapsActivity
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import kotlinx.coroutines.launch

class DialogAdapter(private val mapHandler: MapHandler, private val count: Int): BaseAdapter() {

    private val layoutInflater: LayoutInflater = LayoutInflater.from(mapHandler.activity)

    private var showChairliftImage: View? = null

    private var showEasyRunsImage: View? = null

    private var showModerateRunsImage: View? = null

    private var showDifficultRunsImage: View? = null

    private var showNightRunsButton: Button? = null

    private var launchActivitySummaryButton: Button? = null

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

        val view: View = convertView ?: layoutInflater.inflate(R.layout.custom_menu_v2, parent, false)

        if (MtSpokaneMapItems.chairlifts == null) {
            this.mapHandler.activity.lifecycleScope.launch { MtSpokaneMapItems.initializeChairliftsAsync(this@DialogAdapter.mapHandler.activity::class,
                    this@DialogAdapter.mapHandler).start() }
        }

        if (MtSpokaneMapItems.easyRuns == null) {
            this.mapHandler.activity.lifecycleScope.launch { MtSpokaneMapItems.initializeEasyRunsAsync(this@DialogAdapter.mapHandler.activity::class,
                    this@DialogAdapter.mapHandler).start() }
        }

        if (MtSpokaneMapItems.moderateRuns == null) {
            this.mapHandler.activity.lifecycleScope.launch { MtSpokaneMapItems.initializeModerateRunsAsync(this@DialogAdapter.mapHandler.activity::class,
                    this@DialogAdapter.mapHandler).start() }
        }

        if (MtSpokaneMapItems.difficultRuns == null) {
            this.mapHandler.activity.lifecycleScope.launch { MtSpokaneMapItems.initializeDifficultRunsAsync(this@DialogAdapter.mapHandler.activity::class,
                    this@DialogAdapter.mapHandler).start() }
        }

        if (this.showChairliftImage == null) {
            this.showChairliftImage = view.findViewById(R.id.show_chairlift)
            this.showChairliftImage!!.setOnClickListener {
                MtSpokaneMapItems.chairlifts!!.forEach {
                    it.togglePolyLineVisibility(!it.defaultVisibility, false) // FIXME
                }
            }
        }

        if (this.showEasyRunsImage == null) {
            this.showEasyRunsImage = view.findViewById(R.id.show_easy_runs)
            this.showEasyRunsImage!!.setOnClickListener {
                MtSpokaneMapItems.easyRuns!!.forEach {
                    it.togglePolyLineVisibility(!it.defaultVisibility, false) // FIXME
                }
            }
        }

        if (this.showModerateRunsImage == null) {
            this.showModerateRunsImage = view.findViewById(R.id.show_moderate_runs)
            this.showModerateRunsImage!!.setOnClickListener {
                MtSpokaneMapItems.moderateRuns!!.forEach {
                    it.togglePolyLineVisibility(!it.defaultVisibility, false) // FIXME
                }
            }
        }


        if (this.showDifficultRunsImage == null) {
            this.showDifficultRunsImage = view.findViewById(R.id.show_difficult_runs)
            this.showDifficultRunsImage!!.setOnClickListener {
                MtSpokaneMapItems.difficultRuns!!.forEach {
                    it.togglePolyLineVisibility(!it.defaultVisibility, false) // FIXME
                }
            }
        }

        if (this.showNightRunsButton == null) {
            this.showNightRunsButton = view.findViewById(R.id.show_night_runs)
            this.showNightRunsButton!!.setOnClickListener {
                // TODO
            }
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
}