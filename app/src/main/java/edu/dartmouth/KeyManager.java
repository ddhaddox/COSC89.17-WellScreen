// File: edu/dartmouth/KeyManager.java
package edu.dartmouth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.security.SecureRandom;

import edu.dartmouth.SecurePreferences;

public class KeyManager {
    private static final String TAG = "KeyManager";
    private static final String PREFS_NAME = "encrypted_prefs";
    private static final String KEY_NAME = "database_key";

    public static void generateKey(Context context) {
        try {
            SharedPreferences prefs = SecurePreferences.getEncryptedSharedPreferences(context, PREFS_NAME);
            if (!prefs.contains(KEY_NAME)) {
                // Generate random key
                byte[] key = new byte[64]; // 512 bits
                SecureRandom random = new SecureRandom();
                random.nextBytes(key);
                // Convert key to Base64 string
                String keyString = Base64.encodeToString(key, Base64.DEFAULT);
                // Store in EncryptedSharedPreferences
                prefs.edit().putString(KEY_NAME, keyString).apply();
                Log.d(TAG, "Encryption key generated and stored.");
            } else {
                Log.d(TAG, "Encryption key already exists.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating key: ", e);
        }
    }

    public static byte[] getKey(Context context) {
        try {
            SharedPreferences prefs = SecurePreferences.getEncryptedSharedPreferences(context, PREFS_NAME);
            String keyString = prefs.getString(KEY_NAME, null);
            if (keyString != null) {
                Log.d(TAG, "Encryption key retrieved successfully.");
                return Base64.decode(keyString, Base64.DEFAULT);
            } else {
                Log.e(TAG, "Encryption key not found.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving key: ", e);
        }
        return null;
    }
}
