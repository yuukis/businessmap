/*
 * ContactsMapFragment.kt
 *
 * Copyright 2013 Yuuki Shimizu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.yuukis.businessmap.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.SparseArray
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.data.MapStatePreferences
import com.github.yuukis.businessmap.model.ContactsItem
import com.github.yuukis.businessmap.util.ContactPhotoLoader
import com.github.yuukis.businessmap.util.GeocoderUtils
import com.github.yuukis.businessmap.view.OnInfoWindowElemTouchListener
import com.github.yuukis.businessmap.widget.MapWrapperLayout
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import java.util.Locale

class ContactsMapFragment :
    SupportMapFragment(),
    GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnMapLongClickListener,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMarkerClickListener,
    OnMapReadyCallback {

    private val viewModel: MainActivityViewModel by activityViewModels()

    private var map: GoogleMap? = null
    private var mapWrapperLayout: MapWrapperLayout? = null
    private var infoWindow: View? = null
    private var infoButton: Button? = null
    private var infoButtonListener: OnInfoWindowElemTouchListener? = null
    private val contactMarkerHashMap = SparseArray<Marker>()
    private val markerContactHashMap = SparseArray<ContactsItem>()
    private val latlngContactsHashMap = SparseArray<MutableList<ContactsItem>>()
    private lateinit var preferences: MapStatePreferences
    private lateinit var contactPhotoLoader: ContactPhotoLoader
    private var longPressMarker: Marker? = null
    private var longPressAddress: String? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        preferences = MapStatePreferences(requireActivity())
        contactPhotoLoader = ContactPhotoLoader(requireContext())
        getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        if (map == null) {
            map = googleMap
            setUpMap()
            setUpMapInfoWindow()
            // The map becomes ready asynchronously, possibly after the
            // Activity already tried (and, since map was still null,
            // failed) to draw markers for the current contacts list.
            // Draw them now that we actually have a map to draw on.
            notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        map?.let {
            val position = it.cameraPosition
            preferences.setCameraPosition(position)
        }

        super.onDestroyView()
    }

    fun notifyDataSetChanged() {
        val currentMap = map ?: return
        val list = getContactsList() ?: return
        val removeMarkerMap = cloneSparseArray(contactMarkerHashMap)

        for (contacts in list) {
            if (removeMarkerMap[contacts.hashCode()] == null) {
                val marker = createMarker(currentMap, contacts)
                if (marker != null) {
                    val position = marker.position
                    val contactList = latlngContactsHashMap[position.hashCode()] ?: ArrayList()
                    if (!contactList.contains(contacts)) {
                        contactList.add(contacts)
                    }

                    contactMarkerHashMap.put(contacts.hashCode(), marker)
                    markerContactHashMap.put(marker.hashCode(), contacts)
                    latlngContactsHashMap.put(position.hashCode(), contactList)
                }
            } else {
                removeMarkerMap.remove(contacts.hashCode())
            }
        }

        // 存在しない連絡先を地図から削除
        for (i in 0 until removeMarkerMap.size()) {
            val key = removeMarkerMap.keyAt(i)
            val marker = removeMarkerMap[key]
            if (marker != null) {
                val contacts = markerContactHashMap[marker.hashCode()]
                if (contacts != null) {
                    contactMarkerHashMap.remove(contacts.hashCode())
                }

                val position = marker.position
                val contactList = latlngContactsHashMap[position.hashCode()] ?: ArrayList()
                contactList.remove(contacts)
                latlngContactsHashMap.put(position.hashCode(), contactList)

                markerContactHashMap.remove(marker.hashCode())
                marker.remove()
            }
        }
        removeMarkerMap.clear()
    }

    fun showMarkerInfoWindow(contact: ContactsItem?, animate: Boolean): Boolean {
        val currentMap = map
        if (currentMap == null || contact == null) {
            return false
        }
        val marker = contactMarkerHashMap[contact.hashCode()] ?: return false
        if (animate) {
            currentMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(marker.position)
                        .zoom(15.5f)
                        .build()
                ),
                object : CancelableCallback {
                    override fun onCancel() {
                    }

                    override fun onFinish() {
                        showContactInfoWindow(marker)
                    }
                }
            )
        } else {
            showContactInfoWindow(marker)
        }
        return true
    }

    override fun onInfoWindowClick(marker: Marker) {
        if (marker == longPressMarker) {
            val position = marker.position
            LocationActionFragment.showDialog(requireActivity(), position.latitude, position.longitude, longPressAddress)
            return
        }
        val contact = markerContactHashMap[marker.hashCode()] ?: return
        ContactsActionFragment.showDialog(requireActivity(), contact)
    }

    override fun onMapLongClick(latLng: LatLng) {
        val currentMap = map ?: return
        removeLongPressMarker()

        val marker = currentMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(getString(R.string.message_geocoding_address))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        ) ?: return
        longPressMarker = marker
        marker.showInfoWindow()

        viewLifecycleOwner.lifecycleScope.launch {
            val address = GeocoderUtils.getFromLocation(requireContext(), latLng.latitude, latLng.longitude)
            if (marker == longPressMarker) {
                longPressAddress = address
                marker.title = address.ifEmpty { getString(R.string.message_geocoding_failed) }
                if (marker.isInfoWindowShown) {
                    marker.showInfoWindow()
                }
            }
        }
    }

    override fun onMapClick(latLng: LatLng) {
        removeLongPressMarker()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if (marker == longPressMarker) {
            return false
        }

        removeLongPressMarker()
        showContactInfoWindow(marker)
        return true
    }

    private fun showContactInfoWindow(marker: Marker) {
        marker.showInfoWindow()
        loadContactPhotoAndRefreshInfoWindow(marker)
    }

    private fun loadContactPhotoAndRefreshInfoWindow(marker: Marker) {
        val contact = markerContactHashMap[marker.hashCode()] ?: return
        if (contactPhotoLoader.isLoadCompleted(contact.cid)) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            contactPhotoLoader.loadThumbnailAsync(contact.cid)
            if (marker.isInfoWindowShown) {
                marker.showInfoWindow()
            }
        }
    }

    private fun removeLongPressMarker() {
        longPressMarker?.remove()
        longPressMarker = null
        longPressAddress = null
    }

    private fun getContactsList(): List<ContactsItem>? = viewModel.currentGroupContactsList.value

    @SuppressLint("MissingPermission")
    private fun setUpMap() {
        val currentMap = map ?: return
        val position = preferences.getCameraPosition()
        currentMap.setInfoWindowAdapter(MyInfoWindowAdapter())
        currentMap.setOnInfoWindowClickListener(this)
        currentMap.setOnMapLongClickListener(this)
        currentMap.setOnMapClickListener(this)
        currentMap.setOnMarkerClickListener(this)
        currentMap.isIndoorEnabled = false
        if (hasLocationPermission()) {
            currentMap.isMyLocationEnabled = true
        }
        currentMap.moveCamera(CameraUpdateFactory.newCameraPosition(position))
    }

    @SuppressLint("MissingPermission")
    fun enableMyLocationIfPermitted() {
        val currentMap = map
        if (currentMap != null && hasLocationPermission()) {
            currentMap.isMyLocationEnabled = true
        }
    }

    private fun hasLocationPermission(): Boolean {
        val context = activity
        return context != null &&
            (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED)
    }

    private fun cloneSparseArray(array: SparseArray<Marker>): SparseArray<Marker> {
        val sdk = Build.VERSION.SDK_INT
        return if (sdk < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            val clone = SparseArray<Marker>()
            for (i in 0 until array.size()) {
                val key = array.keyAt(i)
                val marker = array.valueAt(i)
                clone.put(key, marker)
            }
            clone
        } else {
            array.clone()
        }
    }

    private fun createMarker(map: GoogleMap, contacts: ContactsItem): Marker? {
        val lat = contacts.lat
        val lng = contacts.lng
        if (lat == null || lng == null) {
            return null
        }
        val name = contacts.name ?: getString(R.string.message_no_data)
        val address = contacts.address ?: getString(R.string.message_no_data)
        val latLng = LatLng(lat, lng)
        return map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(name)
                .snippet(address)
        )
    }

    private fun setUpMapInfoWindow() {
        val activity = requireActivity()
        val wrapperLayout = activity.findViewById<MapWrapperLayout>(R.id.map_relative_layout)
        mapWrapperLayout = wrapperLayout
        wrapperLayout.init(map, getPixelsFromDp(activity, (39 + 20).toFloat()))

        val window = activity.layoutInflater.inflate(R.layout.marker_info_contents, null)
        infoWindow = window
        val button = window.findViewById<Button>(R.id.other_count)
        infoButton = button
        val listener = object : OnInfoWindowElemTouchListener(
            button,
            requireNotNull(ContextCompat.getDrawable(activity, R.drawable.infowindow_button_normal)),
            requireNotNull(ContextCompat.getDrawable(activity, R.drawable.infowindow_button_pressed))
        ) {
            override fun onClickConfirmed(v: View, marker: Marker) {
                val position = marker.position
                val contactsList = latlngContactsHashMap[position.hashCode()]
                ContactsItemsDialogFragment.showDialog(requireActivity(), contactsList ?: emptyList())
            }
        }
        infoButtonListener = listener
        button.setOnTouchListener(listener)
    }

    private inner class MyInfoWindowAdapter : InfoWindowAdapter {

        override fun getInfoContents(marker: Marker): View? {
            val contacts = markerContactHashMap[marker.hashCode()]
            val position = marker.position
            val samePositionContacts = latlngContactsHashMap[position.hashCode()]

            val view = infoWindow ?: return null
            val tvTitle = view.findViewById<TextView>(R.id.title)
            val contactPhoto = view.findViewById<ImageView>(R.id.contact_photo)
            val tvCompanyName = view.findViewById<TextView>(R.id.company_name)
            val tvSnippet = view.findViewById<TextView>(R.id.snippet)
            val tvNote = view.findViewById<TextView>(R.id.note)
            val btnOtherCount = infoButton
            val separator = view.findViewById<View>(R.id.separator)
            infoButtonListener?.setMarker(marker)

            if (marker == longPressMarker) {
                contactPhoto.setImageDrawable(null)
                contactPhoto.visibility = View.GONE
                tvTitle.text = marker.title
                tvCompanyName.visibility = View.GONE
                tvSnippet.visibility = View.GONE
                separator.visibility = View.GONE
                tvNote.visibility = View.GONE
                btnOtherCount?.visibility = View.GONE

                mapWrapperLayout?.setMarkerWithInfoWindow(marker, view)
                return view
            }

            if (contacts != null) {
                val photo = contactPhotoLoader.getCachedThumbnail(contacts.cid)
                if (photo != null) {
                    contactPhoto.setImageBitmap(photo)
                    contactPhoto.visibility = View.VISIBLE
                } else if (contactPhotoLoader.isLoadCompleted(contacts.cid)) {
                    contactPhoto.setImageDrawable(null)
                    contactPhoto.visibility = View.GONE
                } else {
                    contactPhoto.setImageResource(R.drawable.contact_photo_placeholder)
                    contactPhoto.visibility = View.VISIBLE
                }

                val title = marker.title
                tvTitle.text = title

                val companyName = contacts.companyName
                if (TextUtils.isEmpty(companyName)) {
                    tvCompanyName.visibility = View.GONE
                } else {
                    tvCompanyName.text = companyName
                    tvCompanyName.visibility = View.VISIBLE
                }

                val snippet = marker.snippet?.replace("[ 　]".toRegex(), "\n")
                tvSnippet.text = snippet
                tvSnippet.visibility = View.VISIBLE

                val note = contacts.note
                if (TextUtils.isEmpty(note)) {
                    separator.visibility = View.GONE
                    tvNote.visibility = View.GONE
                } else {
                    tvNote.text = note
                    separator.visibility = View.VISIBLE
                    tvNote.visibility = View.VISIBLE
                }

                if (samePositionContacts != null && samePositionContacts.size > 1) {
                    val otherCount = String.format(
                        Locale.getDefault(),
                        getString(R.string.message_other_items),
                        samePositionContacts.size - 1
                    )
                    btnOtherCount?.text = otherCount
                    btnOtherCount?.visibility = View.VISIBLE
                } else {
                    btnOtherCount?.visibility = View.GONE
                }
            }

            mapWrapperLayout?.setMarkerWithInfoWindow(marker, view)

            return view
        }

        override fun getInfoWindow(marker: Marker): View? {
            return null
        }
    }

    companion object {
        private fun getPixelsFromDp(context: Context, dp: Float): Int {
            val scale = context.resources.displayMetrics.density
            return (dp * scale + 0.5f).toInt()
        }
    }
}
