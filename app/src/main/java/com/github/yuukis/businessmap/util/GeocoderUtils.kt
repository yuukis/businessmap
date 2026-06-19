package com.github.yuukis.businessmap.util

import android.content.Context
import android.location.Geocoder
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

object GeocoderUtils {

    @JvmStatic
    @Throws(IOException::class, JSONException::class)
    fun getFromLocationName(context: Context, address: String): Array<Double?> {
        var lat = Double.NaN
        var lng = Double.NaN

        return try {
            val list = Geocoder(context, Locale.getDefault()).getFromLocationName(address, 1)
            if (list != null && list.isNotEmpty()) {
                val addr = list[0]
                lat = addr.latitude
                lng = addr.longitude
            }
            arrayOf(lat, lng)
        } catch (e: IOException) {
            getFromLocationNameToGoogleMaps(address)
        }
    }

    @Throws(IOException::class, JSONException::class)
    private fun getFromLocationNameToGoogleMaps(address: String): Array<Double?> {
        val urlFormat = "https://maps.google.com/maps/api/geocode/json?address=%s&ka&sensor=false"
        val encodedAddress = URLEncoder.encode(address, "UTF-8")
        val url = String.format(Locale.getDefault(), urlFormat, encodedAddress)
        val stringBuilder = StringBuilder()

        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            val stream = connection.inputStream
            var b: Int
            while (stream.read().also { b = it } != -1) {
                stringBuilder.append(b.toChar())
            }
        } finally {
            connection.disconnect()
        }

        val jsonObject = JSONObject(stringBuilder.toString())
        var lat = Double.NaN
        var lng = Double.NaN
        try {
            lat = (jsonObject.get("results") as JSONArray)
                .getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONObject("location")
                .getDouble("lat")
            lng = (jsonObject.get("results") as JSONArray)
                .getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONObject("location")
                .getDouble("lng")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return arrayOf(lat, lng)
    }
}
