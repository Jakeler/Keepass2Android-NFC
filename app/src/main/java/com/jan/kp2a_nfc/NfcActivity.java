package com.jan.kp2a_nfc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import com.tozny.crypto.android.AesCbcWithIntegrity;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class NfcActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = getSharedPreferences("main", Context.MODE_PRIVATE);

        Intent intent = new Intent();
        intent.setClassName(sharedPref.getString("package", null), "md5f0702f468598c68ce18586502249fb40.PasswordActivity");
        intent.putExtra("fileName", sharedPref.getString("path", null));
        //intent.putExtra("keyFile", "");

        Intent NfcIntent = getIntent();
        Tag tag = NfcIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Ndef ndef = Ndef.get(tag);
        NdefMessage ndefMessage = ndef.getCachedNdefMessage();
        NdefRecord[] records = ndefMessage.getRecords();
        String plainPwd = decrypt(records[0].getPayload(), sharedPref.getString("keySha", null), sharedPref.getString("pwdEncrypted", null));

        intent.putExtra("password", plainPwd);
        intent.putExtra("launchImmediately", sharedPref.getBoolean("instant", false));

        startActivity(intent);
        finish();
    }


    private String decrypt(byte[] key, String shaText, String cipherText) {

        try {
            byte[] sha = Base64.decode(shaText, Base64.NO_WRAP);
            SecretKey integ = new SecretKeySpec(sha, "HmacSHA256");
            SecretKey confe = new SecretKeySpec(key, "AES");
            AesCbcWithIntegrity.SecretKeys keys = new AesCbcWithIntegrity.SecretKeys(confe, integ);

            AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac = new AesCbcWithIntegrity.CipherTextIvMac(cipherText);

            String plain = AesCbcWithIntegrity.decryptString(cipherTextIvMac, keys);
            Toast.makeText(getApplicationContext(), "KP2A NFC: Sucessfully decrypted password", Toast.LENGTH_SHORT).show();
            return plain;

        } catch (UnsupportedEncodingException | GeneralSecurityException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "KP2A NFC: Failed to decrypt password", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}