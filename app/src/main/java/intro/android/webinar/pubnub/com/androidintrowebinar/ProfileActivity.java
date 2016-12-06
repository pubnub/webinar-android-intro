package intro.android.webinar.pubnub.com.androidintrowebinar;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class ProfileActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();

        // Loading mypreference XML where CheckBoxPreference with key "track_my_checkbox" exists
        addPreferencesFromResource(R.xml.profile);

        // Setup on change listener for checkbox - to track when user turns if off
        super.findPreference("fullname")
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference pref, Object val) {
                        String strval = (String) val;
//                        MainActivity.getPubNub.setState();
                        return true; // Finally, let the checkbox value go through to update itself
                    }
                }); // end of checkbox listener
    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.profile);
        }
    }

}