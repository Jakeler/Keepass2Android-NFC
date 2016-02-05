package com.jan.kp2a_nfc;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.ProgressBar;

public class VerifyActivity extends AppCompatActivity {

    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;

    CheckBox found, db, pwd, keyFile, kpaApp;
    ProgressBar searching;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);

        found = (CheckBox) findViewById(R.id.checkBoxFound);
        db = (CheckBox) findViewById(R.id.checkBoxDB);
        pwd = (CheckBox) findViewById(R.id.checkBoxPwd);
        keyFile = (CheckBox) findViewById(R.id.checkBoxKf);
        kpaApp = (CheckBox) findViewById(R.id.checkBoxKPA);
        searching = (ProgressBar) findViewById(R.id.progressBarSearch);
        searching.setIndeterminate(true);

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Setup an intent filter for all MIME based dispatches
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        mFilters = new IntentFilter[] {
                ndef,
        };

        // Setup a tech list for all NfcF tags
        mTechLists = new String[][] { new String[] { NfcF.class.getName() } };
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
    }

    @Override
    protected void onNewIntent(Intent NfcIntent) {
        super.onNewIntent(NfcIntent);

        searching.setIndeterminate(false);
        searching.setProgress(100);

        found.setActivated(true);
        found.setChecked(true);

        SharedPreferences sharedPref = getSharedPreferences("main", Context.MODE_PRIVATE);
        NfcData nfcData = new NfcData();
        nfcData.loadSettings(sharedPref);

        Tag tag = NfcIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        found.setText(getString(R.string.tag_found) + tag.getId().toString());

        if (nfcData.dbPath != null) {
            db.setChecked(true);
            db.setText(getString(R.string.path) + nfcData.dbPath);
        } else {
            db.setChecked(false);
        }

        if (nfcData.packageName.equals(getString(R.string.kp2a))) {
            kpaApp.setChecked(true);
            kpaApp.setText(getString(R.string.kp2a_app) + "Standard");
        }
        if (nfcData.packageName.equals(getString(R.string.kp2a_nonet))) {
            kpaApp.setChecked(true);
            kpaApp.setText(getString(R.string.kp2a_app) + "Offline");
        }


        Ndef ndef = Ndef.get(tag);
        NdefMessage ndefMessage = ndef.getCachedNdefMessage();
        NdefRecord[] records = ndefMessage.getRecords();
        //pwd.setActivated(nfcData.decrypt(records[0].getPayload()));
    }
}
