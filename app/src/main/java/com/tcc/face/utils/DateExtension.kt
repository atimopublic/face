package com.tcc.face.utils

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log

import java.io.ByteArrayOutputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


fun Long.getFormatTime(): String? {
    val currentTime: Date? = Date(this * 1000)
    val timeZoneDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
    return timeZoneDate.format(currentTime)
}
fun Date.dateToStringdateAndTime() : String
{
    val timeZoneDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
    return timeZoneDate.format(this)

}


fun Bitmap.convertBitmapToBase64() : String
{
    val byteArrayOutputStream = ByteArrayOutputStream()
    // Compress the bitmap to a byte array
    this.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    // Encode the byte array to a Base64 string
    return android.util.Base64.encodeToString(byteArray, Base64.DEFAULT)
}

fun getTodayDate(): String {

    val currentTimeMillis = System.currentTimeMillis()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    val currentDate = Date(currentTimeMillis)

    return dateFormat.format(currentDate)
}

// create fun to check if date is today

fun isDateToday(date: String): Boolean {
    // Define the date format according to the expected input
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Get today's date
    val today = Calendar.getInstance()

    // Create another Calendar instance for the input date
    val calendar = Calendar.getInstance()
    try {
        // Parse the input date string and set it to the calendar
        calendar.time = dateFormat.parse(date) ?: return false
    } catch (e: Exception) {
        e.printStackTrace()
        return false // Return false if parsing fails
    }

    // Compare year and day of year
    return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
}


// create fun to check if date is today with string parameter

fun stringToDateCalendar(dateStr: String, dateFormat: String): Calendar? {
    val simpleDateFormat = SimpleDateFormat(dateFormat)
    try {
        val date = simpleDateFormat.parse(dateStr)
        if (date != null) {
            val calendar = Calendar.getInstance()
            calendar.time = date
            return calendar
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun isdateisMatched(startDate : Calendar,endDate : Calendar) : Boolean
{
    return startDate.time.dateToStringdateAndTime() == endDate.time.dateToStringdateAndTime()
}

fun isEndDateValid(startDate : Calendar,endDate : Calendar): Boolean {
    return  (startDate < endDate && startDate.time.dateToStringdateAndTime() != endDate.time.dateToStringdateAndTime())
 }fun getTodayDateWithTime(): String {





    val currentTimeMillis = System.currentTimeMillis()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)

    val currentDate = Date(currentTimeMillis)

    return dateFormat.format(currentDate)
}
fun getTodayDateWithMonthFormat(): String? {
    val currentTimeMillis = System.currentTimeMillis()

    val dateFormat = SimpleDateFormat("yyyy-MMMM-dd HH:mm", Locale.ENGLISH)

    val currentDate = Date(currentTimeMillis)

    return dateFormat.format(currentDate)
}
fun Long.convertLongToDate(): String {
    val date = Date(this)
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    return format.format(date)
}

fun String.reverseDateFormat(): String {
    val originalFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    val newFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    val date = originalFormat.parse(this)
    return newFormat.format(date)
}

@Throws(ParseException::class)
 fun parseDate(date: String, format: String): String {
    val formatter = SimpleDateFormat(format, Locale.ENGLISH)
    val date1 = formatter.parse(date)
    return formatter.format(date1)
}
//Input string -------	Pattern
//2001.07.04 AD at 12:08:56 ------- PDT	yyyy.MM.dd G 'at' HH:mm:ss z
//Wed, Jul 4, '01	-------     EEE, MMM d, ''yy
//12:08 PM  -------	h:mm a
//12 o'clock PM, Pacific Daylight Time  -------	hh 'o''clock' a, zzzz
//0:08 PM, PDT  -------	K:mm a, z
//02001.July.04 AD 12:08 PM     ------- 	yyyyy.MMMM.dd GGG hh:mm aaa
//Wed, 4 Jul 2001 12:08:56 -0700	-------     EEE, d MMM yyyy HH:mm:ss Z
//2001-07-04T12:08:56.235-0700  ------- 	yyyy-MM-dd'T'HH:mm:ss.SSSZ
//2001-07-04T12:08:56.235-07:00     -------	yyyy-MM-dd'T'HH:mm:ss.SSSXXX
//2001-W27-3    ------- 	YYYY-'W'ww-u
