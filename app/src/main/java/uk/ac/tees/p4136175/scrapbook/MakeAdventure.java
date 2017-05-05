package uk.ac.tees.p4136175.scrapbook;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Calendar;
import android.view.Menu;


public class MakeAdventure extends AppCompatActivity implements View.OnClickListener{

    private BroadcastReceiver broadcastReceiver;

    private void enable_buttons() {
                Intent i = new Intent(getApplicationContext(),GPS_Service.class);
                startService(i);
    }

    private boolean runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}
                    ,100);
            return true;
        }
        return false;
    }


    //----------------------------------------------------------------------------------------------------------------------------------

    Button btnSave, btnCancel, btnDelete;
    EditText makeEntry;
    TextView date, location;
    String formattedDate;
    private int _Adventure_Id=0;

    // Image Stuff
    private Uri mImageCaptureUri;
    private ImageView mImageView;


    private static final int PICK_FROM_CAMERA = 1;
    private static final int PICK_FROM_FILE = 2;

    private final int SELECT_PHOTO = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_adventure);

        // Get all the components of the UI
        btnSave = (Button) findViewById(R.id.saveButton);
        btnSave.setOnClickListener(this);

        btnDelete = (Button) findViewById(R.id.deleteButton);
        btnDelete.setOnClickListener(this);

        btnCancel = (Button) findViewById(R.id.cancelButton);
        btnCancel.setOnClickListener(this);

        makeEntry = (EditText) findViewById(R.id.adventureEntry);

        date = (TextView) findViewById(R.id.dateText);
        location = (TextView) findViewById(R.id.locationText);

        mImageView = (ImageView) findViewById(R.id.imageView);

        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    location.append("\n" +intent.getExtras().get("coordinates"));

                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter("location_update"));

        _Adventure_Id =0;
        Intent intent = getIntent();
        _Adventure_Id =intent.getIntExtra("adventure_Id", 0);
        AdventureRepo repo = new AdventureRepo(this);
        AdventureEntry adv = new AdventureEntry();
        adv = repo.getAdventureById(_Adventure_Id);

        if(adv.note_text != null){
            makeEntry.setText(String.valueOf(adv.note_text));
        }

        if(adv.image != null){
            System.out.println(adv.image + " is the image");
            System.out.println(getImage(adv.image));
            mImageView.setImageBitmap(getImage(adv.image));
        }

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df  = new SimpleDateFormat("dd MMM yyyy");
        formattedDate = df.format(c.getTime());

        date.setText(formattedDate);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        Button pickImage = (Button) findViewById(R.id.imageButton);
        pickImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            }
        });

        if(!runtime_permissions())
            enable_buttons();



    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            case 100: {
                if( grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    enable_buttons();
                } else {
                    runtime_permissions();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    final Uri imageUri = imageReturnedIntent.getData();
                    InputStream imageStream = null;
                    try {
                        imageStream = getContentResolver().openInputStream(imageUri);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    mImageView.setImageBitmap(selectedImage);

                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public String getRealPathFromURI(Uri contentUri) {
        String [] proj      = {MediaStore.Images.Media.DATA};
        Cursor cursor       = getContentResolver().query(contentUri, proj, null, null, null);

        if (cursor == null) return null;

        int column_index    = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        cursor.moveToFirst();

        return cursor.getString(column_index);
    }

    @Override
    public void onClick(View v) {
        if(v == findViewById(R.id.saveButton)){
            AdventureRepo repo = new AdventureRepo(this);
            AdventureEntry adv = new AdventureEntry();
            adv.ID = _Adventure_Id;
            adv.note_text = makeEntry.getText().toString();

            Bitmap image = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
            adv.image = getBytes(image);
            adv.datetime = formattedDate;
            adv.loc_lang = "tester";
            adv.loc_lat = "tester";

            if(_Adventure_Id == 0){
                _Adventure_Id = repo.insert(adv);
                Toast.makeText(this, "New Adventure Created", Toast.LENGTH_SHORT).show();
            } else {
                repo.update(adv);
                Toast.makeText(this, "Adventure Entry Updated", Toast.LENGTH_SHORT).show();
            }
        } else if (v == findViewById(R.id.deleteButton)){
            AdventureRepo repo = new AdventureRepo(this);
            repo.delete(_Adventure_Id);
            Toast.makeText(this, "Adventure Deleted", Toast.LENGTH_SHORT);
            finish();
        } else if (v == findViewById(R.id.cancelButton)){
            unregisterReceiver(broadcastReceiver);
            finish();
        }
    }

    // This converts from bitmap to byte array
    public static byte[] getBytes(Bitmap bitmap){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    // This converts from byte array to bitmap
    public static Bitmap getImage(byte[] image){
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

}
