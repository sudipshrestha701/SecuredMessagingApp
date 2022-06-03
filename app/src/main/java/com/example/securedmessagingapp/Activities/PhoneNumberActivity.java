package com.example.securedmessagingapp.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.securedmessagingapp.R;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneNumberActivity extends AppCompatActivity {

    EditText enterednumber;
    Button getOtp;
    FirebaseAuth auth;
    ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number);
        enterednumber = findViewById(R.id.phoneBox);
        getOtp = findViewById(R.id.continueBtn);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            Intent intent = new Intent(PhoneNumberActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }else {

            getSupportActionBar().hide();
            getOtp.setOnClickListener(view -> {
                if (!enterednumber.getText().toString().trim().isEmpty()) {

                    if ((enterednumber.getText().toString().trim()).length() == 10) {
                        dialog = new ProgressDialog(PhoneNumberActivity.this);
                        dialog.setMessage("Sending OTP...");
                        dialog.setCancelable(false);
                        dialog.show();
                        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                                "+977" + enterednumber.getText().toString(),
                                100,
                                TimeUnit.SECONDS,
                                PhoneNumberActivity.this,
                                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                    @Override
                                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

                                    }

                                    @Override
                                    public void onVerificationFailed(@NonNull FirebaseException e) {
                                        dialog.dismiss();
                                        Toast.makeText(PhoneNumberActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onCodeSent(@NonNull String backendotp, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {

                                        dialog.dismiss();
                                        Intent intent = new Intent(PhoneNumberActivity.this, OTPActivity.class);
                                        intent.putExtra("phoneNumber", enterednumber.getText().toString());
                                        intent.putExtra("backendotp", backendotp);

                                        startActivity(intent);
                                    }
                                }
                        );
                    } else {
                        Toast.makeText(this, "Please enter a correct phone number", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(this, "Please enter a mobile number", Toast.LENGTH_SHORT).show();
                }
            });
        }

    }
}