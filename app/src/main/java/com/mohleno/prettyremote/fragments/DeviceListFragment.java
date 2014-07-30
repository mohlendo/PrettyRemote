package com.mohleno.prettyremote.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mohleno.prettyremote.R;
import com.mohleno.prettyremote.RemoteActivity;
import com.mohleno.prettyremote.services.Device;
import com.mohleno.prettyremote.services.DeviceStorageService;
import com.mohleno.prettyremote.services.LGConnectService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by moh on 29.07.14.
 */
public class DeviceListFragment extends Fragment {
    private static final String TAG = DeviceListFragment.class.getSimpleName();


    private RecyclerView recyclerView;
    private List<Device> devices = new ArrayList<Device>();

    private DeviceStorageService deviceStorageService;
    private LGConnectService lgConnectService;
    private AsyncTask authRequestTask, pairingTask;

    public static DeviceListFragment newInstance() {
        DeviceListFragment fragment = new DeviceListFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        deviceStorageService = DeviceStorageService.getInstance(getActivity());
        lgConnectService = LGConnectService.getInstance(getActivity());

        recyclerView = (RecyclerView) getView().findViewById(R.id.rv_device_list);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);

        recyclerView.setAdapter(new DeviceListAdapter(devices, LayoutInflater.from(getActivity()), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int itemPosition = recyclerView.getChildPosition(view);
                connectToDevice(devices.get(itemPosition));
            }
        }));    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PairingKeyDialogFragment.PAIRING_KEY_RESULT) {
            String pairingKey = data.getStringExtra(PairingKeyDialogFragment.PAIRING_KEY_EXTRA);
            Device device = (Device) data.getSerializableExtra(PairingKeyDialogFragment.DEVICE_EXTRA);
            pairWithDevice(device, pairingKey);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem menuItem = menu.add(R.string.action_demo);
        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                openRemote(new Device("1.2.3.4"));
                return true;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    public void updateDevices(List<Device> updatedDevices) {
        devices.clear();
        devices.addAll(updatedDevices);
        if (recyclerView != null) {
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    /**
     * Connects to the given device. Either requests pairing key or, if key is present, just connects
     * @param device the device to connect with
     */
    private void connectToDevice(Device device) {
        if (TextUtils.isEmpty(device.getPairingKey())) {
            requestPairingKey(device);
        } else {
            pairWithDevice(device, device.getPairingKey());
        }
    }

    private boolean isRequestingPairingKey() {
        return authRequestTask != null && authRequestTask.getStatus() != AsyncTask.Status.FINISHED && !authRequestTask.isCancelled();
    }

    private void requestPairingKey(final Device device) {
        if (isRequestingPairingKey()) {
            return;
        }

        authRequestTask = new AsyncTask<Device, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Device... devices) {
                try {
                    return lgConnectService.requestPairingKey(devices[0]);
                } catch (IOException e) {
                    Log.e(TAG, "Error", e);
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if(result) {
                    openPairingKeyDialog(device);
                } else {
                    Toast.makeText(getActivity(), R.string.toast_cannot_request_pairing_key, Toast.LENGTH_SHORT).show();
                }
            }
        }.execute(device);
    }

    private boolean isPairingWithDevice() {
        return pairingTask != null && pairingTask.getStatus() != AsyncTask.Status.FINISHED && !pairingTask.isCancelled();
    }

    private void pairWithDevice(final Device device, final String pairingKey) {
        if (isPairingWithDevice()) {
            return;
        }
        pairingTask = new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... keys) {
                try {
                    return lgConnectService.pair(device, keys[0]);
                } catch (Exception e) {
                    Log.e(TAG, "Error pairing:", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(String session) {
                if (session != null) {
                    device.setPairingKey(pairingKey);
                    deviceStorageService.update(device);
                    openRemote(device);
                } else {
                    Toast.makeText(getActivity(), R.string.toast_cannot_connect, Toast.LENGTH_LONG).show();
                }
            }
        }.execute(pairingKey);
    }

    /**
     * Open the remote control to the given device
     * @param device the device to open
     */
    private void openRemote(Device device) {
        Intent intent = new Intent();
        intent.putExtra(RemoteActivity.DEVICE_INTENT_KEY, device);
        intent.setClass(getActivity(), RemoteActivity.class);
        startActivity(intent);
    }

    /**
     * Open the pairing dialog for the given device
     * @param device the device to pair with
     */
    private void openPairingKeyDialog(Device device) {
        PairingKeyDialogFragment fragment = PairingKeyDialogFragment.newInstance(device);
        fragment.setTargetFragment(this, PairingKeyDialogFragment.PAIRING_KEY_RESULT);
        fragment.show(getFragmentManager(), "PAIRING_KEY_DIALOG");
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
}
