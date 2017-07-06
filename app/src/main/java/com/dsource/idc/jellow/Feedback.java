package com.dsource.idc.jellow;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;

import com.dsource.idc.jellow.Utility.SessionManager;

public class Feedback extends AppCompatActivity {
    private RatingBar mRatingEasyToUse;
    private Button mBtnSubmit;
    private EditText mEtComments;
    private String strEaseOfUse, mClearPicture, mClearVoice, mEaseToNav;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(Html.fromHtml("<font color='#F7F3C6'>" + getString(R.string.menuFeedback) + "</font>"));
        {
            SessionManager session = new SessionManager(getApplicationContext());
            if (session.getScreenHeight() >= 600) {
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_navigation_arrow_back_600);
            }
            else {
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_navigation_arrow_back);
            }
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        mBtnSubmit = (Button) findViewById(R.id.bSubmit);
        addListenerOnRatingBar();
        addListenerOnButton();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.getItem(4).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.profile: startActivity(new Intent(Feedback.this, ProfileForm.class)); finish(); break;
            case R.id.info: startActivity(new Intent(Feedback.this, AboutJellow.class)); finish(); break;
            case R.id.usage: startActivity(new Intent(Feedback.this, Tutorial.class)); finish(); break;
            case R.id.keyboardinput: startActivity(new Intent(Feedback.this, KeyboardInput.class)); finish(); break;
            case R.id.settings: startActivity(new Intent(Feedback.this, Setting.class)); finish(); break;
            case R.id.reset: startActivity(new Intent(Feedback.this, ResetPreferences.class)); finish(); break;
            case android.R.id.home: finish(); break;
            default: return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void addListenerOnRatingBar() {
        mRatingEasyToUse = (RatingBar) findViewById(R.id.easy_to_use);
        Drawable progress1 = mRatingEasyToUse.getProgressDrawable();
        DrawableCompat.setTint(progress1, Color.argb(255, 255, 204, 20));
        RatingBar pictures = (RatingBar) findViewById(R.id.pictures);
        Drawable progress2 = pictures.getProgressDrawable();
        DrawableCompat.setTint(progress2, Color.argb(255, 255, 204, 20));
        RatingBar voice = (RatingBar) findViewById(R.id.voice);
        Drawable progress3 = voice.getProgressDrawable();
        DrawableCompat.setTint(progress3, Color.argb(255, 255, 204, 20));
        RatingBar navigate = (RatingBar) findViewById(R.id.navigate);
        Drawable progress4 = navigate.getProgressDrawable();
        DrawableCompat.setTint(progress4, Color.argb(255, 255, 204, 20));

        mRatingEasyToUse.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
            public void onRatingChanged(RatingBar ratingBar, float rating,
                                        boolean fromUser) {
                strEaseOfUse = String.valueOf(rating);
            }
        });
        pictures.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
            public void onRatingChanged(RatingBar ratingBar, float rating,
                                        boolean fromUser) {
                mClearPicture = String.valueOf(rating);
            }
        });
        voice.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
            public void onRatingChanged(RatingBar ratingBar, float rating,
                                        boolean fromUser) {
                mClearVoice = String.valueOf(rating);
            }
        });
        navigate.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
            public void onRatingChanged(RatingBar ratingBar, float rating,
                                        boolean fromUser) {
                mEaseToNav = String.valueOf(rating);
            }
        });
    }

    public void addListenerOnButton() {
        mRatingEasyToUse = (RatingBar) findViewById(R.id.easy_to_use);
        mEtComments = (EditText)findViewById(R.id.comments);
        mBtnSubmit = (Button) findViewById(R.id.bSubmit);
        mBtnSubmit.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String cs  = mEtComments.getText().toString();
                Intent email = new Intent(Intent.ACTION_SEND);
                email.putExtra(Intent.EXTRA_EMAIL, new String[]{"dsource.in@gmail.com"});
                email.putExtra(Intent.EXTRA_SUBJECT, "Jellow Feedback");
                email.putExtra(Intent.EXTRA_TEXT, "Easy to use: "+ strEaseOfUse +"\nClear Pictures: "+ mClearPicture +"\nClear Voices: "+ mClearVoice +"\nEasy to Navigate: "+ mEaseToNav + "\n\nComments and Suggestions:-\n"+cs);
                email.setType("message/rfc822");
                startActivity(Intent.createChooser(email, "Choose an Email client :"));
            }
        });
    }
}