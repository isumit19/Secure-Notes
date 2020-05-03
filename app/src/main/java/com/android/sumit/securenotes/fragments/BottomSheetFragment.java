package com.android.sumit.securenotes.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.sumit.securenotes.R;
import com.android.sumit.securenotes.activity.MainActivity;
import com.android.sumit.securenotes.activity.NoteListAcitivity;
import com.android.sumit.securenotes.fingerprint.FingerprintUiHelper;


public class BottomSheetFragment extends BottomSheetDialogFragment implements FingerprintUiHelper.Callback {




    private FingerprintUiHelper mFingerprintUiHelper;
    private Activity activity;

    public BottomSheetFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_bottom_sheet, container, false);
        v.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.onFailed();
                dismiss();
            }
        });
        v.findViewById(R.id.cancelIcon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.onFailed();
                dismiss();
            }
        });

        mFingerprintUiHelper = new FingerprintUiHelper(activity.getSystemService(FingerprintManager.class),
                (ImageView) v.findViewById(R.id.fingerprintIcon),
                (TextView) v.findViewById(R.id.fingerprint_details), this);
        return v;
    }


    @Override
    public void onResume() {
        super.onResume();
        mFingerprintUiHelper.startListening(null);

    }

    @Override
    public void onPause() {

        mFingerprintUiHelper.stopListening();
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = getActivity();

    }

    @Override
    public void onAuthenticated() {
        Intent it = new Intent(activity, NoteListAcitivity.class);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(it);
    }

    @Override
    public void onError(final int errId) {
        if(errId == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT || errId == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT){
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.onFailed();
            dismiss();
        }


    }
}