package com.mohleno.prettyremote;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mohleno.prettyremote.fragments.PairingKeyDialogFragment;
import com.mohleno.prettyremote.services.Device;
import com.mohleno.prettyremote.services.DeviceStorageService;
import com.mohleno.prettyremote.services.LGConnectService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class DevicesActivity extends Activity {
    private static final String TAG = DevicesActivity.class.getSimpleName();

    private RecyclerView recyclerView;
    private View noDeviceFoundView;
    private List<Device> devices = new ArrayList<Device>();

    private DeviceStorageService deviceStorageService;
    private LGConnectService lgConnectService;
    private AsyncTask deviceScanTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);
        setTitle(R.string.device_activity_label);

        deviceStorageService = DeviceStorageService.getInstance(this);
        lgConnectService = new LGConnectService();

        noDeviceFoundView = findViewById(R.id.vg_no_devices_found);
        noDeviceFoundView.setVisibility(View.GONE);
        findViewById(R.id.button_wifi_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.rv_device_list);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);

        recyclerView.setAdapter(new DeviceListAdapter(devices, LayoutInflater.from(this), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int itemPosition = recyclerView.getChildPosition(view);
                openPairingKeyDialog(devices.get(itemPosition));
            }
        }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        devices.addAll(deviceStorageService.load());
        if (!devices.isEmpty()) {
            recyclerView.getAdapter().notifyDataSetChanged();
        } else {
            scanForDevices();
        }
    }

    private static class DeviceListAdapter extends RecyclerView.Adapter {
        private final List<Device> devices;
        private final LayoutInflater inflater;
        private final View.OnClickListener onClickListener;

        private DeviceListAdapter(List<Device> devices, LayoutInflater inflater, View.OnClickListener onClickListener) {
            this.devices = devices;
            this.inflater = inflater;
            this.onClickListener = onClickListener;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = inflater.inflate(R.layout.device_list_item, viewGroup, false);
            view.setOnClickListener(onClickListener);
            Device device = devices.get(i);
            DeviceViewHolder viewHolder = new DeviceViewHolder(view);
            viewHolder.setDevice(device);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
            Device device = devices.get(i);
            ((DeviceViewHolder) viewHolder).setDevice(device);
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }
    }

    private static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceIp;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            deviceName = (TextView) itemView.findViewById(R.id.tv_device_name);
            deviceIp = (TextView) itemView.findViewById(R.id.tv_device_ip);
        }

        public void setDevice(Device device) {
            deviceName.setText(device.getName());
            deviceIp.setText(device.getIP());
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

    private void openPairingKeyDialog(Device device) {
        PairingKeyDialogFragment fragment = PairingKeyDialogFragment.newInstance(device);
        fragment.show(getFragmentManager(), "PAIRING_KEY_DIALOG");
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
                scannedDevices = new ArrayList<Device>();
                scannedDevices.add(new Device("1.2.3.2", "Glotze 1"));
                scannedDevices.add(new Device("1.2.3.3", "Glotze 2"));
                scannedDevices.add(new Device("1.2.3.4", "Glotze 3"));

                // merge the scanned devices with the current list
                for (Device scannedDevice : scannedDevices) {
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
                Collections.sort(devices, new Comparator<Device>() {
                    @Override
                    public int compare(Device device1, Device device2) {
                        return device1.getName().compareTo(device2.getName());
                    }
                });

                // inform the adapter
                recyclerView.getAdapter().notifyDataSetChanged();

                // and finally save it
                deviceStorageService.save(devices);

                // show/hide error view, depending on the state
                if (devices.isEmpty()) {
                    noDeviceFoundView.setVisibility(View.VISIBLE);
                } else {
                    noDeviceFoundView.setVisibility(View.GONE);
                }
            }
        }.execute();
    }
}
