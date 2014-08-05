package com.mohleno.prettyremote;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.mohleno.prettyremote.fragments.DeviceListFragment;
import com.mohleno.prettyremote.services.DeviceStorageService;
import com.mohleno.prettyremote.services.LGConnectService;


public class DevicesActivity extends Activity {
    private static final String TAG = DevicesActivity.class.getSimpleName();

    private DeviceStorageService deviceStorageService;
    private LGConnectService lgConnectService;
    private DeviceListFragment deviceListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);
        setTitle(R.string.title_connect_device);

        deviceStorageService = DeviceStorageService.getInstance(this);
        lgConnectService = LGConnectService.getInstance(this);

        showDeviceList();
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
}
