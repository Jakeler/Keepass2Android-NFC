package com.jan.kp2a_nfc;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;


public class WriteNfcDialog extends DialogFragment {

    String text;
    ProgressDialog progressDialog;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        progressDialog = new ProgressDialog(getActivity(), getTheme());
        //progressDialog.setTitle(title);
        progressDialog.setMessage(text);
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        return progressDialog;
    }

}
