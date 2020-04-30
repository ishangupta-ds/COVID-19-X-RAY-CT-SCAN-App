package com.ishan.covirusfight;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import org.tensorflow.lite.Interpreter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class TensorFlowClassifier implements Classifier {

    static Interpreter tflite;
    private  static final int IMAGE_MEAN = 120;
    private  static  final float IMAGE_STD = 120.0f;
    private static ByteBuffer imgData = null;
    private static int DIM_IMG_SIZE_X = 224;
    private static int DIM_IMG_SIZE_Y = 224;
    private static int[] intValues;

    private static final float THRESHOLD = 0.1f;
    private List<String> labels;
    private float[][] output=new float[1][2];



    private static List<String> readLabels(AssetManager am, String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(am.open(fileName)));

        String line;
        List<String> labels = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            labels.add(line);
        }

        br.close();
        return labels;
    }


    private static MappedByteBuffer loadModelFile(AssetManager assetManager, String MODEL_FILE) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }



    public static TensorFlowClassifier create(AssetManager assetManager,
                                              String modelPath, String labelFile) throws IOException {

        TensorFlowClassifier c = new TensorFlowClassifier();

        intValues = new int[DIM_IMG_SIZE_X*DIM_IMG_SIZE_Y];

        imgData = ByteBuffer.allocateDirect(3*DIM_IMG_SIZE_Y*DIM_IMG_SIZE_X*4);
        imgData.order(ByteOrder.nativeOrder());

        c.labels = readLabels(assetManager, labelFile);

        try {
            tflite = new Interpreter(loadModelFile(assetManager, modelPath));
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        c.output = new float[1][2];

        return c;
    }

    @Override
    public Classification recognize(Bitmap bitmap) {

        ByteBuffer byteBuffer = convertBitmapToByteBuffer(bitmap);

        tflite.run(byteBuffer, output);

        Classification ans = new Classification();
        for (int i = 0; i < output[0].length; ++i) {
            System.out.println(output[0][i]);
            System.out.println(labels.get(i));
            if (output[0][i] > THRESHOLD && output[0][i] > ans.getConf()) {
                ans.update(output[0][i], labels.get(i));
            }
        }

        return ans;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer;
        byteBuffer = ByteBuffer.allocateDirect(
                4 * 1 * 224 * 224 * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[224 * 224];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < 224; ++i) {
            for (int j = 0; j < 224; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat(
                        (((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                byteBuffer.putFloat(
                        (((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                byteBuffer.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
            }
        }
        return byteBuffer;
    }

    }