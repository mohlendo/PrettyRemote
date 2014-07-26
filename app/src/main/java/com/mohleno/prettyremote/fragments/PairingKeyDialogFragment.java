package com.mohleno.prettyremote.fragments;

import android.app.ActivityOptions;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.mohleno.prettyremote.R;
import com.mohleno.prettyremote.RemoteActivity;
import com.mohleno.prettyremote.services.Device;

/**
 * Created by moh on 25.07.14.
 */
public class PairingKeyDialogFragment extends DialogFragment {

    private EditText pairingKeyEditText;
    private Device device;

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
                dismiss();
                Intent intent = new Intent();
                intent.setClass(getActivity(), RemoteActivity.class);
                startActivity(intent);
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
