/*
 * DrawableUtils.java
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
package com.github.yuukis.businessmap.util;

import com.github.yuukis.businessmap.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;

public class DrawableUtils {

    private static final float DEFAULT_S = 1.0f;
    private static final float DEFAULT_V = 1.0f;

    public static Drawable getCircleDrawable(Context context, float hue) {
        float[] hsv = new float[] {hue, DEFAULT_S, DEFAULT_V};
        return getCircleDrawable(context, hsv);
    }

    private static Drawable getCircleDrawable(Context context, float[] hsv) {
        Resources resources = context.getResources();
        Drawable oval = getOvalDrawable(context, hsv);
        Drawable wrap = resources.getDrawable(R.drawable.shape_circle_wrap);
        Drawable[] layer = new Drawable[] {oval, wrap};
        Drawable drawable = new LayerDrawable(layer);
        return drawable;
    }

    private static Drawable getOvalDrawable(Context context, float[] hsv) {
        int color = Color.HSVToColor(hsv);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }
}  