package intro.android.webinar.pubnub.com.androidintrowebinar;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

public class ProfileActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public final static String TAG = "ProfileActivity";

    private UserProfile profile;
    private boolean profileUpdated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "$$$ ProfileActivity.onStart");
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new ProfileFragment()).commit();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);

        profile = (UserProfile)getIntent().getSerializableExtra("profile");
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "$$$ ProfileActivity.onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "$$$ ProfileActivity.onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "$$$ ProfileActivity.onPause");
        finish();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "$$$ ProfileActivity.onDestroy");

//        if (profileUpdated) {
//            Intent intent = new Intent(this, MainActivity.class);
//            intent.putExtra("updatedProfile", profile);
//        }

        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "*** key: " + key.toString() + ", value: " + sharedPreferences.getString(key, null));

        profileUpdated = true;

        if (key.equals("fullname")) {
            profile.setFullname(sharedPreferences.getString(key, profile.getFullname()));
        }
        else if (key.equals("location")) {
            profile.setLocation(sharedPreferences.getString(key, profile.getLocation()));
        }
        else if (key.equals("language")) {
            profile.setLanguage(sharedPreferences.getString(key, profile.getLanguage()));
        }

        if (profileUpdated) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("updatedProfile", profile);
            setResult(Activity.RESULT_OK, resultIntent);
        }
    }

    public static class ProfileFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.profile);
        }
    }
}