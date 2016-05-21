/*
 * OnInfoWindowElemTouchListener.java
 *
 * cf) http://stackoverflow.com/questions/14123243/google-maps-api-v2-custom-infowindow-like-in-original-android-google-maps/15040761#15040761
 */
package com.github.yuukis.businessmap.view;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.google.android.gms.maps.model.Marker;

public abstract class OnInfoWindowElemTouchListener implements OnTouchListener {
    private final View view;
    private final Drawable bgDrawableNormal;
    private final Drawable bgDrawablePressed;
    private final Handler handler = new Handler();

    private Marker marker;
    private boolean pressed = false;

    public OnInfoWindowElemTouchListener(View view, Drawable bgDrawableNormal, Drawable bgDrawablePressed) {
        this.view = view;
        this.bgDrawableNormal = bgDrawableNormal;
        this.bgDrawablePressed = bgDrawablePressed;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    @Override
    public boolean onTouch(View vv, MotionEvent event) {
        if (0 <= event.getX() && event.getX() <= view.getWidth() &&
            0 <= event.getY() && event.getY() <= view.getHeight())
        {
            switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: startPress(); break;

            // We need to delay releasing of the view a little so it shows the pressed state on the screen
            case MotionEvent.ACTION_UP: handler.postDelayed(confirmClickRunnable, 150); break;

            case MotionEvent.ACTION_CANCEL: endPress(); break;
            default: break;
            }
        }
        else {
            // If the touch goes outside of the view's area
            // (like when moving finger out of the pressed button)
            // just release the press
            endPress();
        }
        return false;
    }

    private void startPress() {
        if (!pressed) {
            pressed = true;
            handler.removeCallbacks(confirmClickRunnable);
            setBackground(bgDrawablePressed);
            if (marker != null)
                marker.showInfoWindow();
        }
    }

    private boolean endPress() {
        if (pressed) {
            this.pressed = false;
            handler.removeCallbacks(confirmClickRunnable);
            setBackground(bgDrawableNormal);
            if (marker != null)
                marker.showInfoWindow();
            return true;
        }
        else
            return false;
    }
    
    @SuppressWarnings("deprecation")
	private void setBackground(Drawable drawable) {
    	int sdk = Build.VERSION.SDK_INT;
    	if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
    	    view.setBackgroundDrawable(drawable);
    	} else {
    	    view.setBackground(drawable);
    	}
    }

    private final Runnable confirmClickRunnable = new Runnable() {
        public void run() {
            if (endPress()) {
                onClickConfirmed(view, marker);
            }
        }
    };

    /**
     * This is called after a successful click
     */
    protected abstract void onClickConfirmed(View v, Marker marker);
}