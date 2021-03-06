package com.mohleno.prettyremote.services;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by moh on 26.07.14.
 */
public class DeviceStorageService {
    protected static final String SHARED_PREF_KEY = "deviceListStorage";
    protected static final String DEVICE_LIST_KEY = "deviceList";
    private static DeviceStorageService instance;
    private final Context context;

    private DeviceStorageService(Context context) {
        this.context = context;
        context.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
    }

    public static DeviceStorageService getInstance(Context context) {
        DeviceStorageService r = instance;
        if (r == null) {
            synchronized (DeviceStorageService.class) { // while we were waiting for the lock, another
                r = instance; // thread may have instantiated instance
                if (r == null) {
                    r = new DeviceStorageService(context);
                    instance = r;
                }
            }
        }
        return r;
    }

    public void update(Device device) {
        List<Device> devices = load();
        int i = devices.indexOf(device);
        if (i != -1) {
            Device oldDevice = devices.get(i);
            oldDevice.setName(device.getName());
            oldDevice.setPairingKey(device.getPairingKey());
        }
        save(devices);
    }

    public void save(List<Device> deviceList) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE).edit();
        editor.putString(DEVICE_LIST_KEY, new Gson().toJson(deviceList));
        editor.apply();
    }

    public List<Device> merge(List<Device> newDevices) {
        List<Device> devices = load();
        // merge the scanned devices with the current list
        for (Device scannedDevice : newDevices) {
            int index = devices.indexOf(scannedDevice);
            if (index != -1) {
                // update the already existing device
                Device device = devices.get(index);
                device.setName(scannedDevice.getName());
            } else {
                // add the new device
                devices.add(scannedDevice);
            }
        }

        // re-sort it
        Collections.sort(devices, new DeviceComparator());

        // and finally save it
        save(devices);
        return devices;
    }

    public List<Device> load() {
        Type listType = new TypeToken<ArrayList<Device>>() {
        }.getType();
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
        if (sharedPreferences.contains(DEVICE_LIST_KEY)) {
            sharedPreferences.getString(DEVICE_LIST_KEY, "");
            return new Gson().fromJson(sharedPreferences.getString(DEVICE_LIST_KEY, ""), listType);
        } else {
            return new ArrayList<Device>();
        }
    }

    private static final class DeviceComparator implements Comparator<Device> {
        @Override
        public int compare(Device device1, Device device2) {
            return device1.getName().compareTo(device2.getName());
        }
    }
}
