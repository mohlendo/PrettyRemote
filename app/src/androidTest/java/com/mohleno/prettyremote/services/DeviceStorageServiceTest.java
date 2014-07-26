package com.mohleno.prettyremote.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by moh on 26.07.14.
 */
public class DeviceStorageServiceTest extends AndroidTestCase {

    public void testSave() {
        DeviceStorageService instance = DeviceStorageService.getInstance(getContext());

        List<Device> devices = new ArrayList<Device>();
        devices.add(new Device("1.2.3.4", "Glotze 1"));
        devices.add(new Device("1.2.3.5", "Glotze 2"));
        devices.add(new Device("1.2.3.6", "Glotze 3"));

        instance.save(devices);

        SharedPreferences sharedPreferences = mContext.getSharedPreferences(DeviceStorageService.SHARED_PREF_KEY, Context.MODE_PRIVATE);
        assertNotNull(sharedPreferences.getString(DeviceStorageService.DEVICE_LIST_KEY, null));
    }

    public void testLoad() {
        DeviceStorageService instance = DeviceStorageService.getInstance(getContext());

        List<Device> devices = new ArrayList<Device>();
        devices.add(new Device("1.2.3.4", "Glotze 1"));
        devices.add(new Device("1.2.3.5", "Glotze 2"));
        devices.add(new Device("1.2.3.6", "Glotze 3"));

        instance.save(devices);

        List<Device> deviceList = instance.load();

        assertEquals(devices.size(), deviceList.size());
    }
}
