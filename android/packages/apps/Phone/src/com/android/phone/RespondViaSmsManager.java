/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Arrays;

/**
 * Helper class to manage the "Respond via SMS" feature for incoming calls.
 *
 * @see InCallScreen.internalRespondViaSms()
 */
public class RespondViaSmsManager {
    private static final String TAG = "RespondViaSmsManager";
    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    // Do not check in with VDBG = true, since that may write PII to the system log.
    private static final boolean VDBG = false;

    /**
     * Reference to the InCallScreen activity that owns us.  This may be
     * null if we haven't been initialized yet *or* after the InCallScreen
     * activity has been destroyed.
     */
    private InCallScreen mInCallScreen;

    /**
     * The popup showing the list of canned responses.
     *
     * This is an AlertDialog containing a ListView showing the possible
     * choices.  This may be null if the InCallScreen hasn't ever called
     * showRespondViaSmsPopup() yet, or if the popup was visible once but
     * then got dismissed.
     */
    private Dialog mPopup;

    /** The array of "canned responses"; see loadCannedResponses(). */
    private String[] mCannedResponses;

    /** SharedPreferences file name for our persistent settings. */
    private static final String SHARED_PREFERENCES_NAME = "respond_via_sms_prefs";

    // Preference keys for the 4 "canned responses"; see RespondViaSmsManager$Settings.
    // Since (for now at least) the number of messages is fixed at 4, and since
    // SharedPreferences can't deal with arrays anyway, just store the messages
    // as 4 separate strings.
    private static final int NUM_CANNED_RESPONSES = 4;
    private static final String KEY_CANNED_RESPONSE_PREF_1 = "canned_response_pref_1";
    private static final String KEY_CANNED_RESPONSE_PREF_2 = "canned_response_pref_2";
    private static final String KEY_CANNED_RESPONSE_PREF_3 = "canned_response_pref_3";
    private static final String KEY_CANNED_RESPONSE_PREF_4 = "canned_response_pref_4";

    private static final String ACTION_SENDTO_NO_CONFIRMATION =
            "com.android.mms.intent.action.SENDTO_NO_CONFIRMATION";

    /**
     * RespondViaSmsManager constructor.
     */
    public RespondViaSmsManager() {
    }

    public void setInCallScreenInstance(InCallScreen inCallScreen) {
        mInCallScreen = inCallScreen;

        if (mInCallScreen != null) {
            // Prefetch shared preferences to make the first canned response lookup faster
            // (and to prevent StrictMode violation)
            mInCallScreen.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        }
    }

    /**
     * Brings up the "Respond via SMS" popup for an incoming call.
     *
     * @param ringingCall the current incoming call
     */
    public void showRespondViaSmsPopup(Call ringingCall) {
        if (DBG) log("showRespondViaSmsPopup()...");

        // Very quick succession of clicks can cause this to run twice.
        // Stop here to avoid creating more than one popup.
        if (isShowingPopup()) {
            if (DBG) log("Skip showing popup when one is already shown.");
            return;
        }

        ListView lv = new ListView(mInCallScreen);

        // Refresh the array of "canned responses".
        mCannedResponses = loadCannedResponses();

        // Build the list: start with the canned responses, but manually add
        // "Custom message..." as the last choice.
        int numPopupItems = mCannedResponses.length + 1;
        String[] popupItems = Arrays.copyOf(mCannedResponses, numPopupItems);
        popupItems[numPopupItems - 1] = mInCallScreen.getResources()
                .getString(R.string.respond_via_sms_custom_message);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(mInCallScreen,
                                         android.R.layout.simple_list_item_1,
                                         android.R.id.text1,
                                         popupItems);
        lv.setAdapter(adapter);

        // Create a RespondViaSmsItemClickListener instance to handle item
        // clicks from the popup.
        // (Note we create a fresh instance for each incoming call, and
        // stash away the call's phone number, since we can't necessarily
        // assume this call will still be ringing when the user finally
        // chooses a response.)

        Connection c = ringingCall.getLatestConnection();
        if (VDBG) log("- connection: " + c);

        if (c == null) {
            // Uh oh -- the "ringingCall" doesn't have any connections any more.
            // (In other words, it's no longer ringing.)  This is rare, but can
            // happen if the caller hangs up right at the exact moment the user
            // selects the "Respond via SMS" option.
            // There's nothing to do here (since the incoming call is gone),
            // so just bail out.
            Log.i(TAG, "showRespondViaSmsPopup: null connection; bailing out...");
            return;
        }

        // TODO: at this point we probably should re-check c.getAddress()
        // and c.getNumberPresentation() for validity.  (i.e. recheck the
        // same cases in InCallTouchUi.showIncomingCallWidget() where we
        // should have disallowed the "respond via SMS" feature in the
        // first place.)

        String phoneNumber = c.getAddress();
        if (VDBG) log("- phoneNumber: " + phoneNumber);
        lv.setOnItemClickListener(new RespondViaSmsItemClickListener(phoneNumber));

        AlertDialog.Builder builder = new AlertDialog.Builder(mInCallScreen)
                .setCancelable(true)
                .setOnCancelListener(new RespondViaSmsCancelListener())
                .setView(lv);
        mPopup = builder.create();
        mPopup.show();
    }

    /**
     * Dismiss the "Respond via SMS" popup if it's visible.
     *
     * This is safe to call even if the popup is already dismissed, and
     * even if you never called showRespondViaSmsPopup() in the first
     * place.
     */
    public void dismissPopup() {
        if (mPopup != null) {
            mPopup.dismiss();  // safe even if already dismissed
            mPopup = null;
        }
    }

    public boolean isShowingPopup() {
        return mPopup != null && mPopup.isShowing();
    }

    /**
     * OnItemClickListener for the "Respond via SMS" popup.
     */
    public class RespondViaSmsItemClickListener implements AdapterView.OnItemClickListener {
        // Phone number to send the SMS to.
        private String mPhoneNumber;

        public RespondViaSmsItemClickListener(String phoneNumber) {
            mPhoneNumber = phoneNumber;
        }

        /**
         * Handles the user selecting an item from the popup.
         */
        @Override
        public void onItemClick(AdapterView<?> parent,  // The ListView
                                View view,  // The TextView that was clicked
                                int position,
                                long id) {
            if (DBG) log("RespondViaSmsItemClickListener.onItemClick(" + position + ")...");
            String message = (String) parent.getItemAtPosition(position);
            if (VDBG) log("- message: '" + message + "'");

            // The "Custom" choice is a special case.
            // (For now, it's guaranteed to be the last item.)
            if (position == (parent.getCount() - 1)) {
                // Take the user to the standard SMS compose UI.
                launchSmsCompose(mPhoneNumber);
            } else {
                // Send the selected message immediately with no user interaction.
                sendText(mPhoneNumber, message);

                // ...and show a brief confirmation to the user (since
                // otherwise it's hard to be sure that anything actually
                // happened.)
                final Resources res = mInCallScreen.getResources();
                String formatString = res.getString(R.string.respond_via_sms_confirmation_format);
                String confirmationMsg = String.format(formatString, mPhoneNumber);
                Toast.makeText(mInCallScreen,
                               confirmationMsg,
                               Toast.LENGTH_LONG).show();

                // TODO: If the device is locked, this toast won't actually ever
                // be visible!  (That's because we're about to dismiss the call
                // screen, which means that the device will return to the
                // keyguard.  But toasts aren't visible on top of the keyguard.)
                // Possible fixes:
                // (1) Is it possible to allow a specific Toast to be visible
                //     on top of the keyguard?
                // (2) Artifically delay the dismissCallScreen() call by 3
                //     seconds to allow the toast to be seen?
                // (3) Don't use a toast at all; instead use a transient state
                //     of the InCallScreen (perhaps via the InCallUiState
                //     progressIndication feature), and have that state be
                //     visible for 3 seconds before calling dismissCallScreen().
            }

            // At this point the user is done dealing with the incoming call, so
            // there's no reason to keep it around.  (It's also confusing for
            // the "incoming call" icon in the status bar to still be visible.)
            // So reject the call now.
            mInCallScreen.hangupRingingCall();

            dismissPopup();

            final PhoneConstants.State state = PhoneGlobals.getInstance().mCM.getState();
            if (state == PhoneConstants.State.IDLE) {
                // There's no other phone call to interact. Exit the entire in-call screen.
                PhoneGlobals.getInstance().dismissCallScreen();
            } else {
                // The user is still in the middle of other phone calls, so we should keep the
                // in-call screen.
                mInCallScreen.requestUpdateScreen();
            }
        }
    }

    /**
     * OnCancelListener for the "Respond via SMS" popup.
     */
    public class RespondViaSmsCancelListener implements DialogInterface.OnCancelListener {
        public RespondViaSmsCancelListener() {
        }

        /**
         * Handles the user canceling the popup, either by touching
         * outside the popup or by pressing Back.
         */
        @Override
        public void onCancel(DialogInterface dialog) {
            if (DBG) log("RespondViaSmsCancelListener.onCancel()...");

            dismissPopup();

            final PhoneConstants.State state = PhoneGlobals.getInstance().mCM.getState();
            if (state == PhoneConstants.State.IDLE) {
                // This means the incoming call is already hung up when the user chooses not to
                // use "Respond via SMS" feature. Let's just exit the whole in-call screen.
                PhoneGlobals.getInstance().dismissCallScreen();
            } else {

                // If the user cancels the popup, this presumably means that
                // they didn't actually mean to bring up the "Respond via SMS"
                // UI in the first place (and instead want to go back to the
                // state where they can either answer or reject the call.)
                // So restart the ringer and bring back the regular incoming
                // call UI.

                // This will have no effect if the incoming call isn't still ringing.
                PhoneGlobals.getInstance().notifier.restartRinger();

                // We hid the GlowPadView widget way back in
                // InCallTouchUi.onTrigger(), when the user first selected
                // the "SMS" trigger.
                //
                // To bring it back, just force the entire InCallScreen to
                // update itself based on the current telephony state.
                // (Assuming the incoming call is still ringing, this will
                // cause the incoming call widget to reappear.)
                mInCallScreen.requestUpdateScreen();
            }
        }
    }

    /**
     * Sends a text message without any interaction from the user.
     */
    private void sendText(String phoneNumber, String message) {
        if (VDBG) log("sendText: number "
                      + phoneNumber + ", message '" + message + "'");

        mInCallScreen.startService(getInstantTextIntent(phoneNumber, message));
    }

    /**
     * Brings up the standard SMS compose UI.
     */
    private void launchSmsCompose(String phoneNumber) {
        if (VDBG) log("launchSmsCompose: number " + phoneNumber);

        Intent intent = getInstantTextIntent(phoneNumber, null);

        if (VDBG) log("- Launching SMS compose UI: " + intent);
        mInCallScreen.startService(intent);
    }

    /**
     * @param phoneNumber Must not be null.
     * @param message Can be null. If message is null, the returned Intent will be configured to
     * launch the SMS compose UI. If non-null, the returned Intent will cause the specified message
     * to be sent with no interaction from the user.
     * @return Service Intent for the instant response.
     */
    private static Intent getInstantTextIntent(String phoneNumber, String message) {
        Uri uri = Uri.fromParts(Constants.SCHEME_SMSTO, phoneNumber, null);
        Intent intent = new Intent(ACTION_SENDTO_NO_CONFIRMATION, uri);
        if (message != null) {
            intent.putExtra(Intent.EXTRA_TEXT, message);
        } else {
            intent.putExtra("exit_on_sent", true);
            intent.putExtra("showUI", true);
        }
        return intent;
    }

    /**
     * Settings activity under "Call settings" to let you manage the
     * canned responses; see respond_via_sms_settings.xml
     */
    public static class Settings extends PreferenceActivity
            implements Preference.OnPreferenceChangeListener {
        @Override
        protected void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            if (DBG) log("Settings: onCreate()...");

            getPreferenceManager().setSharedPreferencesName(SHARED_PREFERENCES_NAME);

            // This preference screen is ultra-simple; it's just 4 plain
            // <EditTextPreference>s, one for each of the 4 "canned responses".
            //
            // The only nontrivial thing we do here is copy the text value of
            // each of those EditTextPreferences and use it as the preference's
            // "title" as well, so that the user will immediately see all 4
            // strings when they arrive here.
            //
            // Also, listen for change events (since we'll need to update the
            // title any time the user edits one of the strings.)

            addPreferencesFromResource(R.xml.respond_via_sms_settings);

            EditTextPreference pref;
            pref = (EditTextPreference) findPreference(KEY_CANNED_RESPONSE_PREF_1);
            pref.setTitle(pref.getText());
            pref.setOnPreferenceChangeListener(this);

            pref = (EditTextPreference) findPreference(KEY_CANNED_RESPONSE_PREF_2);
            pref.setTitle(pref.getText());
            pref.setOnPreferenceChangeListener(this);

            pref = (EditTextPreference) findPreference(KEY_CANNED_RESPONSE_PREF_3);
            pref.setTitle(pref.getText());
            pref.setOnPreferenceChangeListener(this);

            pref = (EditTextPreference) findPreference(KEY_CANNED_RESPONSE_PREF_4);
            pref.setTitle(pref.getText());
            pref.setOnPreferenceChangeListener(this);

            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                // android.R.id.home will be triggered in onOptionsItemSelected()
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        // Preference.OnPreferenceChangeListener implementation
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (DBG) log("onPreferenceChange: key = " + preference.getKey());
            if (VDBG) log("  preference = '" + preference + "'");
            if (VDBG) log("  newValue = '" + newValue + "'");

            EditTextPreference pref = (EditTextPreference) preference;

            // Copy the new text over to the title, just like in onCreate().
            // (Watch out: onPreferenceChange() is called *before* the
            // Preference itself gets updated, so we need to use newValue here
            // rather than pref.getText().)
            pref.setTitle((String) newValue);

            return true;  // means it's OK to update the state of the Preference with the new value
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            final int itemId = item.getItemId();
            if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
                CallFeaturesSetting.goUpToTopLevelSetting(this);
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Read the (customizable) canned responses from SharedPreferences,
     * or from defaults if the user has never actually brought up
     * the Settings UI.
     *
     * This method does disk I/O (reading the SharedPreferences file)
     * so don't call it from the main thread.
     *
     * @see RespondViaSmsManager.Settings
     */
    private String[] loadCannedResponses() {
        if (DBG) log("loadCannedResponses()...");

        SharedPreferences prefs =
                mInCallScreen.getSharedPreferences(SHARED_PREFERENCES_NAME,
                                                   Context.MODE_PRIVATE);
        final Resources res = mInCallScreen.getResources();

        String[] responses = new String[NUM_CANNED_RESPONSES];

        // Note the default values here must agree with the corresponding
        // android:defaultValue attributes in respond_via_sms_settings.xml.

        responses[0] = prefs.getString(KEY_CANNED_RESPONSE_PREF_1,
                                       res.getString(R.string.respond_via_sms_canned_response_1));
        responses[1] = prefs.getString(KEY_CANNED_RESPONSE_PREF_2,
                                       res.getString(R.string.respond_via_sms_canned_response_2));
        responses[2] = prefs.getString(KEY_CANNED_RESPONSE_PREF_3,
                                       res.getString(R.string.respond_via_sms_canned_response_3));
        responses[3] = prefs.getString(KEY_CANNED_RESPONSE_PREF_4,
                                       res.getString(R.string.respond_via_sms_canned_response_4));
        return responses;
    }

    /**
     * @return true if the "Respond via SMS" feature should be enabled
     * for the specified incoming call.
     *
     * The general rule is that we *do* allow "Respond via SMS" except for
     * the few (relatively rare) cases where we know for sure it won't
     * work, namely:
     *   - a bogus or blank incoming number
     *   - a call from a SIP address
     *   - a "call presentation" that doesn't allow the number to be revealed
     *
     * In all other cases, we allow the user to respond via SMS.
     *
     * Note that this behavior isn't perfect; for example we have no way
     * to detect whether the incoming call is from a landline (with most
     * networks at least), so we still enable this feature even though
     * SMSes to that number will silently fail.
     */
    public static boolean allowRespondViaSmsForCall(Context context, Call ringingCall) {
        if (DBG) log("allowRespondViaSmsForCall(" + ringingCall + ")...");

        // First some basic sanity checks:
        if (ringingCall == null) {
            Log.w(TAG, "allowRespondViaSmsForCall: null ringingCall!");
            return false;
        }
        if (!ringingCall.isRinging()) {
            // The call is in some state other than INCOMING or WAITING!
            // (This should almost never happen, but it *could*
            // conceivably happen if the ringing call got disconnected by
            // the network just *after* we got it from the CallManager.)
            Log.w(TAG, "allowRespondViaSmsForCall: ringingCall not ringing! state = "
                  + ringingCall.getState());
            return false;
        }
        Connection conn = ringingCall.getLatestConnection();
        if (conn == null) {
            // The call doesn't have any connections!  (Again, this can
            // happen if the ringing call disconnects at the exact right
            // moment, but should almost never happen in practice.)
            Log.w(TAG, "allowRespondViaSmsForCall: null Connection!");
            return false;
        }

        // Check the incoming number:
        final String number = conn.getAddress();
        if (DBG) log("- number: '" + number + "'");
        if (TextUtils.isEmpty(number)) {
            Log.w(TAG, "allowRespondViaSmsForCall: no incoming number!");
            return false;
        }
        if (PhoneNumberUtils.isUriNumber(number)) {
            // The incoming number is actually a URI (i.e. a SIP address),
            // not a regular PSTN phone number, and we can't send SMSes to
            // SIP addresses.
            // (TODO: That might still be possible eventually, though.  Is
            // there some SIP-specific equivalent to sending a text message?)
            Log.i(TAG, "allowRespondViaSmsForCall: incoming 'number' is a SIP address.");
            return false;
        }

        // Finally, check the "call presentation":
        int presentation = conn.getNumberPresentation();
        if (DBG) log("- presentation: " + presentation);
        if (presentation == PhoneConstants.PRESENTATION_RESTRICTED) {
            // PRESENTATION_RESTRICTED means "caller-id blocked".
            // The user isn't allowed to see the number in the first
            // place, so obviously we can't let you send an SMS to it.
            Log.i(TAG, "allowRespondViaSmsForCall: PRESENTATION_RESTRICTED.");
            return false;
        }

        // Allow the feature only when there's a destination for it.
        if (context.getPackageManager().resolveService(getInstantTextIntent(number, null) , 0)
                == null) {
            return false;
        }

        // TODO: with some carriers (in certain countries) you *can* actually
        // tell whether a given number is a mobile phone or not.  So in that
        // case we could potentially return false here if the incoming call is
        // from a land line.

        // If none of the above special cases apply, it's OK to enable the
        // "Respond via SMS" feature.
        return true;
    }


    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
