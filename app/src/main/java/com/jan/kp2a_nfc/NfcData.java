package com.jan.kp2a_nfc;



import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefRecord;
import android.util.Base64;

import com.tozny.crypto.android.AesCbcWithIntegrity;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class NfcData {

    public String plainPwd, encryptedPwd;
    public String dbPath, keyFilePath;
    public String packageName;
    public boolean instantUnlock;
    private byte[] keyAes, keySha;
    private AesCbcWithIntegrity.SecretKeys keys;

    public void generateKeys() throws GeneralSecurityException{
        keys = AesCbcWithIntegrity.generateKey();
        keyAes = keys.getConfidentialityKey().getEncoded();
        keySha = keys.getIntegrityKey().getEncoded();
    }

    public void loadSettings(SharedPreferences sharedPreferences) {
        dbPath = sharedPreferences.getString("dbPath", null);
        keyFilePath = sharedPreferences.getString("keyFilePath", null);

        encryptedPwd = sharedPreferences.getString("encryptedPwd", null);
        keySha = Base64.decode(sharedPreferences.getString("keySha", null), Base64.NO_WRAP);

        packageName = sharedPreferences.getString("packageName", null);
        instantUnlock = sharedPreferences.getBoolean("instantUnlock", true);
    }

    public void saveSettings(SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("dbPath", dbPath);
        editor.putString("keyFilePath", keyFilePath);

        editor.putString("encryptedPwd", encryptedPwd);
        editor.putString("keySha", Base64.encodeToString(keySha, Base64.NO_WRAP));

        editor.putString("packageName", packageName);
        editor.putBoolean("instantUnlock", instantUnlock);
        editor.apply();
    }


    public void encrypt() throws Exception{
        encryptedPwd = AesCbcWithIntegrity.encrypt(plainPwd, keys).toString();
    }

    public boolean decrypt(byte[] key) {
        keyAes = key;
        try {
            SecretKey integ = new SecretKeySpec(keySha, "HmacSHA256");
            SecretKey confe = new SecretKeySpec(keyAes, "AES");
            AesCbcWithIntegrity.SecretKeys keys = new AesCbcWithIntegrity.SecretKeys(confe, integ);

            AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac = new AesCbcWithIntegrity.CipherTextIvMac(encryptedPwd);

            plainPwd = AesCbcWithIntegrity.decryptString(cipherTextIvMac, keys);
            return true;

        } catch (UnsupportedEncodingException | GeneralSecurityException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Intent getIntent() {
        Intent intent = new Intent();
        intent.setClassName(packageName, "md5f0702f468598c68ce18586502249fb40.PasswordActivity");
        intent.putExtra("fileName", dbPath);
        if (keyFilePath != null) {
            intent.putExtra("keyFile", keyFilePath);
        }

        intent.putExtra("password", plainPwd);
        intent.putExtra("launchImmediately", instantUnlock);

        return intent;
    }

    public NdefRecord getNdefRecord() {
        return NdefRecord.createMime("application/com.jan.kp2a_nfc", keyAes);
    }
}
