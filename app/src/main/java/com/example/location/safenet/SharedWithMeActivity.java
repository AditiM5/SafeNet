package com.example.location.safenet;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import static android.Manifest.permission.READ_CONTACTS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class SharedWithMeActivity extends BaseActivity {

    public static final String MODE = "MODE";
    private static final int PICK_CONTACT = 1002;
    private static final int REQUEST_READ_CONTACTS_REQUEST_CODE = 1003;
    public static final int SHARED_WITH_ME = 0;
    public static final int USERS_SHARED_WITH = 1;

    private ListView mListView;
    private FirebaseListAdapter<FirebaseUtils.User> mAdapter;
    private int mCurrentMode;
    private FloatingActionButton mShareFab;
    private ProgressDialog mProgressDialog;

    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        FrameLayout contentFrameLayout = (FrameLayout) findViewById(R.id.content_frame);
        View view = getLayoutInflater().inflate(R.layout.activity_list, contentFrameLayout);

        mListView = (ListView) view.findViewById(R.id.list_view);

        mCurrentMode = getIntent().getIntExtra(MODE, 0);
        DatabaseReference sharedWithMeRef =
                mCurrentMode == SHARED_WITH_ME ?
                        FirebaseUtils.getUsersSharingLocationWithMeRef(this) :
                        FirebaseUtils.getMyLocationUsersRef(this);

        mAdapter = new FirebaseListAdapter<FirebaseUtils.User>(this, FirebaseUtils.User.class,
                android.R.layout.simple_list_item_1, sharedWithMeRef) {
            @Override
            protected void populateView(View view, FirebaseUtils.User user, int position) {
                ((TextView) view.findViewById(android.R.id.text1)).setText(user.name);
                view.setTag(user);
            }
        };
        mListView.setAdapter(mAdapter);
        mShareFab = (FloatingActionButton) view.findViewById(R.id.fab);

        if (mCurrentMode == SHARED_WITH_ME) {
            mShareFab.setVisibility(View.GONE);
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    FirebaseUtils.User user = (FirebaseUtils.User) view.getTag();
                    if (user != null) {
                        Intent intent = new Intent(SharedWithMeActivity.this, MapsActivity.class);
                        intent.putExtra(MapsActivity.KEY_UID, user.uid);
                        intent.putExtra(MapsActivity.KEY_TITLE, user.name + "'s locations");
                        startActivity(intent);
                    }
                }
            });
        } else if (mCurrentMode == USERS_SHARED_WITH) {
            mShareFab.setVisibility(View.VISIBLE);
            mShareFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ActivityCompat.checkSelfPermission(SharedWithMeActivity.this, READ_CONTACTS)
                            != PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(
                                SharedWithMeActivity.this, new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS_REQUEST_CODE);
                        return;
                    }
                    pickContactsIntent();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if (requestCode == PICK_CONTACT) {
            if (resultCode == RESULT_OK) {
                Pair<String, String> result = FirebaseUtils.getContactInfo(this, intent);
                if (result != null) {
                    Toast.makeText(this,
                            "Will Share location with " + result.second,
                            Toast.LENGTH_LONG).show();
                    checkForContact(result);
                }

            }
        }
    }

    private void checkForContact(Pair<String, String> contactInfo) {
        final String name = contactInfo.second;
        final String phone = contactInfo.first;
        showProgressDialog();
        FirebaseUtils.getContactAUserOfSafeNet(phone, new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                hideProgressDialog();
                String uid = dataSnapshot.getValue(String.class);
                if (uid != null) {
                    Toast.makeText(SharedWithMeActivity.this, "User is present " + uid, Toast.LENGTH_LONG).show();
                    FirebaseUtils.shareMyLocationWith(SharedWithMeActivity.this, name, phone, uid);
                } else {
                    Toast.makeText(SharedWithMeActivity.this, "No user with phone " + phone, Toast.LENGTH_LONG).show();
                    FirebaseUtils.sendSMS(SharedWithMeActivity.this, phone);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_READ_CONTACTS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                pickContactsIntent();
            } else {
                makeSnackBar();
            }
        }
    }

    private void makeSnackBar() {
        Snackbar.make(
                findViewById(R.id.activity_start),
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Build intent that displays the App settings screen.
                        Intent intent = new Intent();
                        intent.setAction(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package",
                                BuildConfig.APPLICATION_ID, null);
                        intent.setData(uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                })
                .show();
    }

    private void pickContactsIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);
    }
}
