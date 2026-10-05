package com.sap.sailing.racecommittee.app.ui.activities;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.AppUtils;
import com.sap.sailing.domain.base.racegroup.RaceGroup;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.fragments.MainPreferenceFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.preference.RegattaPreferenceFragment;
import com.sap.sailing.racecommittee.app.utils.PreferenceHelper;

public class PreferenceActivity extends AppCompatActivity {

    public static final String SPECIFIC_REGATTA_PREFERENCES_NAME = "REGATTA_PREFERENCES_";
    public static final String EXTRA_SPECIFIC_REGATTA_NAME = "EXTRA_SPECIFIC_REGATTA_NAME";
    public static final String EXTRA_SPECIFIC_REGATTA_PREFERENCES_NAME = "EXTRA_SPECIFIC_REGATTA_PREFERENCE_KEY";
    public static final String EXTRA_SHOW_FRAGMENT = "SHOW_FRAGMENT";
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = "SHOW_FRAGMENT_ARGUMENTS";
    private static final String TAG = PreferenceActivity.class.getName();

    public static void openSpecificRegattaConfiguration(Context context, RaceGroup raceGroup) {
        String regattaPreference = PreferenceHelper.getRegattaPrefFileName(raceGroup.getName());

        Intent intent = new Intent(context, PreferenceActivity.class);
        Bundle info = new Bundle();
        intent.putExtra(EXTRA_SHOW_FRAGMENT, RegattaPreferenceFragment.class.getName());
        info.putString(EXTRA_SPECIFIC_REGATTA_PREFERENCES_NAME, regattaPreference);
        info.putString(EXTRA_SPECIFIC_REGATTA_NAME, raceGroup.getName());
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, info);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppUtils.lockOrientation(this);
        setContentView(R.layout.preference_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
                getSupportActionBar().setTitle(getText(R.string.settings_activity_title));
            }
        }

        disableStatusBarTranslucent();
        setStatusBarColor();
        Fragment fragment = null;
        if (getIntent() != null && getIntent().getExtras() != null) {
            Bundle bundle = getIntent().getExtras();
            if (bundle.containsKey(EXTRA_SHOW_FRAGMENT)) {
                final String className = bundle.getString(EXTRA_SHOW_FRAGMENT);
                try {
                    if (className != null) {
                        fragment = (Fragment) Class.forName(className).newInstance();
                    }
                } catch (InstantiationException ex) {
                    ExLog.ex(this, TAG, ex);
                } catch (IllegalAccessException ex) {
                    ExLog.ex(this, TAG, ex);
                } catch (ClassNotFoundException ex) {
                    ExLog.ex(this, TAG, ex);
                }
                if (bundle.containsKey(EXTRA_SHOW_FRAGMENT_ARGUMENTS)) {
                    Bundle info = bundle.getBundle(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
                    if (info != null) {
                        String raceGroupName = info.getString(EXTRA_SPECIFIC_REGATTA_NAME);
                        String title = getString(R.string.preference_regatta_specific_title, raceGroupName);
                        final ActionBar actionBar = getSupportActionBar();
                        if (actionBar != null) {
                            actionBar.setTitle(title);
                        }
                        if (fragment != null) {
                            fragment.setArguments(info);
                        }
                    }
                }
            }
        }
        if (fragment == null) {
            fragment = MainPreferenceFragment.newInstance();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void disableStatusBarTranslucent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.settings_navbar));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            getSupportFragmentManager().beginTransaction()
                    .runOnCommit(() -> {
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle(getString(R.string.settings_activity_title));
                        }
                    })
                    .commit();
        } else {
            super.onBackPressed();
        }
    }
}
