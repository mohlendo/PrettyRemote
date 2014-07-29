package com.mohleno.prettyremote.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mohleno.prettyremote.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeviceScanFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DeviceScanFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class DeviceScanFragment extends Fragment {

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DeviceScanFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DeviceScanFragment newInstance() {
        DeviceScanFragment fragment = new DeviceScanFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
    public DeviceScanFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_device_scan, container, false);
    }
}
