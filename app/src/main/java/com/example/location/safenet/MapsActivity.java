package com.example.location.safenet;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.FrameLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;

import java.util.List;

public class MapsActivity extends BaseActivity implements OnMapReadyCallback {

    public static final String KEY_UID = "UID";
    public static final String KEY_TITLE = "KEY_TITLE";
    private GoogleMap mMap;
    private boolean mAddedChildListener;

    private ChildEventListener childEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            MyLocation location = dataSnapshot.getValue(MyLocation.class);
            if (location != null) {
                LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                        .position(loc)
                        .title("On " + DateUtils.formatDateTime(MapsActivity.this, location.getWhen(), 0)));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            }
        }


        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String title = getIntent().getStringExtra(KEY_TITLE);
        if (title != null) {
            setTitle(title);
        }
        FrameLayout contentFrameLayout = (FrameLayout) findViewById(R.id.content_frame);
        getLayoutInflater().inflate(R.layout.activity_maps, contentFrameLayout);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        String uid = getUid();
        FirebaseUtils.fetchUidLocations(uid, childEventListener);
    }

    private String getUid() {
        String uid = getIntent().getStringExtra(KEY_UID);
        if (uid == null) {
            uid = PrefUtils.getAccountUid(this);
        }
        return uid;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAddedChildListener) {
            String uid = getUid();
            FirebaseUtils.removeUidLocationsListener(uid, childEventListener);
            mAddedChildListener = false;
        }
    }
}
