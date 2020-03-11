/*
Sina Gharavi
*/

package com.deitel.animalquiz;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.RawRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    PlayGroundFragment playgroundragment;
    Button playButton;
    MediaPlayer player;
    public boolean phoneDevice = true; // used to force portrait mode
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playButton = (Button) findViewById(R.id.Button_Play);
        playButton.setOnClickListener(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set default values in the app's SharedPreferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // determine screen size
        int screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;

        // if device is a tablet, set phoneDevice to false
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE)
            phoneDevice = false; // not a phone-sized device

        // if running on phone-sized device, allow only portrait orientation
        if (phoneDevice)
            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        timer = new Timer();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.Button_Play){
            play(R.raw.horsewhinnying, PlayGroundFragment.soundOptions);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Intent playground = new Intent(MainActivity.this, PlayGroundActivity.class);
                    startActivity(playground);
                }
            }, 2000);
        }
    }

    public void play(int resource, boolean soundOptions){
            if(player == null) {
                player = MediaPlayer.create(this, resource);
            }
        if (soundOptions){
            player.start(); // no need to call prepare(); create() does that for you
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stop();
                }
            });
        }
    }

    public void stop(){
        if(player != null){
            player.release();
            player = null;
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        stop();
    }

    // show menu if app is running on a phone or a portrait-oriented tablet
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        // display the app's menu only in portrait orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // inflate the menu
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }
        else
            return false;
    }

    // displays the SettingsActivity when running on a phone
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent preferencesIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferencesIntent);
        return super.onOptionsItemSelected(item);
    }
}
