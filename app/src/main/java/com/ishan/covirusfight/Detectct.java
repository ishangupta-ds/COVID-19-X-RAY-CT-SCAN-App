package com.ishan.covirusfight;


import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Detectct extends AppCompatActivity {

    private static final int IMG_SIZE = 224;
    private FloatingActionButton btn;
    private Button predB;
    private ImageView imageview;
    private TextView resText;
    private static final String IMAGE_DIRECTORY = "/CTDetections";
    private int GALLERY = 1, CAMERA = 2;
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    private String TAG = "DETECTCT ACTIVITY";
    boolean doubleBackToExitPressedOnce = false;
    private Uri fileUri;
    private List<Classifier> mClassifiers = new ArrayList<>();
    private static final int IMAGE_MEAN = 0;
    private static final float IMAGE_STD = 255;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detectct);

        imageview = findViewById(R.id.iv);

        FloatingActionButton btnctx = findViewById(R.id.btnct);
        btnctx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPictureDialog();
            }
        });

        predB = (Button) findViewById(R.id.predictBtn);
        resText = (TextView) findViewById(R.id.takenPic);
        checkAndRequestPermissions();
        loadClassifier();

    }
    private void loadClassifier(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {mClassifiers.add(
                        TensorFlowClassifier.create(getAssets(),
                                "model.tflite", "labels.txt"));
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing classifiers!", e);
                }
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        LinearLayout lL1 = findViewById(R.id.first);
        LinearLayout lL = findViewById(R.id.ImageLay);
        if(lL.getVisibility() == View.VISIBLE){
            lL1.setVisibility(View.VISIBLE);
            lL.setVisibility(View.GONE);
            resText.setText("");
        }
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press Back again to exit.", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);


    }

    private  void checkAndRequestPermissions() {
        int camerapermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int writepermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);


        List<String> listPermissionsNeeded = new ArrayList<>();

        if (camerapermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (writepermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "Permission callback called-------");
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {

                Map<String, Integer> perms = new HashMap<>();
                // Initialize the map with both permissions
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for both permissions
                    if (perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "sms & location services permission granted");

                        recreate();
                        //else any one or both the permissions are not granted
                    } else {
                        Log.d(TAG, "Some permissions are not granted ask again ");
                        //permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
                        // shouldShowRequestPermissionRationale will return true
                        //show the dialog saying its necessary and try again otherwise proceed with setup.
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            showDialogOK("Service Permissions are required for this app",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    checkAndRequestPermissions();
                                                    break;
                                                case DialogInterface.BUTTON_NEGATIVE:
                                                    // proceed with logic by disabling the related features or quit the app.
                                                    finish();
                                                    break;
                                            }
                                        }
                                    });
                        }
                        //permission is denied (and never ask again is  checked)
                        //shouldShowRequestPermissionRationale will return false
                        else {
                            explain("You need to give some mandatory permissions to continue. Do you want to go to app settings?");
                            //                            //proceed with logic by disabling the related features or quit the app.
                        }
                    }
                }
            }
        }

    }

    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }

    private void explain(String msg){
        final androidx.appcompat.app.AlertDialog.Builder dialog = new androidx.appcompat.app.AlertDialog.Builder(this);
        dialog.setMessage(msg)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        //  permissionsclass.requestPermission(type,code);
                        startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.ishan.covirusfight")));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        finish();
                    }
                });
        dialog.show();
    }


    private void showPictureDialog(){
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {
                "Select Photo",
                "Take Photo" };
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                choosePhotoFromGallery();
                                break;
                            case 1:
                                takePhotoFromCamera();
                                break;
                        }
                    }
                });
        pictureDialog.show();
    }

    public void choosePhotoFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALLERY);
    }

    private void takePhotoFromCamera() {

        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();
                try {
                    final Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                    LinearLayout lL1 = findViewById(R.id.first);
                    lL1.setVisibility(View.GONE);
                    LinearLayout lL = findViewById(R.id.ImageLay);
                    lL.setVisibility(View.VISIBLE);
                    imageview.setImageBitmap(bitmap);
                    predB.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            resText.setText("");
                            loadModel(bitmap);
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(Detectct.this, "Failed!", Toast.LENGTH_SHORT).show();
                }
            }

        }


        else if (requestCode == CAMERA) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;
            final Bitmap bitmap;
            System.out.println("file uri= "+fileUri);
            bitmap = (Bitmap) data.getExtras().get("data");
            LinearLayout lL1 = findViewById(R.id.first);
            lL1.setVisibility(View.GONE);
            LinearLayout lL = findViewById(R.id.ImageLay);
            lL.setVisibility(View.VISIBLE);
            predB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resText.setText("");
                    loadModel(bitmap);
                }
            });
            imageview.setImageBitmap(bitmap);


            Toast.makeText(Detectct.this, "Image Saved!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadModel(Bitmap bitmap) {

        if(bitmap.getWidth() != IMG_SIZE || bitmap.getHeight() != IMG_SIZE)   bitmap = Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, false);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        float[] retPixels = new float[width * height * 3];

        for (int i = 0; i < pixels.length; i++) {
            final int val = pixels[i];
            retPixels[i * 3] = (((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            retPixels[i * 3 + 1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            retPixels[i * 3 + 2] = ((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
        }


        String text = "";
        for (Classifier classifier : mClassifiers) {
            //perform classification on the image
            final Classification res = classifier.recognize(bitmap);
            //if it can't classify, output a question mark
            if (res.getLabel() == null) {
                text += "No Prediction\n";
            } else {
                //else output its name
                text += String.format(Locale.US, "%s with Score: %s", res.getLabel(),res.getConf());
            }
        }
        resText.setText(text);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save file url in bundle as it will be null if screen orientation changes

        outState.putParcelable("file_uri", fileUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // get the file url

        fileUri = savedInstanceState.getParcelable("file_uri");
    }

}