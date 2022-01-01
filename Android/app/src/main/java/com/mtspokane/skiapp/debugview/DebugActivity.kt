package com.mtspokane.skiapp.debugview

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.mtspokane.skiapp.databinding.ActivityDebugBinding


class DebugActivity : FragmentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val binding = ActivityDebugBinding.inflate(this.layoutInflater)
		this.setContentView(binding.root)

		this.actionBar!!.setDisplayShowTitleEnabled(true)

	}

}