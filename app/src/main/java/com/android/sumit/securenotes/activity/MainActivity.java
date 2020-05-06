package com.android.sumit.securenotes.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.TextView;


import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.sumit.securenotes.R;
import com.android.sumit.securenotes.fragments.BottomSheetFragment;
import com.android.sumit.securenotes.utils.Decrypt;
import com.android.sumit.securenotes.utils.Encrypt;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity {


    FingerprintManager fingerprintManager;
    Handler handler;
    Runnable runnable1, runnable2;
    SharedPreferences sharedPreferences;
    KeyguardManager keyguardManager;

    private TextInputEditText pwd;
    private TextInputEditText repwd;
    private TextInputLayout pwdInputLayout;
    private TextInputLayout repwdInputLayout;
    private CheckBox checkBox;


    KeyGenerator keyGenerator;

    InputMethodManager inputMethodManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
        sharedPreferences = getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.shared_preference),
                Context.MODE_PRIVATE);
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        LayoutHandler();

    }

    public void onFailed(){
        pwd.requestFocus();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
            }
        },500);
    }

    boolean isHardwareDetected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return fingerprintManager.isHardwareDetected();
        } else {
            FingerprintManagerCompat fingerprintManagerCompat = FingerprintManagerCompat.from(this);
            return fingerprintManagerCompat.isHardwareDetected();
        }
    }

    private boolean isPatternSet() {
        ContentResolver cr = getContentResolver();
        try {
            int lockPatternEnable = Settings.Secure.getInt(cr, Settings.Secure.LOCK_PATTERN_ENABLED);
            return lockPatternEnable == 1;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private boolean isPassOrPinSet() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
    }

    boolean isKeyguardSecure() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return keyguardManager.isDeviceSecure();
        } else {
            return isPassOrPinSet() || isPatternSet();
        }

    }

    boolean hasEnrolledFingerprints() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return fingerprintManager.hasEnrolledFingerprints();
        } else {
            FingerprintManagerCompat fingerprintManagerCompat = FingerprintManagerCompat.from(this);
            return fingerprintManagerCompat.hasEnrolledFingerprints();
        }
    }

    private void LayoutHandler() {

        handler = new Handler();

        runnable1 = new Runnable() {
            @Override
            public void run() {
                if (!isHardwareDetected()) {
                    sharedPreferences.edit().putBoolean(getString(R.string.fingerprint), false).apply();
                    handler.removeCallbacks(runnable1);

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            final Runnable run = this;
                            if (!isKeyguardSecure()) {
                                new MaterialDialog.Builder(MainActivity.this)
                                        .title("Device is not secure!!")
                                        .content("Please enable lockscreen security in your device's Settings")
                                        .positiveText("Setup")
                                        .negativeText("Exit")
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                                                runnable2 = run;
                                                startActivityForResult(intent, 1);
                                            }
                                        })
                                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                //h2.removeCallbacks(r2);
                                                handler.removeCallbacks(run);
                                                finish();
                                                System.exit(0);
                                            }
                                        })
                                        .cancelable(false)
                                        .show();
                            } else {
                                handler.removeCallbacks(run);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        InflateLayout();
                                    }
                                });
                            }
                        }
                    }, 0);

                } else {
                    sharedPreferences.edit().putBoolean(getString(R.string.fingerprint), true).apply();
                    handler.removeCallbacks(runnable1);

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            final Runnable r = this;
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                                //Log.i("fingerprint","permission missing");
                                new MaterialDialog.Builder(MainActivity.this)
                                        .title("Fingerprint permission missing!!")
                                        .content("This app needs fingerprint permission to work properly and securely")
                                        .positiveText("Settings")
                                        .negativeText("Exit")
                                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                handler.removeCallbacks(r);
                                                finish();
                                                System.exit(0);
                                            }
                                        })
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                Intent it = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                                runnable2 = r;
                                                it.setData(uri);
                                                startActivity(it);

                                            }
                                        })
                                        .cancelable(false)
                                        .show();
                            } else {
                                if (!hasEnrolledFingerprints()) {
                                    new MaterialDialog.Builder(MainActivity.this)
                                            .title("Fingerprint data not found!!")
                                            .content("Please enroll atleast one fingerprint")
                                            .positiveText("Settings")
                                            .negativeText("Exit")
                                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                                @Override
                                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                    handler.removeCallbacks(r);
                                                    finish();
                                                    System.exit(0);
                                                }
                                            })
                                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                @Override
                                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                    Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                                                    runnable2 = r;
                                                    startActivityForResult(intent, 2);

                                                }
                                            })
                                            .cancelable(false)
                                            .show();
                                } else {
                                    handler.removeCallbacks(r);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            InflateLayout();
                                        }
                                    });

                                }

                            }
                        }
                    }, 0);
                }
            }
        };
        handler.postDelayed(runnable1, 0);


    }

    public void Encryption(String s) {
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyGenerator.init(256);
        SecretKey secretKey = keyGenerator.generateKey();
        byte[] secreKeyEnc = secretKey.getEncoded();

        SecureRandom secureRandom = new SecureRandom();
        byte[] IV = new byte[16];
        secureRandom.nextBytes(IV);

        try {
            byte[] cipherText = Encrypt.encrypt(s.trim().getBytes(), secretKey, IV);
            String cipher = Encode(cipherText);
            String striv = Encode(IV);
            String strSecretKey = Encode(secreKeyEnc);
            sharedPreferences.edit().putString("cipher", cipher).putString("iv", striv).putString("secretkey", strSecretKey).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String Encode(byte[] decval) {
        String conVal= Base64.encodeToString(decval,Base64.DEFAULT);
        return conVal;
    }


    private boolean doubleCheck(){

        if(TextUtils.isEmpty(pwd.getText())){
            pwdInputLayout.setError(getString(R.string.password_empty));
            if(TextUtils.isEmpty(repwd.getText())){
                repwdInputLayout.setError(getString(R.string.password_empty));
            }
            return false;
        }
        else if(TextUtils.isEmpty(repwd.getText())){
            repwdInputLayout.setError(getString(R.string.password_empty));
            return false;
        }

        if(TextUtils.equals(pwd.getText(),repwd.getText()))
            return true;

        else {
            repwdInputLayout.setError(getString(R.string.password_no_match));
            return false;
        }
    }

    public void clickF(View v) {
        SharedPreferences shp = getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.shared_preference),
                Context.MODE_PRIVATE);



        if (shp.getBoolean(getString(R.string.first_run), true)) {       //handle user first time registration
            //Log.i("first_run", "true");
            if (doubleCheck()) {
                shp.edit().putBoolean(getString(R.string.first_run), false).apply();
                if (checkBox.isChecked())
                    shp.edit().putBoolean(getString(R.string.use_fingerprint_future), true).apply();
                else
                    shp.edit().putBoolean(getString(R.string.use_fingerprint_future), false).apply();

                if(shp.getBoolean(getString(R.string.fingerprint),true) && shp.getBoolean(getString(R.string.use_fingerprint_future),true)){
                }
                Encryption(pwd.getText().toString());

                Intent it = new Intent(MainActivity.this,NoteListAcitivity.class);
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(it);
            }
        } else {
            //handle user login

            if(shp.getBoolean(getString(R.string.fingerprint),true) && shp.getBoolean(getString(R.string.use_fingerprint_future),true)){
            }
            System.out.println("Checking");
            checkPassword();


        }

    }
    public static byte[] Decode(String enval) {
        byte[] conVal = Base64.decode(enval,Base64.DEFAULT);
        return conVal;

    }

    private void checkPassword(){
        if (!TextUtils.isEmpty(pwd.getText())) {


            String encryptedpass = sharedPreferences.getString("cipher",null);
            String iv = sharedPreferences.getString("iv",null);
            String secretkey = sharedPreferences.getString("secretkey",null);

            byte[] pass_arr = Decode(encryptedpass);
            byte[] iv_arr = Decode(iv);
            byte[] secret_arr = Decode(secretkey);
            SecretKey secretKey = new SecretKeySpec(secret_arr,0,secret_arr.length,"AES");

            String decryptedText = Decrypt.decrypt(pass_arr,secretKey,iv_arr);


            if (pwd.getText().toString().equals(decryptedText)) {
                pwd.setText("");
                Intent it = new Intent(MainActivity.this, NoteListAcitivity.class);
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(it);
            } else {

                pwd.setText("");
                pwdInputLayout.setError(getString(R.string.password_wrong));
            }


        } else {
            pwdInputLayout.setError(getString(R.string.password_empty));
        }
    }




    private void InflateLayout() {

        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (sharedPreferences.getBoolean(getApplicationContext().getString(R.string.first_run), true)) {
            setContentView(R.layout.register_page);
            pwdInputLayout = findViewById(R.id.pwdInputLayout);
            repwdInputLayout = findViewById(R.id.repwdInputLayout);
            pwd = findViewById(R.id.password);
            repwd = findViewById(R.id.second_password);
            checkBox = findViewById(R.id.checkBox);
            if (!sharedPreferences.getBoolean(getString(R.string.fingerprint), false)) {
                checkBox.setVisibility(View.GONE);
            }

            pwd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (i == EditorInfo.IME_ACTION_GO) {
                        if (TextUtils.isEmpty(pwd.getText())) {
                            pwdInputLayout.setError(getString(R.string.password_empty));
                        } else {
                            pwdInputLayout.setError(null);
                            repwd.requestFocus();
                        }
                        return true;
                    }
                    return false;
                }
            });

            repwd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (i == EditorInfo.IME_ACTION_GO) {
                        if (TextUtils.isEmpty(repwd.getText())) {
                            repwdInputLayout.setError(getString(R.string.password_empty));
                        } else {

                            findViewById(R.id.button).setEnabled(false);
                            clickF(textView.getRootView());
                        }
                        return true;
                    }
                    return false;
                }
            });

            pwd.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    pwdInputLayout.setError(null);
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }


            });

            repwd.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    repwdInputLayout.setError(null);
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });

            pwd.requestFocus();
            pwd.postDelayed(new Runnable() {
                @Override
                public void run() {
                    inputMethodManager.showSoftInput(pwd, 0);

                }
            }, 500);

        } else {
            setContentView(R.layout.activity_login);
            pwdInputLayout = findViewById(R.id.pwdInputLayout);
            pwd = findViewById(R.id.pwd);



            if (!sharedPreferences.getBoolean(getString(R.string.fingerprint), true))
                findViewById(R.id.fpToggle).setVisibility(View.GONE);

            if (!sharedPreferences.getBoolean(getString(R.string.use_fingerprint_future), true)) {
                findViewById(R.id.fpToggle).setVisibility(View.GONE);
                pwd.requestFocus();
                pwd.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        inputMethodManager.showSoftInput(pwd, 0);
                    }
                }, 500);

            } else {


                pwd.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BottomSheetFragment bottomSheetFragment = new BottomSheetFragment();
                        bottomSheetFragment.show(getSupportFragmentManager(), bottomSheetFragment.getTag());
                    }
                }, 200);
            }


            findViewById(R.id.fpToggle).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final BottomSheetFragment bottomSheetFragment = new BottomSheetFragment();
                    bottomSheetFragment.show(getSupportFragmentManager(), bottomSheetFragment.getTag());

                }
            });

            pwd.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    pwdInputLayout.setError(null);
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });


        }
    }
}
