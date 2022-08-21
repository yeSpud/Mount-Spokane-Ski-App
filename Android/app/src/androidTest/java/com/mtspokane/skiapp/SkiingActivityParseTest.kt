package com.mtspokane.skiapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mtspokane.skiapp.databases.SkiingActivity
import com.mtspokane.skiapp.databases.SkiingActivityManager
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class SkiingActivityParseTest {

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

		val mySkiingActivities: Array<SkiingActivity> = SkiingActivityManager.jsonArrayToSkiingActivities(json.getJSONArray("2022-01-11"))
		Assert.assertNotEquals(0, mySkiingActivities.size)
		Assert.assertEquals(1552, mySkiingActivities.size)

		//ActivitySummary.parseMapMarkersForMap()// TODO Test
	}
}