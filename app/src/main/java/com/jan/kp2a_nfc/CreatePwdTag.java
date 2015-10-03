package com.jan.kp2a_nfc;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.tozny.crypto.android.AesCbcWithIntegrity;

import java.io.IOException;

public class CreatePwdTag extends AppCompatActivity {

    AesCbcWithIntegrity.SecretKeys keys;
    byte[] keyData;  WriteNfcDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_pwd_tag);

        RadioButton kp2a = (RadioButton) findViewById(R.id.kp2a);
        RadioButton kp2a_nonet = (RadioButton) findViewById(R.id.kp2a_nonet);
        boolean standard = isInstalled(getString(R.string.kp2a));
        boolean nonet = isInstalled(getString(R.string.kp2a_nonet));
        kp2a.setEnabled(standard);
        kp2a_nonet.setEnabled(nonet);
        if (standard) {
            kp2a.setChecked(true);
        } else if (nonet) {
            kp2a_nonet.setChecked(true);
        } else {
            Toast.makeText(getApplicationContext(), "WARNING: No Keepass App found", Toast.LENGTH_LONG).show();
        }


    }

    private boolean isInstalled(String pkgName) {
        PackageManager pm = getApplicationContext().getPackageManager();
        try {
            pm.getPackageInfo(pkgName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void openFile(View v) {
        Intent intent = new Intent();
        intent.setType("file/kdbx");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 42);
    }

    public void showHide(View v) {
        ToggleButton tb = (ToggleButton) v;
        EditText edit = (EditText) findViewById(R.id.editPwd);
        if (tb.isChecked()) {
            edit.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            String path = data.getData().getPath();
            ((EditText) findViewById(R.id.editDbPath)).setText(path);
        } catch (Exception e) {
              Toast.makeText(getApplicationContext(), getString(R.string.empty_path), Toast.LENGTH_SHORT).show();
        }

    }

    public void startWrite(View v) throws Exception {

        CheckBox chk = (CheckBox) findViewById(R.id.UnlockCheckBox);
        EditText path = (EditText) findViewById(R.id.editDbPath);
        EditText pwd = (EditText) findViewById(R.id.editPwd);
        RadioGroup radio = (RadioGroup) findViewById(R.id.radio);
        String pwdString = pwd.getText().toString();

        keys = AesCbcWithIntegrity.generateKey();
        keyData = keys.getConfidentialityKey().getEncoded();
        byte[] keySha = keys.getIntegrityKey().getEncoded();
        AesCbcWithIntegrity.CipherTextIvMac cipherText = AesCbcWithIntegrity.encrypt(pwdString, keys);
        String pwdEncrypted = cipherText.toString();


        SharedPreferences sharedPref = getSharedPreferences("main", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("instant", chk.isChecked());
        editor.putString("path", path.getText().toString());

        radio.getCheckedRadioButtonId();
        switch (radio.getCheckedRadioButtonId()) {
            case R.id.kp2a:
                editor.putString("package", getString(R.string.kp2a));
                break;
            case R.id.kp2a_nonet:
                editor.putString("package", getString(R.string.kp2a_nonet));
                break;
        }

        editor.putString("keySha", Base64.encodeToString(keySha, Base64.NO_WRAP));
        editor.putString("pwdEncrypted", pwdEncrypted);
        editor.commit();


        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        Log.d("NFC", "Foreground enabled!");


        dialog = new WriteNfcDialog(); //TODO Dismiss
        dialog.text = "Waiting for Tag...";
        dialog.show(getSupportFragmentManager(), "42");

    }

    private void writeNfc(NdefRecord record, Tag tag) {

        NdefMessage  message = new NdefMessage(record);
        Ndef ndef = Ndef.get(tag);

        try {
            ndef.connect();
            ndef.writeNdefMessage(message);
            ndef.close();

            dialog.progressDialog.setIndeterminate(false);
            dialog.progressDialog.setProgress(100);
            dialog.progressDialog.setMessage("Successfully written!");
        } catch (IOException | FormatException e) {
            dialog.progressDialog.setIndeterminate(false);
            dialog.progressDialog.setMessage("Failed, please try again");
        }

    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d("NFC", "Intent!");
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        NdefRecord mimeRecord = NdefRecord.createMime("application/com.jan.kp2a_nfc", keyData);
        writeNfc(mimeRecord, tag);
    }

}
