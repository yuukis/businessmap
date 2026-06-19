/*
 * OnInfoWindowElemTouchListener.kt
 *
 * cf) http://stackoverflow.com/questions/14123243/google-maps-api-v2-custom-infowindow-like-in-original-android-google-maps/15040761#15040761
 */
package com.github.yuukis.businessmap.view

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import com.google.android.gms.maps.model.Marker

abstract class OnInfoWindowElemTouchListener(
    private val view: View,
    private val bgDrawableNormal: Drawable,
    private val bgDrawablePressed: Drawable
) : View.OnTouchListener {

    private val handler = Handler()

    private var marker: Marker? = null
    private var pressed = false

    fun setMarker(marker: Marker?) {
        this.marker = marker
    }

    override fun onTouch(vv: View, event: MotionEvent): Boolean {
        if (0 <= event.x && event.x <= view.width && 0 <= event.y && event.y <= view.height) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> startPress()
                // We need to delay releasing of the view a little so it shows the pressed state on the screen
                MotionEvent.ACTION_UP -> handler.postDelayed(confirmClickRunnable, 150)
                MotionEvent.ACTION_CANCEL -> endPress()
                else -> {}
            }
        } else {
            // If the touch goes outside of the view's area
            // (like when moving finger out of the pressed button)
            // just release the press
            endPress()
        }
        return false
    }

    private fun startPress() {
        if (!pressed) {
            pressed = true
            handler.removeCallbacks(confirmClickRunnable)
            setBackground(bgDrawablePressed)
            marker?.showInfoWindow()
        }
    }

    private fun endPress(): Boolean {
        return if (pressed) {
            pressed = false
            handler.removeCallbacks(confirmClickRunnable)
            setBackground(bgDrawableNormal)
            marker?.showInfoWindow()
            true
        } else {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun setBackground(drawable: Drawable) {
        val sdk = Build.VERSION.SDK_INT
        if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackgroundDrawable(drawable)
        } else {
            view.background = drawable
        }
    }

    private val confirmClickRunnable = Runnable {
        if (endPress()) {
            marker?.let { onClickConfirmed(view, it) }
        }
    }

    /**
     * This is called after a successful click
     */
    protected abstract fun onClickConfirmed(v: View, marker: Marker)
}
