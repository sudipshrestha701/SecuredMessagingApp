    package com.example.securedmessagingapp.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.securedmessagingapp.databinding.ActivityOtpactivityBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.mukesh.OnOtpCompletionListener;

import java.util.ArrayList;

    public class OTPActivity extends AppCompatActivity {

    ActivityOtpactivityBinding binding;
    FirebaseAuth auth;
        FirebaseStorage storage;
        FirebaseDatabase database;
    ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpactivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();
        getSupportActionBar().hide();
        String phoneNumber = getIntent().getStringExtra("phoneNumber");
        String backendotp = getIntent().getStringExtra("backendotp");
        binding.phoneLbl.setText("Verify "+ phoneNumber);

        binding.otpView.setOtpCompletionListener(new OnOtpCompletionListener() {
            @Override
            public void onOtpCompleted(String otp) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(backendotp,otp);
                auth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){

                            database = FirebaseDatabase.getInstance();
                            storage = FirebaseStorage.getInstance();
                            auth = FirebaseAuth.getInstance();
                            getSupportActionBar().hide();
                            String authUid = auth.getUid();
                            ArrayList databaseUid = new ArrayList();
                            database.getReference().child("users").child(authUid).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    databaseUid.clear();
                                    for (DataSnapshot snapshot1: snapshot.getChildren()){
                                        databaseUid.add(snapshot1.getValue());
                                        Boolean exist = databaseUid.contains(authUid);
                                        if (exist){
                                            Intent intent = new Intent(OTPActivity.this,MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                        else{
                                            Intent intent = new Intent(OTPActivity.this,SetupProfileActivity.class);
                                            intent.putExtra("phoneNumber", phoneNumber);
                                            startActivity(intent);
                                            finishAffinity();
                                        }
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                        }
                        else{
                            Toast.makeText(OTPActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });





    }
}