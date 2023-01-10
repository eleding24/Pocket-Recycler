
package com.example.recyclingapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.util.Pair;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.recyclingapp.ui.gallery.GalleryFragment;
import com.soundcloud.android.crop.Crop;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private Button btn_takePicture;
    private TextView[][] outLabels;
    private ImageView imgDisplay;
    private Uri imageUri;
    private String city;
    LocationManager locationManager;
    private String myText;
    private TextView recyclable;
    private String material;
    Global globalClass;
    Bundle args = new Bundle();
    GalleryFragment fragment = new GalleryFragment();

    private Interpreter tflite;
    private ArrayList<String> labelList;

    public static final int REQUEST_IMAGE = 100;
    public static final int REQUEST_PERMISSION = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions();

        this.setTitle("Pocket Recycler");
        globalClass = (Global) getApplicationContext();
        GalleryFragment fragment = (GalleryFragment) getSupportFragmentManager().findFragmentById(R.id.frag_gallery);
        btn_takePicture = (Button) findViewById(R.id.btn_takePicture);
        //textView_location = findViewById(R.id.text_location);
        recyclable = findViewById(R.id.tv_recycle);


        outLabels = new TextView[3][3];
        int[][] inps = {
                {R.id.tv_lbl1, R.id.tv_pct1},
                {R.id.tv_lbl2, R.id.tv_pct2},
        };
        for (int i = 0; i < inps.length; i++)
            for (int j = 0; j < inps[0].length; j++)
                outLabels[i][j] = (TextView) findViewById(inps[i][j]);

        imgDisplay = (ImageView) findViewById(R.id.img_display);
        //getLocation();

        // preparation for the deep learning stuff
        try {
            tflite = new Interpreter(Helper.loadModelFile(this.getAssets()), new Interpreter.Options());
            labelList = Helper.loadLabelList(this.getAssets());
        } catch (IOException e) {
            e.printStackTrace();
        }

        btn_takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLocation();
                openCameraIntent();

            }
        });


        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();
    }

    private void requestPermissions() {
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.CAMERA
        };

        if (!Helper.hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
    }

    private void openCameraIntent() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");

        // tell camera where to store the resulting picture
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // start the intent and wait for it to finish (c.f. async requests)
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        // if the camera activity is finished, obtained the uri, crop it to make it square, and send it to 'Classify' activity
        if(requestCode == REQUEST_IMAGE && resultCode == RESULT_OK) {
            try {
                Uri source_uri = imageUri;
                Uri dest_uri = Uri.fromFile(new File(getCacheDir(), "cropped"));
                // need to crop it to square image as CNN's always required square input
                Crop.of(source_uri, dest_uri).asSquare().start(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // if cropping acitivty is finished, get the resulting cropped image uri and send it to 'Classify' activity
        else if(requestCode == Crop.REQUEST_CROP && resultCode == RESULT_OK){
            classifyImage(Crop.getOutput(data));
        }
    }

    private void classifyImage(Uri uri) {
        int imageSizeX = Helper.DIM_IMG_SIZE_X;
        int imageSizeY = Helper.DIM_IMG_SIZE_Y;
        int imagePixelSize = Helper.DIM_PIXEL_SIZE;

        // initialize array that holds image data
        int[] imgArray = new int[imageSizeX * imageSizeY];

        // initialize byte array.
        ByteBuffer imgData =  ByteBuffer.allocateDirect(4 * imageSizeX * imageSizeY * imagePixelSize);
        imgData.order(ByteOrder.nativeOrder());

        // initialize probabilities array.
        float[][] labelProbArray = new float[1][labelList.size()];

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            imgDisplay.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // get current bitmap from imageView
        Bitmap bitmap_orig = ((BitmapDrawable) imgDisplay.getDrawable()).getBitmap();
        // resize the bitmap to the required input size to the CNN
        Bitmap bitmap = Helper.getResizedBitmap(bitmap_orig, imageSizeX, imageSizeY);
        // convert bitmap to byte array
        imgData = Helper.convertBitmapToByteBuffer(bitmap, imgData, imgArray);
        // pass byte data to the graph
        tflite.run(imgData, labelProbArray);

        ArrayList<Pair<Float, Integer>> toSort = new ArrayList<>();
        for (int i = 0; i < labelList.size(); i++) {
            toSort.add(new Pair<Float, Integer>(labelProbArray[0][i], i));
        }

        Collections.sort(toSort, new Comparator<Pair<Float, Integer>>() {
            @Override
            public int compare(Pair<Float, Integer> o1, Pair<Float, Integer> o2) {
                float diff = o1.first - o2.first;
                if (diff < 0) return 1;
                else if (diff == 0) return 0;
                else return -1;
            }
        });

        for (int i = 0; i < 2; i++) {
            outLabels[i][0].setText(labelList.get(toSort.get(i).second));
            outLabels[i][1].setText(Float.toString(toSort.get(i).first * 100f) + "%");
        }
        material = outLabels[0][0].getText().toString();
        if(material.equals("plastic")) {
            popup();
        }
        else{
            Toast.makeText(MainActivity.this, "Material: "+material, Toast.LENGTH_LONG).show();
            recycle();
        }

    }

    private void popup(){
        AlertDialog.Builder mydialog = new AlertDialog.Builder(MainActivity.this);
        mydialog.setTitle("Enter the recycling number if there is one (1-7): ");

        final EditText numInput = new EditText(MainActivity.this);
        numInput.setInputType(InputType.TYPE_CLASS_PHONE);
        mydialog.setView(numInput);

        mydialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                myText= numInput.getText().toString();
                material = material + myText;
                Toast.makeText(MainActivity.this, "Material: " + material, Toast.LENGTH_LONG).show();
                recycle();

            }
        });

        mydialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                Toast.makeText(MainActivity.this, "Material: " + material, Toast.LENGTH_LONG).show();
                recyclable.setText("Check out your city guidelines for more info");
            }
        });
        mydialog.show();

    }

    private void recycle(){

        String[] recyclables = {"cardboard", "metal", "paper", "glass"};
        if(city.equals("Plano")||city.equals("Frisco")||city.equals("Grand Prairie")){
            for(int i = 0; i < recyclables.length; i++){
                if(material.equals(recyclables[i])||material.equals("plastic1")||material.equals("plastic2")||material.equals("plastic5")){
                    globalClass.addCount();
                    recyclable.setText("Recyclable");
                    return;
                }
            }
            recyclable.setText("Not Recyclable");
        }
        else if(city.equals("Denton")||city.equals("Carrollton")||city.equals("Lewisville")||city.equals("Arlington")){
            if(material.equals("trash")){
                recyclable.setText("Not Recyclable");
            }
            else {
                globalClass.addCount();
                recyclable.setText("Recyclable");
            }
        }
        else if(city.equals("Allen")||city.equals("Dallas")||city.equals("Fort Worth")||city.equals("Garland")||city.equals("McKinney")){
            if(material.equals("trash")||material.equals("plastic6")){
                recyclable.setText("Not Recyclable");
            }else {
                globalClass.addCount();
                recyclable.setText("Recyclable");
            }
        }
        else if(city.equals("Irving")){
            if(material.equals("trash")||material.equals("plastic4")){
                recyclable.setText("Not Recyclable");
            }else {
                globalClass.addCount();
                recyclable.setText("Recyclable");
            }
        }
        else{
            recyclable.setText("Sorry your location is not supported at the moment.");
        }

    }


    @SuppressLint("MissingPermission")
    private void getLocation() {

        try {
            locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,5,MainActivity.this);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
            String address = addresses.get(0).getLocality();
            Toast.makeText(this, "Your location: "+address, Toast.LENGTH_SHORT).show();

            city = address;

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}