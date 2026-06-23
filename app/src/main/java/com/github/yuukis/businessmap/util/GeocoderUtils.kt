package com.github.yuukis.businessmap.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object GeocoderUtils {

    @JvmStatic
    @Throws(IOException::class, JSONException::class)
    suspend fun getFromLocationName(context: Context, address: String): Array<Double?> {
        return try {
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getFromLocationNameAsync(context, address)
            } else {
                getFromLocationNameSync(context, address)
            }
            var lat = Double.NaN
            var lng = Double.NaN
            if (!list.isNullOrEmpty()) {
                lat = list[0].latitude
                lng = list[0].longitude
            }
            arrayOf(lat, lng)
        } catch (e: IOException) {
            getFromLocationNameToGoogleMaps(address)
        }
    }

    @Suppress("DEPRECATION")
    private fun getFromLocationNameSync(context: Context, address: String): List<Address>? =
        Geocoder(context, Locale.getDefault()).getFromLocationName(address, 1)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun getFromLocationNameAsync(context: Context, address: String): List<Address> =
        suspendCancellableCoroutine { cont ->
            Geocoder(context, Locale.getDefault()).getFromLocationName(
                address,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (cont.isActive) {
                            cont.resume(addresses)
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        if (cont.isActive) {
                            cont.resumeWithException(IOException(errorMessage ?: "Geocoding failed"))
                        }
                    }
                }
            )
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
