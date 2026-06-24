package com.github.yuukis.businessmap.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object GeocoderUtils {

    @JvmStatic
    @Throws(IOException::class)
    suspend fun getFromLocationName(context: Context, address: String): Array<Double?> {
        val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getFromLocationNameAsync(context, address)
        } else {
            withContext(Dispatchers.IO) { getFromLocationNameSync(context, address) }
        }
        var lat = Double.NaN
        var lng = Double.NaN
        if (!list.isNullOrEmpty()) {
            lat = list[0].latitude
            lng = list[0].longitude
        }
        return arrayOf(lat, lng)
    }

    @Suppress("DEPRECATION")
    private fun getFromLocationNameSync(context: Context, address: String): List<Address>? =
        Geocoder(context, Locale.getDefault()).getFromLocationName(address, 1)

    @JvmStatic
    suspend fun getFromLocation(context: Context, lat: Double, lng: Double): String {
        return try {
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getFromLocationAsync(context, lat, lng)
            } else {
                withContext(Dispatchers.IO) { getFromLocationSync(context, lat, lng) }
            }
            list?.firstOrNull()?.getAddressLine(0).orEmpty()
        } catch (e: IOException) {
            ""
        } catch (e: IllegalArgumentException) {
            ""
        }
    }

    @Suppress("DEPRECATION")
    private fun getFromLocationSync(context: Context, lat: Double, lng: Double): List<Address>? =
        Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun getFromLocationAsync(context: Context, lat: Double, lng: Double): List<Address> =
        suspendCancellableCoroutine { cont ->
            Geocoder(context, Locale.getDefault()).getFromLocation(
                lat,
                lng,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (cont.isActive) {
                            cont.resume(addresses)
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        if (cont.isActive) {
                            cont.resumeWithException(IOException(errorMessage ?: "Reverse geocoding failed"))
                        }
                    }
                }
            )
        }

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
}
