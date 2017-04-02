package com.example.location.safenet;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class PrefUtils {

    static final String ACCOUNT_UID = "ACCOUNT_UID";
    public static final String REQUEST_LOCATION_UPDATES = "REQUEST_LOCATION_UPDATES";
    public static final String KEY_UPDATE_INTERVAL_MILLIS = "KEY_UPDATE_INTERVAL_MILLIS";
    public static final String KEY_GCM_ID = "KEY_GCM_ID";
    private static final String KEY_PHONE_NUMBER = "KEY_PHONE_NUMBER";
    public static final String KEY_PHOTO_URL = "KEY_PHOTO_URL";
    private static final String KEY_DISPLAY_NAME = "KEY_DISPLAY_NAME";

    private static String gcmId;

    private PrefUtils() {}

    public static void setAccountUid(Context context, String uid) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(ACCOUNT_UID, uid)
                .apply();
    }

    public static String getAccountUid(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(ACCOUNT_UID, null);
    }

    public static boolean hasAccountUid(Context context) {
        return getAccountUid(context) != null;
    }

    static void setRequestingLocationUpdates(Context context, boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(REQUEST_LOCATION_UPDATES, enabled)
                .apply();
    }

    public static boolean isRequestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(REQUEST_LOCATION_UPDATES, false);
    }

    /**
     * Get the update interval in milliseconds
     * @param context
     * @return
     */
    public static long getUpdateIntervalMs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_UPDATE_INTERVAL_MILLIS, 10000); // default is 10k millis ~ 10 seconds
    }

    /**
     * Set the update interval in millis should be > 10,000 milliseconds.
     *
     * @param context
     * @param millis
     */
    public static void setUpdateIntervalMs(Context context, int millis) {
        if (millis < 10000) {
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(KEY_UPDATE_INTERVAL_MILLIS, millis)
                .apply();
    }

    public static void setGcmId(Context context, String gcmId) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_GCM_ID, gcmId)
                .apply();
    }

    public static String getGcmId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_GCM_ID, null);
    }

    public static String getPhoneNumber(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_PHONE_NUMBER, "");
    }
    public static void setPhoneNumber(Context context, String phoneNumber) {
        phoneNumber = FirebaseUtils.formatNumberCompat(phoneNumber);
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_PHONE_NUMBER, phoneNumber)
                .apply();
    }


    public static void setPhotoUrl(Context context, Uri photoUrl) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_PHOTO_URL, photoUrl.toString())
                .apply();
    }

    public static String getPhotoUrl(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_PHOTO_URL, null);
    }

    public static void setDisplayName(Context context, String displayName) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_DISPLAY_NAME, displayName)
                .apply();
    }

    public static String getDisplayName(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_DISPLAY_NAME, null);
    }

    public static void removePhoneNumber(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .remove(KEY_PHONE_NUMBER)
                .apply();
    }
}
