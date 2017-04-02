package com.example.location.safenet;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.util.Pair;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;


/**
 * Utilities for interacting with the Firebase real time database.
 */
class FirebaseUtils {

    public static final String LOOKUP_USERS = "lookup-users";
    public static final String USERS_SHARING_WITH_ME = "users-sharing-with-me";
    public static final String SHARE_WITH = "share-with";
    public static final String USER_GCM_IDS = "user-gcm-ids";
    public static final String USERS = "users";
    private static FirebaseUser sfirebaseUser;
    private static MyLocation sLastLocation;


    // add new location for curent UID
    public static void addNewLocation(Context context, MyLocation location) {
        // locations/user-locs/${uid}/
        sLastLocation = location;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database
                .getReference("locations")
                .child("user-locs")
                .child(PrefUtils.getAccountUid(context));
        DatabaseReference newLocation = ref.push();
        newLocation.setValue(location);

    }

    public static void addUserInfo(@NonNull FirebaseUser user,
                                   @NonNull String phoneNumber) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String normalizedPhoneNumber = formatNumberCompat(phoneNumber);
        // user info
        // /users
        //      - $uid : "Display Name"
        database.getReference(USERS)
                .child(user.getUid())
                .setValue(user.getDisplayName());
        // PhoneNumber => UID mapping is stored under
        //   /lookup-users
        //                - <phoneNumber1> : "$uid"
        //                - <phoneNumber2> : "$uid2"
        database.getReference(LOOKUP_USERS)
                .child(normalizedPhoneNumber)
                .setValue(user.getUid());
    }

    static String formatNumberCompat(String phoneNumber) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return PhoneNumberUtils.normalizeNumber(phoneNumber);
        } else {
            return PhoneNumberUtils.formatNumber(phoneNumber);
        }
    }

    public static void getContactAUserOfSafeNet(@NonNull String phoneNumber, ValueEventListener listener) {
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        String normalizedPhoneNumber = formatNumberCompat(phoneNumber);
        database.getReference(LOOKUP_USERS)
                .child(normalizedPhoneNumber)
                .addListenerForSingleValueEvent(listener);

    }

    public static void fetchUidLocations(String uid, ChildEventListener listener) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        database.getReference("/locations/user-locs").child(uid)
                .limitToFirst(50)
                .addChildEventListener(listener);
    }

    public static void removeUidLocationsListener(String uid, ChildEventListener listener) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        database.getReference("/locations/user-locs").child(uid)
                .removeEventListener(listener);

    }

    public static void setFirebaseUser(FirebaseUser firebaseUser) {
        sfirebaseUser = firebaseUser;
    }

    public static FirebaseUser getFirebaseUser() {
        return sfirebaseUser;
    }

    static Pair<String, String> getContactInfo(Context context, Intent data) {
        Cursor cursor = null;
        try {
            Uri contactData = data.getData();
            cursor = context.getContentResolver().query(contactData, null, null, null, null);
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToFirst()) {

                String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                String hasPhone = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                String phoneNumber = null;
                if (hasPhone.equalsIgnoreCase("1")) {
                    Cursor phones = null;
                    try {
                        phones = context.getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                                null, null);
                        if (phones == null) {
                            return null;
                        }
                        phones.moveToFirst();
                        phoneNumber = phones.getString(phones.getColumnIndex("data1"));
                    } finally {
                        if (phones != null) {
                            phones.close();
                        }
                    }
                }
                Log.d("FirebaseUtils", "Got phone number " + phoneNumber + " name " + displayName);
                return Pair.create(phoneNumber, displayName);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }


    static class User {
        public String name;
        public String phoneNumber;
        public String uid;
        public User() {}
    }

    /**
     * /share-with
     *            /$my_uid
     *                    - phone1, Name, uid
     *                    - phone2, Name, uid2
     * /users-sharing-with-me
     *            /$my_uid
     *                   - uid1
     *                   - uid2
     */
    public static void shareMyLocationWith(Context context, String name, String phoneNumber, String uid) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference(SHARE_WITH)
                .child(PrefUtils.getAccountUid(context));
        User value = new User();
        value.name = name;
        value.phoneNumber = formatNumberCompat(phoneNumber);
        value.uid = uid;
        ref.push().setValue(value);

        DatabaseReference ref2 = database.getReference(USERS_SHARING_WITH_ME)
                .child(uid);
        User me = new User();
        me.name = sfirebaseUser.getDisplayName();
        me.uid = sfirebaseUser.getUid();
        me.phoneNumber = PrefUtils.getPhoneNumber(context);
        ref2.push().setValue(me);
    }

    /**
     * /users-sharing-with-me
     *            /$my_uid
     *                   - uid1
     *                   - uid2
     */
    public static DatabaseReference getUsersSharingLocationWithMeRef(Context context) {
        return FirebaseDatabase.getInstance().getReference(USERS_SHARING_WITH_ME)
                .child(PrefUtils.getAccountUid(context));

    }

    // Users who can see my Location
    public static DatabaseReference getMyLocationUsersRef(Context context) {
        return FirebaseDatabase.getInstance().getReference(SHARE_WITH)
                .child(PrefUtils.getAccountUid(context));
    }

    public static void putGcmId(Context context, String refreshedToken) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        // /user-gcm-ids
        //              - $uid : "token"
        database.getReference(USER_GCM_IDS)
                .child(PrefUtils.getAccountUid(context))
                .setValue(refreshedToken);
    }

    public static void sendSMS(Context context, String phone)
    {
        Uri uri = Uri.parse("smsto:" + phone);
        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
        String message = "Download Safenet to see where I am ! ";
        if (sLastLocation != null) {
            String url = String.format("http://maps.google.com/maps?q=%s,%s",
                    sLastLocation.getLatitude(), sLastLocation.getLongitude());
            message += url;
        }
        intent.putExtra("sms_body", message);
        context.startActivity(intent);
    }

    }

