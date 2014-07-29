package com.mohleno.prettyremote;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.mohleno.prettyremote.fragments.DeviceListFragment;
import com.mohleno.prettyremote.fragments.DeviceScanFragment;
import com.mohleno.prettyremote.fragments.NoDevicesFragment;
import com.mohleno.prettyremote.services.Device;
import com.mohleno.prettyremote.services.DeviceStorageService;
import com.mohleno.prettyremote.services.LGConnectService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class DevicesActivity extends Activity {
    private static final String TAG = DevicesActivity.class.getSimpleName();

    private DeviceStorageService deviceStorageService;
    private LGConnectService lgConnectService;
    private AsyncTask deviceScanTask;
    private List<Device> devices = new ArrayList<Device>();

    private DeviceListFragment deviceListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);
        setTitle(R.string.title_connect_device);

        deviceStorageService = DeviceStorageService.getInstance(this);
        lgConnectService = LGConnectService.getInstance(this);

        showDeviceList();

        devices.clear();
        devices.addAll(deviceStorageService.load());
        if (!devices.isEmpty()) {
            deviceListFragment.updateDevices(devices);
        } else {
            scanForDevices();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (deviceScanTask != null) {
            deviceScanTask.cancel(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.devices, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            scanForDevices();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeviceList() {
        if (deviceListFragment == null) {
            deviceListFragment = DeviceListFragment.newInstance();
        }
        if (deviceListFragment.isVisible()) {
            return;
        }

        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, deviceListFragment);
        // Commit the transaction
        transaction.commit();
    }

    private void showNoDevicesError() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, NoDevicesFragment.newInstance());
        transaction.commit();
    }

    private void showLoadingIndicator() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, DeviceScanFragment.newInstance());
        transaction.commit();
    }

    private boolean isScanningForDevices() {
        return deviceScanTask != null && deviceScanTask.getStatus() != AsyncTask.Status.FINISHED && !deviceScanTask.isCancelled();
    }

    /**
     * Scan for new devices
     */
    private void scanForDevices() {
        // ignore if the task is already running
        if (isScanningForDevices()) {
            return;
        }
        if (devices.isEmpty()) {
            showLoadingIndicator();
        }
        deviceScanTask = new AsyncTask<Void, Void, List<Device>>() {
            @Override
            protected List<Device> doInBackground(Void... voids) {
                try {
                    return lgConnectService.scanForDevices();

                } catch (IOException e) {
                    Log.e(TAG, "Error scanning for devices", e);
                }
                return Collections.emptyList();
            }

            @Override
            protected void onPostExecute(List<Device> scannedDevices) {
                //TODO: just for offline Tests:
                /*scannedDevices = new ArrayList<Device>();
                scannedDevices.add(new Device("1.2.3.2", "Glotze 1"));
                scannedDevices.add(new Device("1.2.3.3", "Glotze 2"));
                scannedDevices.add(new Device("1.2.3.4", "Glotze 3"));*/

                devices = deviceStorageService.merge(scannedDevices);

                // show/hide error view, depending on the state
                if (devices.isEmpty()) {
                    showNoDevicesError();
                    setTitle(R.string.title_no_devices_found);
                } else {
                    showDeviceList();
                    // inform the fragment
                    deviceListFragment.updateDevices(devices);
                    setTitle(R.string.title_connect_device);
                }
            }
        }.execute();
    }
}
