package org.odk.collect.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.dao.InstancesDao;
import org.odk.collect.android.preferences.AdminKeys;
import org.odk.collect.android.preferences.AdminPreferencesActivity;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.utilities.ApplicationConstants;
import timber.log.Timber;

import java.lang.ref.WeakReference;

/**
 * Created by root on 5/5/17.
 */
public class CompletedFormsActivity extends Activity {
    private static final boolean EXIT = true;
    // buttons
    private Button mEnterDataButton;
    private Button mCompletedForms;
    private Button mManageFilesButton;
    private Button mSendDataButton;
    private Button mViewSentFormsButton;
    private Button mReviewDataButton;
    private Button mGetFormsButton;
    private View mReviewSpacer;
    private View mGetFormsSpacer;
    private AlertDialog mAlertDialog;
    private SharedPreferences mAdminPreferences;
    private int mCompletedCount;
    private int mSavedCount;
    private int mViewSentCount;
    private Cursor mFinalizedCursor;
    private Cursor mSavedCursor;
    private Cursor mViewSentCursor;
    private IncomingHandler mHandler = new IncomingHandler(this);
    private MyContentObserver mContentObserver = new MyContentObserver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.completed_forms);

        // review data button. expects a result.
        mReviewDataButton = (Button) findViewById(R.id.review_data);
        mReviewDataButton.setText(getString(R.string.review_data_button));
        mReviewDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, ApplicationConstants.FormModes.EDIT_SAVED, "click");
                Intent i = new Intent(getApplicationContext(), InstanceChooserList.class);
                i.putExtra(ApplicationConstants.BundleKeys.FORM_MODE,
                        ApplicationConstants.FormModes.EDIT_SAVED);
                startActivity(i);
            }
        });

        // send data button. expects a result.
        mSendDataButton = (Button) findViewById(R.id.send_data);
        mSendDataButton.setText(getString(R.string.send_data_button));
        mSendDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "uploadForms", "click");
                Intent i = new Intent(getApplicationContext(),
                        InstanceUploaderList.class);
                startActivity(i);
            }
        });

        // sent forms button. no result expected.
        mViewSentFormsButton = (Button) findViewById(R.id.view_sent_forms);
        mViewSentFormsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger().logAction(this,
                        ApplicationConstants.FormModes.VIEW_SENT, "click");
                Intent i = new Intent(getApplicationContext(), InstanceChooserList.class);
                i.putExtra(ApplicationConstants.BundleKeys.FORM_MODE,
                        ApplicationConstants.FormModes.VIEW_SENT);
                startActivity(i);
            }
        });

        // manage forms button. no result expected.
        mManageFilesButton = (Button) findViewById(R.id.manage_forms);
        mManageFilesButton.setText(getString(R.string.manage_files));
        mManageFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "deleteSavedForms", "click");
                Intent i = new Intent(getApplicationContext(),
                        FileManagerTabs.class);
                startActivity(i);
            }
        });

        setTitle(getString(R.string.completed_forms));

        mAdminPreferences = this.getSharedPreferences(
                AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

        InstancesDao instancesDao = new InstancesDao();

        // count for finalized instances
        try {
            mFinalizedCursor = instancesDao.getFinalizedInstancesCursor();
        } catch (Exception e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        if (mFinalizedCursor != null) {
            startManagingCursor(mFinalizedCursor);
        }
        mCompletedCount = mFinalizedCursor != null ? mFinalizedCursor.getCount() : 0;
        getContentResolver().registerContentObserver(InstanceProviderAPI.InstanceColumns.CONTENT_URI, true,
                mContentObserver);
        // mFinalizedCursor.registerContentObserver(mContentObserver);

        // count for saved instances
        try {
            mSavedCursor = instancesDao.getUnsentInstancesCursor();
        } catch (Exception e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        if (mSavedCursor != null) {
            startManagingCursor(mSavedCursor);
        }
        mSavedCount = mSavedCursor != null ? mSavedCursor.getCount() : 0;

        //count for view sent form
        try {
            mViewSentCursor = instancesDao.getSentInstancesCursor();
        } catch (Exception e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }
        if (mViewSentCursor != null) {
            startManagingCursor(mViewSentCursor);
        }
        mViewSentCount = mViewSentCursor != null ? mViewSentCursor.getCount() : 0;

        updateButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = this.getSharedPreferences(
                AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

        boolean edit = sharedPreferences.getBoolean(
                AdminKeys.KEY_EDIT_SAVED, true);
        if (!edit) {
            if (mReviewDataButton != null) {
                mReviewDataButton.setVisibility(View.GONE);
            }
            if (mReviewSpacer != null) {
                mReviewSpacer.setVisibility(View.GONE);
            }
        } else {
            if (mReviewDataButton != null) {
                mReviewDataButton.setVisibility(View.VISIBLE);
            }
            if (mReviewSpacer != null) {
                mReviewSpacer.setVisibility(View.VISIBLE);
            }
        }

        boolean send = sharedPreferences.getBoolean(
                AdminKeys.KEY_SEND_FINALIZED, true);
        if (!send) {
            if (mSendDataButton != null) {
                mSendDataButton.setVisibility(View.GONE);
            }
        } else {
            if (mSendDataButton != null) {
                mSendDataButton.setVisibility(View.VISIBLE);
            }
        }

        boolean viewSent = sharedPreferences.getBoolean(
                AdminKeys.KEY_VIEW_SENT, true);
        if (!viewSent) {
            if (mViewSentFormsButton != null) {
                mViewSentFormsButton.setVisibility(View.GONE);
            }
        } else {
            if (mViewSentFormsButton != null) {
                mViewSentFormsButton.setVisibility(View.VISIBLE);
            }
        }

        boolean getBlank = sharedPreferences.getBoolean(
                AdminKeys.KEY_GET_BLANK, true);
        if (!getBlank) {
            if (mGetFormsButton != null) {
                mGetFormsButton.setVisibility(View.GONE);
            }
            if (mGetFormsSpacer != null) {
                mGetFormsSpacer.setVisibility(View.GONE);
            }
        } else {
            if (mGetFormsButton != null) {
                mGetFormsButton.setVisibility(View.VISIBLE);
            }
            if (mGetFormsSpacer != null) {
                mGetFormsSpacer.setVisibility(View.VISIBLE);
            }
        }

        boolean deleteSaved = sharedPreferences.getBoolean(
                AdminKeys.KEY_DELETE_SAVED, true);
        if (!deleteSaved) {
            if (mManageFilesButton != null) {
                mManageFilesButton.setVisibility(View.GONE);
            }
        } else {
            if (mManageFilesButton != null) {
                mManageFilesButton.setVisibility(View.VISIBLE);
            }
        }

        ((Collect) getApplication())
                .getDefaultTracker()
                .enableAutoActivityTracking(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Collect.getInstance().getActivityLogger().logOnStart(this);
    }

    @Override
    protected void onStop() {
        Collect.getInstance().getActivityLogger().logOnStop(this);
        super.onStop();
    }

    private void createErrorDialog(String errorMsg, final boolean shouldExit) {
        Collect.getInstance().getActivityLogger()
                .logAction(this, "createErrorDialog", "show");
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
        mAlertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Collect.getInstance()
                                .getActivityLogger()
                                .logAction(this, "createErrorDialog",
                                        shouldExit ? "exitApplication" : "OK");
                        if (shouldExit) {
                            finish();
                        }
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), errorListener);
        mAlertDialog.show();
    }

    private void updateButtons() {
        if (mFinalizedCursor != null && !mFinalizedCursor.isClosed()) {
            mFinalizedCursor.requery();
            mCompletedCount = mFinalizedCursor.getCount();
            if (mCompletedCount > 0) {
                mSendDataButton.setText(
                        getString(R.string.send_data_button, String.valueOf(mCompletedCount)));
            } else {
                mSendDataButton.setText(getString(R.string.send_data));
            }
        } else {
            mSendDataButton.setText(getString(R.string.send_data));
            Timber.w("Cannot update \"Send Finalized\" button label since the database is closed. "
                    + "Perhaps the app is running in the background?");
        }

        if (mSavedCursor != null && !mSavedCursor.isClosed()) {
            mSavedCursor.requery();
            mSavedCount = mSavedCursor.getCount();
            if (mSavedCount > 0) {
                mReviewDataButton.setText(getString(R.string.review_data_button,
                        String.valueOf(mSavedCount)));
            } else {
                mReviewDataButton.setText(getString(R.string.review_data));
            }
        } else {
            mReviewDataButton.setText(getString(R.string.review_data));
            Timber.w("Cannot update \"Edit Form\" button label since the database is closed. "
                    + "Perhaps the app is running in the background?");
        }

        if (mViewSentCursor != null && !mViewSentCursor.isClosed()) {
            mViewSentCursor.requery();
            mViewSentCount = mViewSentCursor.getCount();
            if (mViewSentCount > 0) {
                mViewSentFormsButton.setText(
                        getString(R.string.view_sent_forms_button, String.valueOf(mViewSentCount)));
            } else {
                mViewSentFormsButton.setText(getString(R.string.view_sent_forms));
            }
        } else {
            mViewSentFormsButton.setText(getString(R.string.view_sent_forms));
            Timber.w("Cannot update \"View Sent\" button label since the database is closed. "
                    + "Perhaps the app is running in the background?");
        }
    }

    /*
     * Used to prevent memory leaks
     */
    static class IncomingHandler extends Handler {
        private final WeakReference<CompletedFormsActivity> mTarget;

        IncomingHandler(CompletedFormsActivity target) {
            mTarget = new WeakReference<CompletedFormsActivity>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            CompletedFormsActivity target = mTarget.get();
            if (target != null) {
                //target.updateButtons();
            }
        }
    }

    /**
     * notifies us that something changed
     */
    private class MyContentObserver extends ContentObserver {

        public MyContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            mHandler.sendEmptyMessage(0);
        }
    }

}
