package com.ishan.covirusfight;

import android.graphics.Bitmap;

public interface Classifier {
    Classification recognize(final Bitmap pixels);
}