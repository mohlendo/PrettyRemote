package com.mohleno.prettyremote.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.mohleno.prettyremote.R;
import com.mohleno.prettyremote.services.Device;
import com.mohleno.prettyremote.services.DeviceStorageService;
import com.mohleno.prettyremote.services.LGConnectService;

/**
 * Created by moh on 25.07.14.
 */
public class PairingKeyDialogFragment extends DialogFragment {
    private static final String TAG = PairingKeyDialogFragment.class.getSimpleName();
    private EditText pairingKeyEditText;
    private Device device;
    private LGConnectService lgConnectService;
    private DeviceStorageService deviceStorageService;
    private OnPairingKeyEnteredListener listener;
    private AsyncTask pairingTask;

    public interface OnPairingKeyEnteredListener {
        void onPairingKeyEntered(Device device, String pairingKey);
    }

    public static PairingKeyDialogFragment newInstance(Device device) {
        PairingKeyDialogFragment fragment = new PairingKeyDialogFragment();
        fragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog);
        Bundle bundle = new Bundle();
        bundle.putSerializable("device", device);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        device = (Device) getArguments().getSerializable("device");
        lgConnectService = LGConnectService.getInstance(getActivity());
        deviceStorageService = DeviceStorageService.getInstance(getActivity());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (OnPairingKeyEnteredListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPairingKeyEnteredListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(R.string.title_pairing_dialog);

        View v = inflater.inflate(R.layout.fragment_dialog_pairing_key, container, false);
        pairingKeyEditText = (EditText) v.findViewById(R.id.et_pairing_number);

        v.findViewById(R.id.button_pair).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String pairingKey = pairingKeyEditText.getText().toString();
                listener.onPairingKeyEntered(device, pairingKey);
                dismiss();
            }
        });
        v.findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        return v;
    }
}
