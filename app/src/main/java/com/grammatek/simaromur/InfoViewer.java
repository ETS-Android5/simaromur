package com.grammatek.simaromur;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.grammatek.simaromur.db.AppData;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity shows information about the application.
 */
public class InfoViewer extends AppCompatActivity {
    private final static String LOG_TAG = "Simaromur_Java_" + InfoViewer.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        // use our non-default layout
        setContentView(R.layout.activity_info);
        setTitle("Símarómur / " + getResources().getString(R.string.simaromur_info));
        populateInformation();
    }

    @Override
    public void onResume() {
        Log.v(LOG_TAG, "onResume:");
        super.onResume();
        // set the Firebase analytics checkbox according to what's in the database
        CheckBox cb = findViewById(R.id.UserConsentFireBase);
        cb.setOnClickListener(view -> {
            boolean isChecked = ((CheckBox) view).isChecked();
            setFirebaseConsentCheckBox(cb, isChecked);
            App.getAppRepository().doGiveCrashLyticsUserConsent(isChecked);
        });

        // initialize checkbox appearance
        AppData appData = App.getAppRepository().getCachedAppData();
        if (appData != null) {
            final boolean consentGiven = appData.crashLyticsUserConsentGiven;
            cb.setChecked(consentGiven);
            setFirebaseConsentCheckBox(cb, consentGiven);
        }
    }

    /**
     * Set check mark and text color appearance
     * @param cb       CheckBox view
     * @param checked  boolean for if checkbox is checked or not
     */
    private void setFirebaseConsentCheckBox(CheckBox cb, boolean checked) {
        if (checked) {
            cb.setButtonTintList(getColorStateList(R.color.green));
            cb.setTextColor(getColorStateList(R.color.colorTextPrimary));
        } else {
            cb.setButtonTintList(getColorStateList(R.color.chrome));
            cb.setTextColor(getColorStateList(R.color.chrome));
        }
    }

    private void populateInformation() {
        final List<String> Info = new ArrayList<String>() {
            {
                add(getString(R.string.info_app_version));
                add(getString(R.string.info_url));
                add(getString(R.string.info_copyright));
                add(getString(R.string.info_privacy_notice));
                add(getString(R.string.info_android_version));
                add(getString(R.string.info_phone_model));
            }
        };

        final List<String> Data = new ArrayList<String>() {
            {
            try {
                PackageInfo pInfo = getApplicationContext().getPackageManager()
                        .getPackageInfo(getApplicationContext().getPackageName(), 0);
                String version = pInfo.versionName;
                add(version);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                add(getString(R.string.info_version_error));
            }
            add(getString(R.string.info_repo_url));
            add(getString(R.string.info_about));
            add(getString(R.string.info_privacy_notice_url));
            add(android.os.Build.VERSION.RELEASE);
            add(android.os.Build.MODEL);
            }
        };

        String[] dataArray = new String[Data.size()];
        Data.toArray(dataArray);
        String[] infoArray = new String[Info.size()];
        Info.toArray(infoArray);
        ListView infoView = findViewById(R.id.infoListView);
        infoView.setAdapter(new SettingsArrayAdapter(this, infoArray, dataArray));
    }

    private static class SettingsArrayAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final String[] values;
        private final String[] data;

        public SettingsArrayAdapter(Context context, String[] values, String[] data) {
            super(context, R.layout.activity_info, values);
            this.context = context;
            this.values = values;
            this.data = data;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.info_item, parent, false);
            }

            TextView infoType = convertView.findViewById(R.id.infotitle);
            TextView infoDetail = convertView.findViewById(R.id.infodetail);
            infoType.setText(values[position]);
            infoDetail.setText(data[position]);
            return convertView;
        }
    }
}
