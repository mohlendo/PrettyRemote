package com.mohleno.prettyremote;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;

import com.mohleno.prettyremote.services.Device;
import com.mohleno.prettyremote.services.LGConnectService;
import com.mohleno.prettyremote.services.LGKey;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class RemoteActivity extends Activity {
    private final static String TAG = RemoteActivity.class.getSimpleName();
    public static final String DEVICE_INTENT_KEY = "device";
    private Device device;
    private ExecutorService service;
    private LGConnectService lgConnectService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);

        device = (Device) getIntent().getSerializableExtra(DEVICE_INTENT_KEY);
        lgConnectService = LGConnectService.getInstance(this);


        findViewById(R.id.button_volume_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendKeyCommand(LGKey.VOLUME_UP);
            }
        });

        findViewById(R.id.button_volume_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendKeyCommand(LGKey.VOLUME_DOWN);
            }
        });


        findViewById(R.id.button_mute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendKeyCommand(LGKey.MUTE);
            }
        });

        findViewById(R.id.button_channel_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendKeyCommand(LGKey.CHANNEL_UP);
            }
        });

        findViewById(R.id.button_channel_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendKeyCommand(LGKey.CHANNEL_DOWN);
            }
        });

        findViewById(R.id.button_touch_area).setOnTouchListener(new View.OnTouchListener() {

            // The ‘active pointer’ is the one currently moving our object.
            private int mActivePointerId = 0;
            public float mLastTouchX
                    ,
                    distanceX;
            public float mLastTouchY
                    ,
                    distanceY;

            @Override
            public boolean onTouch(View view, MotionEvent ev) {
                final int action = MotionEventCompat.getActionMasked(ev);

                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
                        final float x = MotionEventCompat.getX(ev, pointerIndex);
                        final float y = MotionEventCompat.getY(ev, pointerIndex);

                        // Remember where we started (for dragging)
                        mLastTouchX = x;
                        mLastTouchY = y;
                        distanceX = 0;
                        distanceY = 0;

                        // Save the ID of this pointer (for dragging)
                        mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                        break;
                    }

                    case MotionEvent.ACTION_MOVE: {
                        // Find the index of the active pointer and fetch its position
                        final int pointerIndex =
                                MotionEventCompat.findPointerIndex(ev, mActivePointerId);

                        final float x = MotionEventCompat.getX(ev, pointerIndex);
                        final float y = MotionEventCompat.getY(ev, pointerIndex);

                        // Calculate the distance moved
                        final float dx = x - mLastTouchX;
                        final float dy = y - mLastTouchY;
                        distanceY += dy;
                        distanceX += dx;

                        sendTouchMove(new Point((int) dx, (int) dy));

                        // Remember this touch position for the next move event
                        mLastTouchX = x;
                        mLastTouchY = y;

                        break;
                    }

                    case MotionEvent.ACTION_UP: {
                        mActivePointerId = 0;
                        if (Math.abs(distanceX) <= 10 && Math.abs(distanceY) <= 10) {
                            service.submit(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        lgConnectService.sendTouchClick(device);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                        break;
                    }

                    case MotionEvent.ACTION_CANCEL: {
                        mActivePointerId = 0;
                        break;
                    }

                    case MotionEvent.ACTION_POINTER_UP: {

                        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
                        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);

                        if (pointerId == mActivePointerId) {
                            // This was our active pointer going up. Choose a new
                            // active pointer and adjust accordingly.
                            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                            mLastTouchX = MotionEventCompat.getX(ev, newPointerIndex);
                            mLastTouchY = MotionEventCompat.getY(ev, newPointerIndex);
                            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
                        }
                        break;
                    }
                }
                return false;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        service = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onStop() {
        super.onStop();
        service.shutdown();
    }

    private void sendTouchMove(final Point point) {
        service.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    lgConnectService.sendTouchMove(device, point);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void sendKeyCommand(final LGKey command) {
        service.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    lgConnectService.sendKeyInput(device, command);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
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
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
