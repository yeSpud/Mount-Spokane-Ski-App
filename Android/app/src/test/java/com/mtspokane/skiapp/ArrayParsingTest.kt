package com.mtspokane.skiapp

import com.mtspokane.skiapp.activities.activitysummary.ActivitySummary
import com.mtspokane.skiapp.databases.SkiingActivity
import org.junit.Assert
import org.junit.Test

class ArrayParsingTest {

    @Test
    fun intArrayParseTest() {

        val inArray: ArrayList<Int> = ArrayList(8)
        inArray.add(0)
        inArray.add(1)
        inArray.add(1)
        inArray.add(2)
        inArray.add(1)
        inArray.add(1)
        inArray.add(1)
        inArray.add(0)

        val outArray: ArrayList<Int> = this.parseIntArrayRecursively(inArray)
        Assert.assertEquals(5, outArray.size)

        Assert.assertEquals(0, outArray[0])
        Assert.assertEquals(1, outArray[1])
        Assert.assertEquals(2, outArray[2])
        Assert.assertEquals(1, outArray[3])
        Assert.assertEquals(0, outArray[4])
    }

    @Test
    fun skiingActivityArrayParseTest() {

        val mySkiingActivities: ArrayList<SkiingActivity> = ArrayList()


        // Get test activities.

        //ActivitySummary.parseMapMarkersForMap()// TODO Test

    }

    /*
     * Proof of concept.
     */
    private fun parseIntArrayRecursively(arrayList: ArrayList<Int>): ArrayList<Int> {

        val currentArray: ArrayList<Int> = ArrayList()
        val initial:Int = arrayList.removeFirst()
        var final: Int? = null

        while(arrayList.isNotEmpty()) {
            if (arrayList[0] == initial) {
                final = arrayList.removeFirst()
            } else {
                break
            }
        }
        currentArray.add(initial)

        return if (arrayList.isEmpty()) {

            currentArray
        } else {
            currentArray.addAll(this.parseIntArrayRecursively(arrayList))
            currentArray
        }
    }
}