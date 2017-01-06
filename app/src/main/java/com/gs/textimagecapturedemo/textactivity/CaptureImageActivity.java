package com.gs.textimagecapturedemo.textactivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chirag.textimagecapturedemo.BuildConfig;
import com.example.chirag.textimagecapturedemo.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.gs.textimagecapturedemo.CameraSource;
import com.gs.textimagecapturedemo.CameraSourcePreview;
import com.gs.textimagecapturedemo.GraphicOverlay;
import com.gs.textimagecapturedemo.OcrDetectorProcessor;
import com.gs.textimagecapturedemo.OcrGraphic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CaptureImageActivity extends AppCompatActivity {

    private static final String TAG = "OcrCaptureActivity";

    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String TextBlockObject = "String";

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    LinearLayout topLayout;

    int previewWidth;
    int previewHeight;

    LinearLayout startbt,capturebt,stopbt;

    private String recognizedText;
    private int removedkey;

    File file;
    public static String _path;
    ImageView cropImageView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.activity_main);


        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<OcrGraphic>) findViewById(R.id.graphicOverlay);

        previewWidth = getWindowManager().getDefaultDisplay().getWidth();
        previewHeight = getWindowManager().getDefaultDisplay().getHeight() - 50;

        if (Build.VERSION.SDK_INT >= 23) {

            int result;
            List<String> listPermissionsNeeded = new ArrayList<>();
            for (String p : mypermissions) {
                result = ContextCompat.checkSelfPermission(this, p);
                if (result != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(p);
                }
            }
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),111);

            }else{

                boolean autoFocus = true;
                boolean useFlash = false;
                // Check for the camera permission before accessing the camera.  If the
                // permission is not granted yet, request permission.
                createCameraSource(autoFocus, useFlash);
                startCameraSource();
            }

        }else {

            boolean autoFocus = true;
            boolean useFlash = false;

        }

        topLayout = (LinearLayout)findViewById(R.id.topLayout);
        startbt = (LinearLayout) findViewById(R.id.startbt);
        capturebt = (LinearLayout)findViewById(R.id.capturebt);
        stopbt = (LinearLayout)findViewById(R.id.stopbt);

        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        String APP_FOLDER = extStorageDirectory + "/CustomOcrApp/";
        File wallpaperDirectory = new File(APP_FOLDER + "/");
        wallpaperDirectory.mkdirs();
        _path = wallpaperDirectory + "/temp.jpg";
//        if (new File(_path).exists()) {
//            new File(_path).delete();
//        }



        startbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    file = new File(_path);
                    Uri outputFileUri = Uri.fromFile(file);
                    Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                    intent.putExtra("output", outputFileUri);
                    startActivityForResult(intent, 111);
              }
        });

        capturebt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {





        }});

        stopbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();

            }
        });

    }


    private void scanText(final Bitmap imageview) {

        new AsyncTask<Void, Boolean, Boolean>() {
            Bitmap croppedbitmap;
            ProgressDialog pd;

            {
                this.pd = null;
                this.croppedbitmap = null;
            }

            protected void onPostExecute(Boolean result) {
                try {
                    this.pd.dismiss();
                } catch (Exception e) {
                }

                showDialogBox(CaptureImageActivity.this,CaptureImageActivity.this.recognizedText);

                super.onPostExecute(result);
            }

            protected void onPreExecute() {

                this.croppedbitmap = /*BitmapFactory.decodeResource(getResources(),R.drawable.ocr_test);*/imageview;
//                this.croppedbitmap = imageview.getCroppedImage();
                this.pd = new ProgressDialog(CaptureImageActivity.this);
                this.pd.setMessage("Detecting text ..\nPlease wait....");
                this.pd.setCanceledOnTouchOutside(false);
                this.pd.setCancelable(false);
                this.pd.show();
//                Toast.makeText(MainActivity.this, "Language selected to OCR process :\n   " + MainActivity.this.langoption.getText(), 1).show();
                super.onPreExecute();
            }

            protected Boolean doInBackground(Void... arg0) {

                String lang = CaptureImageActivity.this.getSharedPreferences("settings", 0).getString("lang", "eng");

                TextRecognizer treg = new TextRecognizer.Builder(CaptureImageActivity.this).build();
                if (treg.isOperational()) {
                        SparseArray<TextBlock> str = treg.detect(new Frame.Builder().setBitmap(this.croppedbitmap).build());

                        ArrayList<TextBlock> textbloackarr = new ArrayList();

                        while (str.size() > 0) {

                            TextBlock textBlock = CaptureImageActivity.this.getTopOne(str);
                            textbloackarr.add(textBlock);
                            str.remove(CaptureImageActivity.this.removedkey);

                        }


                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < textbloackarr.size(); i++) {
                            sb.append(((TextBlock) textbloackarr.get(i)).getValue());
                            sb.append("\n");
                        }

                        treg.release();
                        CaptureImageActivity.this.recognizedText = sb.toString();
                        Log.e(BuildConfig.FLAVOR, "API USED:GOOGLEVISIONAPI");

                }


                return true;

            }
        }.execute(new Void[0]);
    }




    public static void showDialogBox(Context mcontext,String msg){

        try {

            final Dialog dialog = new Dialog(mcontext);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            // dialog.setTitle("Edit Profile!");
            dialog.setContentView(R.layout.alert_dialog);

            int width = ((Activity)mcontext).getWindowManager().getDefaultDisplay()
                    .getWidth();

            if (width > 480) {

                width = ((width*2)/3)+70;
                dialog.getWindow().setLayout(width, ActionBar.LayoutParams.WRAP_CONTENT);

            }


            dialog.setCancelable(true);

            TextView txtview_logout_confirm = (TextView) dialog
                    .findViewById(R.id.txtview_alert);

            txtview_logout_confirm.setText(msg);

            final Button button_yes = (Button) dialog
                    .findViewById(R.id.button_ok_alert);


            button_yes.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    dialog.dismiss();

                }

            });


            dialog.show();

        } catch (Exception e) {

            e.printStackTrace();

        }


    }

    private TextBlock getTopOne(SparseArray<TextBlock> sparearry) {

        TextBlock topone = (TextBlock) sparearry.get(sparearry.keyAt(0));
        int top = ((TextBlock) sparearry.get(sparearry.keyAt(0))).getBoundingBox().top;
        int left = ((TextBlock) sparearry.get(sparearry.keyAt(0))).getBoundingBox().left;

        int right = ((TextBlock) sparearry.get(sparearry.keyAt(0))).getBoundingBox().right;
        int bottom = ((TextBlock) sparearry.get(sparearry.keyAt(0))).getBoundingBox().bottom;


        this.removedkey = sparearry.keyAt(0);
        for (int i = 1; i < sparearry.size(); i++) {
            Rect rc = ((TextBlock) sparearry.get(sparearry.keyAt(i))).getBoundingBox();
            if (rc.top < top) {
                topone = (TextBlock) sparearry.get(sparearry.keyAt(i));
                this.removedkey = sparearry.keyAt(i);
                top = rc.top;
                left = rc.left;


            }
        }

        return topone;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            //user is returning from capturing an image using the camera
            if(requestCode == 111){

                new SaveImageTask(new File(_path)).execute();

            }
       }
    }


    @Override
    public boolean onTouchEvent(MotionEvent e) {
//        boolean b = scaleGestureDetector.onTouchEvent(e);

//        boolean c = gestureDetector.onTouchEvent(e);

        return /*b || c || */super.onTouchEvent(e);
    }


    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();

        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
        textRecognizer.setProcessor(new OcrDetectorProcessor(mGraphicOverlay));

        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies are not yet available.");

            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }


        mCameraSource =
                new CameraSource.Builder(getApplicationContext(), textRecognizer)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(previewWidth, previewHeight)
                        .setRequestedFps(1.0f)
                        .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                        .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_FIXED : null)
                        .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    String[] mypermissions= new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            /*Manifest.permission.CAMERA,*/
            Manifest.permission.READ_EXTERNAL_STORAGE,
           /* Manifest.permission.READ_CONTACTS*/
            Manifest.permission.CAMERA
    };


    String[] mypermissionsDenied = new String[]{
            "Permission Denied to write on external storage",
          /*  "Permission Denied to use camera",*/
            "Permission Denied to read from external storage",
           /* "Permission Denied to read contacts"*/
            "Permission Denied to open camera",
    };

    private  void checkPermissions() {

        if (Build.VERSION.SDK_INT >= 23) {

            int result;
            List<String> listPermissionsNeeded = new ArrayList<>();
            for (String p : mypermissions) {
                result = ContextCompat.checkSelfPermission(this, p);
                if (result != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(p);
                }
            }
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),111);

            }else{

                boolean autoFocus = true;
                boolean useFlash = false;
                createCameraSource(autoFocus, useFlash);
                startCameraSource();
            }

        }else{

            boolean autoFocus = true;
            boolean useFlash = false;
            createCameraSource(autoFocus, useFlash);
            startCameraSource();

        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean isPermissionGranted = true;

        switch(requestCode) {

            case 111:

                if (grantResults != null && grantResults.length == mypermissions.length) {

                    for (int i = 0; i < grantResults.length; i++) {

                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {

                            Log.i("PERMISSION_DENIED",mypermissions[i]);

                            isPermissionGranted = false;
                            Toast.makeText(CaptureImageActivity.this, mypermissionsDenied[i],1).show();

                        }else{

                            Log.i("PERMISSION_GRANTED",mypermissions[i]);

                        }
                    }
                }
                break;
        }


        if(isPermissionGranted){

            boolean autoFocus = getIntent().getBooleanExtra(AutoFocus,false);
            boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
            createCameraSource(autoFocus, useFlash);
            startCameraSource();

        }else{

            showDialogBox(CaptureImageActivity.this,"please install again and set permission manually");
        }
    }

    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }


    public class SaveImageTask extends AsyncTask<Void,Void, Void>{

        File tempFile,mFile;
        public SaveImageTask(File ff){
            mFile = ff;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            hideProgress();
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();

            Bitmap bitmap = BitmapFactory.decodeFile(tempFile.getAbsolutePath(),bitmapOptions);
            scanText(bitmap);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
	    	showProgress();
        }
        @Override
        protected Void doInBackground(Void... params) {

            tempFile = rotateImage(mFile);
            return null;

        }
    }

    int rotate;
    public File rotateImage(File uri){
        File file = uri;
        try{
            try {

                ExifInterface exif = new ExifInterface(
                        file.getAbsolutePath());
                int orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);
                Log.v("my", "Exif orientation: " + orientation);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        Log.v("", "rotated " +270);
                        rotate = 270;
                        Log.e("rotate", ""+rotate);
                        ImageOrientation(file,rotate);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        Log.v("", "rotated " +180);
                        rotate = 180;
                        Log.e("rotate", ""+rotate);
                        ImageOrientation(file,rotate);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_90:
                        Log.v("", "rotated " +90);
                        rotate = 90;
                        ImageOrientation(file,rotate);
                        break;

                    case 1:
                        Log.v("", "rotated1-" +90);
                        rotate = 90;
                        ImageOrientation(file,rotate);
                        break;

                    case 2:
                        Log.v("", "rotated1-" +0);
                        rotate = 0;
                        ImageOrientation(file,rotate);
                        break;
                    case 4:
                        Log.v("", "rotated1-" +180);
                        rotate = 180;
                        ImageOrientation(file,rotate);
                        break;

                    case 0:
                        Log.v("", "rotated 0-" +90);
                        rotate = 90;
                        ImageOrientation(file,rotate);
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        catch(Exception e){
            Log.e("Error - ", e.getMessage());
        }
        return file;
    }

    private void ImageOrientation(File file,int rotate){
        try {
            FileInputStream fis = new FileInputStream(file);
            Bitmap photo = BitmapFactory.decodeStream(fis);
            Matrix matrix = new Matrix();
            matrix.preRotate(rotate); // clockwise by 90 degrees
            photo = Bitmap.createBitmap(photo , 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
            byte[] bitmapdata = bos.toByteArray();

            //write the bytes in file
            FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(bitmapdata);
                fos.close();
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    ProgressDialog progress;
    public void showProgress() {

        try {

            if (progress == null)
                progress = new ProgressDialog(CaptureImageActivity.this);
            progress.setMessage("Please Wait..");
            progress.setCancelable(false);
            progress.show();

        } catch (Exception e) {

            e.printStackTrace();
            try {

                progress = new ProgressDialog(CaptureImageActivity.this);
                progress.setMessage("Please Wait..");
                progress.setCancelable(false);
                progress.show();

            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }


    public void hideProgress() {

        if (progress != null && progress.isShowing()) {

            progress.dismiss();

        }
    }

}
