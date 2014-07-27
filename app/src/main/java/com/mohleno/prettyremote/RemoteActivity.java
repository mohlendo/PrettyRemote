package com.mohleno.prettyremote;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import com.mohleno.prettyremote.services.Device;
import com.mohleno.prettyremote.services.LGCommand;
import com.mohleno.prettyremote.services.LGConnectService;

import java.io.IOException;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class RemoteActivity extends Activity {

    private Device device;
    private LGConnectService lgConnectService;

    public static final String DEVICE_INTENT_KEY = "device";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);

        device = (Device) getIntent().getSerializableExtra(DEVICE_INTENT_KEY);
        lgConnectService = LGConnectService.getInstance(this);

        findViewById(R.id.button_mute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand(LGCommand.MUTE);
            }
        });
    }

    private void sendCommand(LGCommand command) {
        new AsyncTask<LGCommand, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(LGCommand... lgCommands) {
                try {
                    return lgConnectService.sendCommand(device, lgCommands[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (!result) {
                    Toast.makeText(getApplicationContext(), R.string.toast_command_error, Toast.LENGTH_LONG).show();
                }
            }
        }.execute(command);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hideSystemUI();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    // This snippet shows the system bars. It does this by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
