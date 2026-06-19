/*
 * MapWrapperLayout.kt
 *
 * cf) http://stackoverflow.com/questions/14123243/google-maps-api-v2-custom-infowindow-like-in-original-android-google-maps/15040761#15040761
 */
package com.github.yuukis.businessmap.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class MapWrapperLayout : RelativeLayout {

    /** Reference to a GoogleMap object */
    private var map: GoogleMap? = null

    /**
     * Vertical offset in pixels between the bottom edge of our InfoWindow
     * and the marker position (by default it's bottom edge too).
     * It's a good idea to use custom markers and also the InfoWindow frame,
     * because we probably can't rely on the sizes of the default marker and frame.
     */
    private var bottomOffsetPixels = 0

    /** A currently selected marker */
    private var marker: Marker? = null

    /**
     * Our custom view which is returned from either the InfoWindowAdapter.getInfoContents
     * or InfoWindowAdapter.getInfoWindow
     */
    private var infoWindow: View? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    /** Must be called before we can route the touch events */
    fun init(map: GoogleMap?, bottomOffsetPixels: Int) {
        this.map = map
        this.bottomOffsetPixels = bottomOffsetPixels
    }

    /**
     * Best to be called from either the InfoWindowAdapter.getInfoContents
     * or InfoWindowAdapter.getInfoWindow.
     */
    fun setMarkerWithInfoWindow(marker: Marker?, infoWindow: View?) {
        this.marker = marker
        this.infoWindow = infoWindow
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        var ret = false
        // Make sure that the infoWindow is shown and we have all the needed references
        val currentMarker = marker
        val currentMap = map
        val currentInfoWindow = infoWindow
        if (currentMarker != null && currentMarker.isInfoWindowShown && currentMap != null && currentInfoWindow != null) {
            // Get a marker position on the screen
            val point = currentMap.projection.toScreenLocation(currentMarker.position)

            // Make a copy of the MotionEvent and adjust it's location
            // so it is relative to the infoWindow left top corner
            val copyEv = MotionEvent.obtain(ev)
            copyEv.offsetLocation(
                (-point.x + currentInfoWindow.width / 2).toFloat(),
                (-point.y + currentInfoWindow.height + bottomOffsetPixels).toFloat()
            )

            // Dispatch the adjusted MotionEvent to the infoWindow
            ret = currentInfoWindow.dispatchTouchEvent(copyEv)
        }
        // If the infoWindow consumed the touch event, then just return true.
        // Otherwise pass this event to the super class and return it's result
        return ret || super.dispatchTouchEvent(ev)
    }
}
