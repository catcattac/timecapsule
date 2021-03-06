package com.timecapsule.app.feedactivity;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import com.facebook.FacebookSdk;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.timecapsule.app.NotificationsFragment;
import com.timecapsule.app.R;
import com.timecapsule.app.addmediafragment.AddCapsuleLocationFragment;
import com.timecapsule.app.addmediafragment.AudioFragment;
import com.timecapsule.app.addmediafragment.CapsuleUploadFragment;
import com.timecapsule.app.locationpick.PlaceDetectionFragment;
import com.timecapsule.app.locationpick.controller.MediaListener;
import com.timecapsule.app.profilefragment.ProfileFragment;
import com.timecapsule.app.profilefragment.model.Capsule;
import com.timecapsule.app.profilefragment.model.User;
import com.timecapsule.app.searchfragment.SearchFragment;
import com.timecapsule.app.users.UsersFragment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


public class FeedActivity extends AppCompatActivity implements View.OnClickListener, MediaListener {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_LOCATION = 201;
    private static final int REQUEST_CAMERA_PERMISSION = 203;
    private static final int TAKE_PICTURE = 200;
    private static final int CAPTURE_VIDEO = 201;
    private static final String TAG = FeedActivity.class.getSimpleName();
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    private BottomNavigationView bottomNavigationView;
    private ImageView iv_add_friend;
    private FloatingActionsMenu fab_add_media;
    private FloatingActionButton fab_photo;
    private FloatingActionButton fab_audio;
    private FloatingActionButton fab_videocam;
    private String mCurrentPhotoPath;
    private GoogleApiClient googleApiClient;
    private AddCapsuleLocationFragment addCapsuleLocationFragment;
    private Fragment timePlacePickerFragment;
    private String place;
    private PlaceDetectionFragment placeDetectionFragment;
    private AudioFragment audioFragment;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private StorageReference imagesRef;
    private UploadTask uploadTask;
    private ProgressDialog mProgress;
    private String mediaType;
    private CapsuleUploadFragment capsuleUploadFragment;
    private double locationLat;
    private double locationLong;
    private String address;
    private File destinationFile;
    private User user;
    private ListView userListView;
    List<User> users = new ArrayList<>();
    private Capsule capsule;
    private String mPhotoPath;
    private Uri photoURI;
    private String userName;


    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        imagesRef = storageReference.child("images");
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        mProgress = new ProgressDialog(this);
        imagesRef = storageReference.child("images");
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        firebaseStorage = FirebaseStorage.getInstance();
        databaseReference = firebaseDatabase.getReferenceFromUrl("https://timecapsule-8b809.firebaseio.com/");
        users = new ArrayList<>();
        requestLocationPermission();
        requestCameraPemission();
        requestAudioPermission();
        setViews();
        setBottomNavButtons();
        clickCamera();
        clickAudio();
        clickVideocam();
        FacebookSdk.getApplicationContext();

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.container_main, new FeedFragment())
                    .commit();
        }

        googleApiClient = new GoogleApiClient
                .Builder(getApplicationContext())
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(LocationServices.API)
                .build();
    }

    private void goToCapsuleUploadFragment(String capsuleUpload) {
        android.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
        capsuleUploadFragment = CapsuleUploadFragment.newInstance(capsuleUpload);
        capsuleUploadFragment.show(ft, "Capsule Uploaded");
    }


    private void setViews() {
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        iv_add_friend = (ImageView) findViewById(R.id.iv_add_friend);
        fab_photo = (FloatingActionButton) findViewById(R.id.fab_photo);
        fab_audio = (FloatingActionButton) findViewById(R.id.fab_audio);
        fab_videocam = (FloatingActionButton) findViewById(R.id.fab_videocam);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_add_friend:
                setUsersDatabase();
                getUserList();
        }
    }

    private void setUsersDatabase() {
        DatabaseReference allUsers = databaseReference.child("users");
        allUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                users.clear();
                for (DataSnapshot child : children) {
                    Log.d(TAG, "USERS: " + dataSnapshot.getChildren());
                    user = new User((String) child.child("name").getValue(),
                            (String) child.child("username").getValue(),
                            (String) child.child("profilePhoto").getValue());
                    Log.d(TAG, "USERS: " + user);
                    users.add(user);
                }
                UsersFragment usersFragment = (UsersFragment) getFragmentManager().findFragmentByTag(UsersFragment.TAG);
                if (usersFragment != null) {
                    usersFragment.setUsers(users);
                }
                Log.d(TAG, "USERS: " + users);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    public void getUserList() {
        UsersFragment usersFragment = new UsersFragment();
        Bundle args = new Bundle();
        args.putSerializable(UsersFragment.EXTRA_USERS, (ArrayList<User>) users);
        usersFragment.setArguments(args);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container_main, usersFragment, UsersFragment.TAG)
                .addToBackStack(null)
                .commit();
    }


    private void goToAddLocation(String mediaType) {
        Bundle bundle = new Bundle();
        bundle.putString("mediaType", mediaType);
        android.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
        addCapsuleLocationFragment = new AddCapsuleLocationFragment();
        addCapsuleLocationFragment.setListener(this);
        addCapsuleLocationFragment.setMediaType(mediaType);
        addCapsuleLocationFragment.show(ft, "NearbyLocation");
    }


    private void clickCamera() {
        fab_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaType = "camera";
                goToAddLocation(mediaType);

            }
        });
    }

    private void clickAudio() {
        fab_audio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaType = "audio";
                goToAddLocation(mediaType);
            }
        });

    }

    private void clickVideocam() {
        fab_videocam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaType = "video";
                goToAddLocation(mediaType);

            }
        });
    }

    public void setAddFriend() {
        String appLinkUrl;
        String previewImageUrl;

        appLinkUrl = "https://fb.me/1777539359241152";
        previewImageUrl = "https://sarahasousa.files.wordpress.com/2014/11/time-capsule.gif";

        AppInviteContent content = new AppInviteContent.Builder()
                .setApplinkUrl(appLinkUrl)
                .setPreviewImageUrl(previewImageUrl)
                .build();
        AppInviteDialog.show(this, content);
    }

    private void setBottomNavButtons() {
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_feed:
                        setFeedFragment();
                        break;
                    case R.id.action_search:
                        setSearchFragment();
                        break;
                    case R.id.action_notifications:
                        setNotificationsFragment();
                        break;
                    case R.id.action_profile:
                        setProfileFragment();
                        break;
                }
                return true;
            }
        });
    }

    private void setFeedFragment() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container_main, new FeedFragment())
                .addToBackStack("feed")
                .commit();

        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
            }
        });
    }


    private void setSearchFragment() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container_main, new SearchFragment())
                .addToBackStack("search")
                .commit();
    }


    private void setNotificationsFragment() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container_main, new NotificationsFragment())
                .addToBackStack("notifications")
                .commit();
    }


    private void setProfileFragment() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container_main, new ProfileFragment())
                .addToBackStack("profile")
                .commit();
    }

    private void requestCameraPemission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            Log.d("damn", "openCamera: ");
            return;
        }
    }

    private void requestAudioPermission() {
        ActivityCompat.requestPermissions(FeedActivity.this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                if (grantResults.length > 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;
                }
                break;

            case REQUEST_LOCATION:
                if (grantResults.length == 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // We can now safely use the API we requested access to
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                } else {
                    // Permission was denied or request was cancelled
                }
                break;
        }
    }

    @Override
    public void goToCamera(Intent intent) {
        Log.d("GO TO CAMERA LISTENER", "goToCamera: ");

        if (intent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(intent, TAKE_PICTURE);
            }
        }
        //startActivityForResult(intent, TAKE_PICTURE);



    }


    @Override
    public void goToVideo(Intent intent) {
        startActivityForResult(intent, CAPTURE_VIDEO);
    }

    @Override
    public void goToAudio() {
        android.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
        audioFragment = AudioFragment.newInstance("Audio");
        audioFragment.setListener(this);
        audioFragment.show(ft, "audio");
    }

    @Override
    public void setLatLongValues(double locationLatitude, double locationLongitude) {
        this.locationLat = locationLatitude;
        this.locationLong = locationLongitude;
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public void uploadAudio(Uri downloadUrl) {
        addUrlToDatabase(downloadUrl);
    }


    private void addUrlToDatabase(Uri uri) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()).replace("_","");
        Calendar c = Calendar.getInstance();
        String date = c.getTime().toString();
        String capsuleId = UUID.randomUUID().toString().replaceAll("-", "");
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("users")
                .child(FirebaseAuth.getInstance()
                        .getCurrentUser().getUid())
                .child("capsules").child(capsuleId);
        myRef.setValue(uri.toString());
        DatabaseReference capRef = database.getReference("capsules").child(capsuleId);
        String storageLink = uri.toString();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userName = " ";
        Log.d(TAG, "addUrlToDatabase: " + userName);
        Log.d(TAG, "addUrlToDatabase: " + locationLong + locationLat);
        myRef.setValue(new Capsule(userId, storageLink, locationLat, locationLong, date, address, userName, timeStamp));
        capRef.setValue(new Capsule(userId, storageLink, locationLat, locationLong, date, address, userName, timeStamp));
        Log.d(TAG, "onDataChange: " + capRef);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("GO TO MEDIA", "onActivityResult: ");
        switch (requestCode) {
            case TAKE_PICTURE:
                if (resultCode == RESULT_OK) {
                    mProgress.setMessage("Uploading Photo");
                    mProgress.show();

                    if (data == null) {
//                        Bundle extras = data.getExtras();
//                        Bitmap imageBitmap = (Bitmap) extras.get("data");
//                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//                        byte[] dataBAOS = baos.toByteArray();
//                        Bundle extras = data.getExtras();
//                        Uri uri = (Uri)extras.get("EXTRA_OUTPUT");

                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        String imageFileName = "JPEG_" + timeStamp + "_";
                        String firebaseReference = imageFileName.concat(".jpg");
                        imagesRef = imagesRef.child(firebaseReference);
                        StorageReference newImageRef = storageReference.child("images/".concat(firebaseReference));
                        newImageRef.getName().equals(newImageRef.getName());
                        newImageRef.getPath().equals(newImageRef.getPath());
//                        UploadTask uploadTask = imagesRef.putBytes(dataBAOS);
                        UploadTask uploadTask = imagesRef.putFile(photoURI);

                        uploadTask.addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle unsuccessful uploads
                            }
                        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                                @SuppressWarnings("VisibleForTests") Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                Log.d(TAG, "onSuccess: " + downloadUrl.toString());
                                addUrlToDatabase(downloadUrl);
                                mProgress.dismiss();
                                goToCapsuleUploadFragment("capsule upload");
                            }
                        });
                    }
                }
                break;
            case CAPTURE_VIDEO:
                if (resultCode == RESULT_OK) {
                    mProgress.setMessage("uploading video...");
                    mProgress.show();

                    if (data != null) {
                    }
                }
                break;
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents

        mPhotoPath = image.getAbsolutePath();

        return image;
    }
}
