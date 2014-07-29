package com.mohleno.prettyremote.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mohleno.prettyremote.R;

/**
 * Created by moh on 29.07.14.
 */
public class NoDevicesFragment extends Fragment {

    public static NoDevicesFragment newInstance() {
        NoDevicesFragment fragment = new NoDevicesFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_no_devices, container, false);

        view.findViewById(R.id.button_wifi_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });

        return view;
    }
}
