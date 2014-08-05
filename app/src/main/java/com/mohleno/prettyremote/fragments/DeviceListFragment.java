package com.mohleno.prettyremote.fragments;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
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
 * Fragment showing list of devices and the progress bar when scanning for new devices
 */
public class DeviceListFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Device>> {
    private static final String TAG = DeviceListFragment.class.getSimpleName();

    private RecyclerView recyclerView;
    private View progressView, emptyView;
    private DeviceListAdapter deviceListAdapter;

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

        View view = getView();
        if (view == null) {
            return;
        }

        progressView = view.findViewById(R.id.vg_progress_view);
        progressView.setVisibility(View.VISIBLE);

        emptyView = view.findViewById(R.id.vg_no_devices_found);
        emptyView.setVisibility(View.GONE);

        view.findViewById(R.id.button_wifi_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });

        recyclerView = (RecyclerView) view.findViewById(R.id.rv_device_list);
        recyclerView.setVisibility(View.GONE);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);

        deviceListAdapter = new DeviceListAdapter(devices, LayoutInflater.from(getActivity()), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int itemPosition = recyclerView.getChildPosition(view);
                connectToDevice(devices.get(itemPosition));
            }
        });
        recyclerView.setAdapter(deviceListAdapter);
        getLoaderManager().initLoader(0, null, this);
    }

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
        inflater.inflate(R.menu.devices, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
                getLoaderManager().getLoader(0).forceLoad();
                return true;
            case R.id.action_demo:
                openRemote(new Device("1.2.3.4"));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<List<Device>> onCreateLoader(int i, Bundle bundle) {
        return new DeviceScanLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<Device>> loader, List<Device> data) {
        devices.clear();
        devices.addAll(data);
        deviceListAdapter.notifyDataSetChanged();

        progressView.setVisibility(View.GONE);
        if (devices.isEmpty()) {
            getActivity().setTitle(R.string.title_no_devices_found);
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            getActivity().setTitle(R.string.title_connect_device);
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Device>> objectLoader) {
        devices.clear();
        deviceListAdapter.notifyDataSetChanged();
    }

    /**
     * Connects to the given device. Either requests pairing key or, if key is present, just connects
     *
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
                if (result) {
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
     *
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
     *
     * @param device the device to pair with
     */
    private void openPairingKeyDialog(Device device) {
        PairingKeyDialogFragment fragment = PairingKeyDialogFragment.newInstance(device);
        fragment.setTargetFragment(this, PairingKeyDialogFragment.PAIRING_KEY_RESULT);
        fragment.show(getFragmentManager(), "PAIRING_KEY_DIALOG");
    }

    /**
     * Device List Adapter
     */
    private final static class DeviceListAdapter extends RecyclerView.Adapter {
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

    /**
     * Device scan task loader
     */
    private static final class DeviceScanLoader extends AsyncTaskLoader<List<Device>> {
        private final LGConnectService connectService;
        private final DeviceStorageService storageService;

        public DeviceScanLoader(Context context) {
            super(context);
            this.storageService = DeviceStorageService.getInstance(context);
            this.connectService = LGConnectService.getInstance(context);
        }

        @Override
        public List<Device> loadInBackground() {
            try {
                return storageService.merge(connectService.scanForDevices());
            } catch (IOException e) {
                Log.e(TAG, "Error scanning for devices", e);
            }
            return storageService.load();
        }
    }

}
