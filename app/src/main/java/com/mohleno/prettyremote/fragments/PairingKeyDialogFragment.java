package com.mohleno.prettyremote.fragments;

import android.app.DialogFragment;
import android.content.Intent;
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
    public static final int PAIRING_KEY_RESULT = 42;
    public static final String PAIRING_KEY_EXTRA = "pairing_key";
    public static final String DEVICE_EXTRA = "device";
    private EditText pairingKeyEditText;
    private Device device;


    public static PairingKeyDialogFragment newInstance(Device device) {
        PairingKeyDialogFragment fragment = new PairingKeyDialogFragment();
        fragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogTheme);
        Bundle bundle = new Bundle();
        bundle.putSerializable("device", device);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        device = (Device) getArguments().getSerializable("device");
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
                Intent i = new Intent();
                i.putExtra(PAIRING_KEY_EXTRA, pairingKey);
                i.putExtra(DEVICE_EXTRA, device);
                getTargetFragment().onActivityResult(getTargetRequestCode(), PAIRING_KEY_RESULT, i);

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
