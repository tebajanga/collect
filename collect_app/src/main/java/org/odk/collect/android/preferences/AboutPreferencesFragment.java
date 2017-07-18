package org.odk.collect.android.preferences;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.customtabs.CustomTabsIntent;

import org.odk.collect.android.R;
import org.odk.collect.android.utilities.CustomTabHelper;
import org.odk.collect.android.activities.OpenSourceLicensesActivity;

import java.util.List;

import timber.log.Timber;


public class AboutPreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    public static final String KEY_OPEN_SOURCE_LICENSES = "open_source_licenses";
    public static final String KEY_TELL_YOUR_FRIENDS = "tell_your_friends";
    public static final String KEY_LEAVE_A_REVIEW = "leave_a_review";
    public static final String KEY_ODK_WEBSITE = "info";
    private static final String GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details?id=";
    //private static final String ODK_WEBSITE = "https://opendatakit.org";
    private static final String ODK_WEBSITE = "https://opendatakit.org";
    private CustomTabHelper mCustomTabHelper;
    private Uri uri;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about_preferences);

        PreferenceScreen odkWebsitePreference = (PreferenceScreen) findPreference(
                KEY_ODK_WEBSITE);
        PreferenceScreen openSourceLicensesPreference = (PreferenceScreen) findPreference(
                KEY_OPEN_SOURCE_LICENSES);
        PreferenceScreen tellYourFriendsPreference = (PreferenceScreen) findPreference(
                KEY_TELL_YOUR_FRIENDS);
        PreferenceScreen leaveAReviewPreference = (PreferenceScreen) findPreference(
                KEY_LEAVE_A_REVIEW);
        mCustomTabHelper = new CustomTabHelper();
        uri = Uri.parse(ODK_WEBSITE);
        odkWebsitePreference.setOnPreferenceClickListener(this);
        openSourceLicensesPreference.setOnPreferenceClickListener(this);
        tellYourFriendsPreference.setOnPreferenceClickListener(this);
        leaveAReviewPreference.setOnPreferenceClickListener(this);
    }


    @Override
    public void onStart() {
        super.onStart();
        mCustomTabHelper.bindCustomTabsService(this.getActivity(), uri);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final String APP_PACKAGE_NAME = getActivity().getPackageName();

        switch (preference.getKey()) {
            case KEY_ODK_WEBSITE:
                if (mCustomTabHelper.getPackageName(getActivity()).size() != 0) {
                    CustomTabsIntent customTabsIntent =
                            new CustomTabsIntent.Builder()
                                    .build();
                    customTabsIntent.intent.setPackage(mCustomTabHelper.getPackageName(getActivity()).get(0));
                    customTabsIntent.launchUrl(getActivity(), uri);
                } else {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ODK_WEBSITE)));
                }
                break;

            case KEY_OPEN_SOURCE_LICENSES:
                startActivity(new Intent(getActivity().getApplicationContext(),
                        OpenSourceLicensesActivity.class));
                break;

            case KEY_TELL_YOUR_FRIENDS:
                Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT,
                        getString(R.string.tell_your_friends_msg) + " " + GOOGLE_PLAY_URL
                                + APP_PACKAGE_NAME);
                startActivity(Intent.createChooser(shareIntent,
                        getString(R.string.tell_your_friends_title)));
                break;

            case KEY_LEAVE_A_REVIEW:
                boolean reviewTaken = false;
                try {
                    // Open the google play store app if present
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=" + APP_PACKAGE_NAME));
                    PackageManager packageManager = getActivity().getPackageManager();
                    List<ResolveInfo> list = packageManager.queryIntentActivities(intent, 0);
                    for (ResolveInfo info : list) {
                        ActivityInfo activity = info.activityInfo;
                        if (activity.name.contains("com.google.android")) {
                            ComponentName name = new ComponentName(
                                    activity.applicationInfo.packageName,
                                    activity.name);
                            intent.setComponent(name);
                            startActivity(intent);
                            reviewTaken = true;
                        }
                    }
                } catch (android.content.ActivityNotFoundException anfe) {
                    Timber.e(anfe);
                }
                if (!reviewTaken) {
                    // Show a list of all available browsers if user doesn't have a default browser
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(GOOGLE_PLAY_URL + APP_PACKAGE_NAME));
                    startActivity(intent);
                }
                break;
        }
        return true;
    }
}