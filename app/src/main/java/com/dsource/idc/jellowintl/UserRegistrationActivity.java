package com.dsource.idc.jellowintl;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

import com.crashlytics.android.Crashlytics;
import com.dsource.idc.jellowintl.TalkBack.TalkbackHints_DropDownMenu;
import com.dsource.idc.jellowintl.factories.LanguageFactory;
import com.dsource.idc.jellowintl.models.GlobalConstants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.messaging.FirebaseMessaging;
import com.hbb20.CountryCodePicker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import static com.dsource.idc.jellowintl.utility.Analytics.bundleEvent;
import static com.dsource.idc.jellowintl.utility.Analytics.getAnalytics;
import static com.dsource.idc.jellowintl.utility.Analytics.setCrashlyticsCustomKey;
import static com.dsource.idc.jellowintl.utility.Analytics.setUserProperty;
import static com.dsource.idc.jellowintl.utility.SessionManager.LangMap;
import static com.dsource.idc.jellowintl.utility.SessionManager.MR_IN;
import static com.dsource.idc.jellowintl.utility.SessionManager.UNIVERSAL_PACKAGE;

/**
 * Created by Rahul on 12 Nov, 2019.
 */
public class UserRegistrationActivity extends BaseActivity implements CheckNetworkStatus {
    public static final String LCODE = "lcode";
    public static final String TUTORIAL = "tutorial";

    private Button bRegister;
    private EditText etName, etEmergencyContact, etAddress;
    private DatabaseReference mRef;
    private CountryCodePicker mCcp;
    Spinner languageSelect;
    String[] languagesCodes = new String[LangMap.size()],
            languageNames = new String[LangMap.size()];
    String selectedLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);
        FirebaseMessaging.getInstance().subscribeToTopic("jellow_aac");
        setActivityTitle(getString(R.string.menuUserRegistration));
        findViewById(R.id.iv_action_bar_back).setVisibility(View.GONE);
        getSession().changePreferencesFile(this);

        if (!getSession().getUserId().equals("")) {
            getAnalytics(this, getSession().getUserId());
            getSession().setSessionCreatedAt(new Date().getTime());
            Crashlytics.setUserIdentifier(getSession().getUserId());
        }

        if (getSession().isUserLoggedIn()) {
            if (LanguageFactory.isLanguageDataAvailable(this) && getSession().isCompletedIntro()) {
                startActivity(new Intent(this, SplashActivity.class));
            }else if(!LanguageFactory.isLanguageDataAvailable(this)){
                startActivity(new Intent(UserRegistrationActivity.this,
                        LanguageDownloadActivity.class)
                        .putExtra(LCODE, UNIVERSAL_PACKAGE).putExtra(TUTORIAL, true));
            }else if(getSession().getLanguage().equals(MR_IN) && !LanguageFactory.
                    isMarathiPackageAvailable(this)){
                startActivity(new Intent(UserRegistrationActivity.this,
                        LanguageDownloadActivity.class)
                        .putExtra(LCODE, MR_IN).putExtra(TUTORIAL, true));
            } else if (LanguageFactory.isLanguageDataAvailable(this) &&
                    !getSession().isCompletedIntro()) {
                startActivity(new Intent(this, Intro.class));
            }
            finish();
        } else {
            getSession().setBlood(-1);
        }

        FirebaseDatabase mDB = FirebaseDatabase.getInstance();
        mRef = mDB.getReference(BuildConfig.DB_TYPE + "/users");

        languagesCodes = LanguageFactory.getAvailableLanguages();
        languageNames = LanguageFactory.getAvailableLanguages();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        etName = findViewById(R.id.etName);
        etName.clearFocus();
        ((TextView)findViewById(R.id.tv_pivacy_link)).setText(Html.fromHtml(getString(R.string.privacy_link_info)));
        etEmergencyContact = findViewById(R.id.etEmergencyContact);
        mCcp = findViewById(R.id.ccp);
        if (isAccessibilityTalkBackOn((AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE)))
            mCcp.setCountryPreference(null);
        ViewCompat.setAccessibilityDelegate(mCcp, new TalkbackHints_DropDownMenu());
        mCcp.registerCarrierNumberEditText(etEmergencyContact);
        //This listener is useful only when Talkback accessibility is "ON".
        mCcp.setDialogEventsListener(new CountryCodePicker.DialogEventsListener() {
            @Override public void onCcpDialogOpen(Dialog dialog) {}

            @Override
            public void onCcpDialogDismiss(DialogInterface dialogInterface) {
                etEmergencyContact.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
            }
            @Override public void onCcpDialogCancel(DialogInterface dialogInterface) {}
        });
        etAddress = findViewById(R.id.etAddress);
        bRegister = findViewById(R.id.bRegister);
        findViewById(R.id.childName).setFocusableInTouchMode(true);
        findViewById(R.id.childName).setFocusable(true);

        languageSelect = findViewById(R.id.langSelectSpinner);
        ArrayAdapter<String> adapter_lan = new ArrayAdapter<>(this,
                R.layout.simple_spinner_item, populateCountryNameByUserType(languageNames));
        adapter_lan.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSelect.setAdapter(adapter_lan);
        languageSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedLanguage = languagesCodes[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                selectedLanguage = null;
            }
        });

        bRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bRegister.setEnabled(false);

                if (etName.getText().toString().trim().isEmpty()) {
                    bRegister.setEnabled(true);
                    Toast.makeText(UserRegistrationActivity.this,
                            getString(R.string.enterTheName), Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!etEmergencyContact.getText().toString().matches("[0-9]+")){
                    bRegister.setEnabled(true);
                    Toast.makeText(UserRegistrationActivity.this,
                            getString(R.string.enternonemptycontact), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (etAddress.getText().toString().trim().isEmpty()){
                    bRegister.setEnabled(true);
                    Toast.makeText(UserRegistrationActivity.this,getString(R.string.invalid_address),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                CheckBox cb = findViewById(R.id.cb_privacy_consent);
                if (!cb.isChecked()){
                    bRegister.setEnabled(true);
                    Toast.makeText(UserRegistrationActivity.this,
                            getString(R.string.consent_privacy), Toast.LENGTH_SHORT).show();
                    return;
                }

                if(selectedLanguage == null)
                    return;

                new NetworkConnectionTest(UserRegistrationActivity.this).execute();
            }
        });

        if(!getSession().getName().isEmpty()){
            etName.setText(getSession().getName());
            etEmergencyContact.setText(getSession().getCaregiverNumber());
            etAddress.setText(getSession().getAddress());
        }
    }

    private String[] populateCountryNameByUserType(String[] langNameToBeShorten) {
        String[] shortenLanguageNames = new String[langNameToBeShorten.length];
        if (isAccessibilityTalkBackOn((AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE))) {
            for (int i = 0; i < langNameToBeShorten.length; i++) {
                switch (langNameToBeShorten[i]) {
                    case "मराठी":
                        shortenLanguageNames[i] = getString(R.string.acc_lang_marathi);
                        break;
                    case "हिंदी":
                        shortenLanguageNames[i] = getString(R.string.acc_lang_hindi);
                        break;
                    case "বাঙালি":
                        shortenLanguageNames[i] = getString(R.string.acc_lang_bengali);
                        break;
                    case "English (India)":
                        shortenLanguageNames[i] = getString(R.string.acc_lang_eng_in);
                        break;
                    case "English (United Kingdom)":
                        shortenLanguageNames[i] = getString(R.string.acc_lang_eng_gb);
                        break;
                    case "English (United States)":
                        shortenLanguageNames[i] = getString(R.string.acc_lang_eng_us);
                        break;
                    case "English (Australia)":
                        shortenLanguageNames[i] = getString(R.string.acc_lang_eng_au);
                        break;
                    case "Spanish (Spain)":
                        shortenLanguageNames[i] = getString(R.string.acc_lang_span_span);
                        break;
                    case "தமிழ்":
                        shortenLanguageNames[i] = getString(R.string.acc_lang_tamil_in);
                        break;
                    case "German (Deutschland)":
                        shortenLanguageNames[i] = getString(R.string.acc_lang_german_ger);
                        break;
                    default:
                        shortenLanguageNames[i] = langNameToBeShorten[i];
                        break;
                }
            }
        }else{
            for (int i = 0; i < langNameToBeShorten.length; i++) {
                switch (langNameToBeShorten[i]) {
                    case "English (India)":
                        shortenLanguageNames[i] = "English (IN)";
                        break;
                    case "English (United Kingdom)":
                        shortenLanguageNames[i] = "English (UK)";
                        break;
                    case "English (United States)":
                        shortenLanguageNames[i] = "English (US)";
                        break;
                    case "English (Australia)":
                        shortenLanguageNames[i] = "English (AU)";
                        break;
                    case "Spanish (Spain)":
                        shortenLanguageNames[i] = "Spanish (ES)";
                        break;
                    case "Tamil (India)":
                        shortenLanguageNames[i] = "Tamil (IN)";
                        break;
                    case "German (Deutschland)":
                        shortenLanguageNames[i] = "German (DE)";
                        break;
                    case "French (France)":
                        shortenLanguageNames[i] = "French (FR)";
                        break;
                    default:
                        shortenLanguageNames[i] = langNameToBeShorten[i];
                        break;
                }
            }
        }
        return shortenLanguageNames;
    }

    @Override
    public void onReceiveNetworkState(int state) {
        if (state == GlobalConstants.NETWORK_CONNECTED){
            Toast.makeText(UserRegistrationActivity.this,
                    getString(R.string.register_user), Toast.LENGTH_SHORT).show();
            autoLoginAndSetupUser();
        }else {
            bRegister.setEnabled(true);
            Toast.makeText(UserRegistrationActivity.this,
                    getString(R.string.checkConnectivity), Toast.LENGTH_LONG).show();
        }
    }

    private void autoLoginAndSetupUser() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();
        mAuth.signInAnonymously()
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Crashlytics.log("User logged in");
                            getSession().setName(etName.getText().toString().trim());
                            getSession().setCaregiverNumber(mCcp.getFullNumberWithPlus());
                            getSession().setUserCountryCode(mCcp.getSelectedCountryCode());
                            getSession().setAddress(etAddress.getText().toString().trim());
                            getSession().setUserId(UUID.randomUUID().toString());

                            getAnalytics(UserRegistrationActivity.this, getSession().getUserId());
                            getSession().setSessionCreatedAt(new Date().getTime());
                            HashMap<String, Object> map = new HashMap<>();
                            map.put("firstLanguage", selectedLanguage);
                            map.put("versionCode", BuildConfig.VERSION_CODE);
                            map.put("joinedOn", ServerValue.TIMESTAMP);
                            mRef.child(getSession().getUserId()).setValue(map)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        getSession().setUserLoggedIn(true);
                                        getSession().setLanguage(LangMap.get(selectedLanguage));
                                        getSession().setGridSize(GlobalConstants.NINE_ICONS_PER_SCREEN);
                                        Bundle bundle = new Bundle();
                                        bundle.putString("LanguageSet", "First time "+ LangMap.get(selectedLanguage));
                                        setUserProperty("UserId", getSession().getUserId());
                                        setUserProperty("UserLanguage", LangMap.get(selectedLanguage));
                                        setUserProperty("GridSize", "9");
                                        setUserProperty("PictureViewMode", "PictureText");
                                        bundleEvent("Language", bundle);
                                        Crashlytics.setUserIdentifier(getSession().getUserId());
                                        setCrashlyticsCustomKey("GridSize", "9");
                                        setCrashlyticsCustomKey("PictureViewMode", "PictureText");
                                        bundle.clear();
                                        bundle.putString(LCODE, UNIVERSAL_PACKAGE);
                                        bundle.putBoolean(TUTORIAL, true);
                                        startActivity(new Intent(UserRegistrationActivity.this,
                                                LanguageDownloadActivity.class).putExtras(bundle));
                                        finish();
                                    } else {
                                        bRegister.setEnabled(true);
                                        Crashlytics.log("User data not added.");
                                        Toast.makeText(UserRegistrationActivity.this,
                                                getString(R.string.checkConnectivity), Toast.LENGTH_LONG).show();
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    bRegister.setEnabled(true);
                                    Crashlytics.log("User data not added.");
                                    Crashlytics.logException(e);
                                    Toast.makeText(UserRegistrationActivity.this,
                                            getString(R.string.error_in_registration), Toast.LENGTH_SHORT).show();
                                }
                            });

                        }else
                            Toast.makeText(UserRegistrationActivity.this,
                                    getString(R.string.error_in_registration), Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(UserRegistrationActivity.this,
                        e.getMessage(), Toast.LENGTH_SHORT).show();
                bRegister.setEnabled(true);
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    private class NetworkConnectionTest extends AsyncTask<Void, Void, Boolean> {
        private Context context;
        private CheckNetworkStatus listener;

        NetworkConnectionTest(Context context) {
            this.context = context;
            this.listener = (CheckNetworkStatus) context;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if(!isConnectedToNetwork((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)))
                return false;
            try {
                URL url = new URL("http://www.google.com");
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setConnectTimeout(3000);
                urlc.connect();
                if (urlc.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    urlc.disconnect();
                    return true;
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean isConnected) {
            super.onPostExecute(isConnected);
            if(isConnected){
                listener.onReceiveNetworkState(GlobalConstants.NETWORK_CONNECTED);
            }else{
                listener.onReceiveNetworkState(GlobalConstants.NETWORK_DISCONNECTED);
            }
        }
    }
}