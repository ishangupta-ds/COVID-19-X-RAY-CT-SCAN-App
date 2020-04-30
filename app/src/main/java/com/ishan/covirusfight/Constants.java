package com.ishan.covirusfight;

public class Constants {
    public static final String TAG = "PyTorchDemo";

    public static String[] GTSRB_CLASSES = new String[]{
            "CORONA POSITIVE",
            "CORONA NEGATIVE",
    };

    public static float[] GTSRB_MEAN = new float[]{0.36f, 0.3243f, 0.3263f};
    public static float[] GTSRB_STD = new float[]{0.2793f, 0.2634f, 0.2675f};
}