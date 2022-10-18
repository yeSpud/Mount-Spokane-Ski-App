package com.mtspokane.skiapp

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.android.gms.maps.MapsInitializer
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummary
import com.mtspokane.skiapp.activities.activitysummary.ActivitySummary.ActivitySummaryEntry
import com.mtspokane.skiapp.databases.SkiingActivity
import com.mtspokane.skiapp.databases.SkiingActivityManager
import com.mtspokane.skiapp.mapItem.MapMarker
import com.mtspokane.skiapp.mapItem.MtSpokaneMapItems
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream

@RunWith(AndroidJUnit4::class)
class SkiingActivityParseTest {

	private lateinit var device: UiDevice

	private lateinit var context: Context

	@Before
	fun prepareUnitTest() {

		// Initialize UiDevice instance.
		this.device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

		// Start from the home screen.
		this.device.pressHome()

		// Wait for the launcher.
		val launcherName: String = this.device.launcherPackageName
		Assert.assertNotNull(launcherName)
		this.device.wait(Until.hasObject(By.pkg(launcherName).depth(0)), TIMEOUT)

		// Launch the activity to the activity summary view.
		this.context = ApplicationProvider.getApplicationContext()
		val intent: Intent? = this.context.packageManager.getLaunchIntentForPackage(SAMPLE_PACKAGE)
		Assert.assertNotNull(intent)
		intent!!.apply {

			// Be sure to clear out any previous package instances.
			addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
		}
		this.context.startActivity(intent)

		// Wait for the activity to launch.
		this.device.wait(Until.hasObject(By.pkg(SAMPLE_PACKAGE).depth(0)), TIMEOUT)
	}

	@Test
	fun skiingActivityArrayParseTest() {

		/*
		val fooJson = JSONObject()
		val testJson = JSONObject("{'test':'foo'}")
		val asdf = testJson.getString("test")
		*/

		//val file = File("src/test/java/com/mtspokane/skiapp/example.json")
		if (this.javaClass.classLoader == null) {
			Assert.fail("Class loader is null")
		}

		val inputStream: InputStream? = this.javaClass.classLoader!!.getResourceAsStream("example.json")
		if (inputStream == null) {
			Assert.fail("Could not find example json file")
		}

		val json: JSONObject = inputStream!!.bufferedReader().useLines {

			val string = it.fold("") { _, inText -> inText }

			JSONObject(string)
		}
		inputStream.close()

		val skiingActivities: Array<SkiingActivity> = SkiingActivityManager.jsonArrayToSkiingActivities(json.getJSONArray("2022-01-11"))
		Assert.assertNotEquals(0, skiingActivities.size)
		Assert.assertEquals(1552, skiingActivities.size)

		// Load the bitmap descriptor.
		//MapsInitializer.initialize(InstrumentationRegistry.getInstrumentation().targetContext)

		//MtSpokaneMapItems.checkoutObject(this::class)

		val mapMarkers: Array<MapMarker> = MapMarker.loadFromSkiingActivityArray(skiingActivities)
		//val unprocessedSummaries: Array<ActivitySummaryEntry> = ActivitySummaryEntry.loadAllFromMapMarkers(mapMarkers)
		//val processedSummaries: Array<ActivitySummaryEntry> = ActivitySummaryEntry.crushDownActivities(unprocessedSummaries)
		//Assert.assertNotEquals(unprocessedSummaries.size, processedSummaries.size)

		//MtSpokaneMapItems.destroyUIItems(this::class)
	}

	companion object {
		private const val TIMEOUT = 5000L
		private const val SAMPLE_PACKAGE: String = "com.mtspokane.skiapp.activities.activitysummary.ActivitySummary"  // FIXME Find correct package name
	}
}
