package com.example.securedmessagingapp.Activities;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.securedmessagingapp.Adapters.MessageAdapter;
import com.example.securedmessagingapp.Models.Message;
import com.example.securedmessagingapp.databinding.ActivityChatBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class ChatActivity extends AppCompatActivity {


    ActivityChatBinding binding;
    MessageAdapter adapter;
    ArrayList<Message> messages;
    String senderRoom,receiverRoom;
    FirebaseDatabase database;
    String secretkey;
    CheckBox checkBox;
    long feeling = -1;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        checkBox = binding.timeout;

        messages = new ArrayList<>();

        String name = getIntent().getStringExtra("name");
        String receiverUid = getIntent().getStringExtra("uid");
        String senderUid = FirebaseAuth.getInstance().getUid();
        binding.call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                database.getReference().child("users").child(receiverUid).child("phoneNumber").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String phone = snapshot.getValue(String.class);
                        System.err.println(phone);
                        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone));
                        startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
        senderRoom = senderUid+receiverUid;
        receiverRoom = receiverUid+senderUid;
        secretkey = senderRoom+receiverRoom;
        adapter = new MessageAdapter(this,messages,senderRoom,receiverRoom,secretkey);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        database=FirebaseDatabase.getInstance();
        ValueEventListener seenListener;
        database.getReference().child("chats")
                .child(senderRoom)
                .child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for (DataSnapshot snapshot1 : snapshot.getChildren()){
                            Message message = snapshot1.getValue(Message.class);
                            message.setMessageId(snapshot1.getKey());
                            messages.add(message);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String messageTxt = binding.messageBox.getText().toString();
                String encryptedMessage = null;
                try {
                    encryptedMessage = encrypt(messageTxt,secretkey);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Date date = new Date();
                TextView mTextField = binding.timer;

                if (checkBox.isChecked()){
                    feeling = 1;
                }
                if (!checkBox.isChecked()){
                    feeling = -1;
                }
                Message message = new Message(encryptedMessage,senderUid,date.getTime(),feeling);
                binding.messageBox.setText("");
                String randomKey = database.getReference().push().getKey();
                if(!checkBox.isChecked()){
                    HashMap<String, Object> lastMsgObj = new HashMap<>();
                    lastMsgObj.put("lastMsg",message.getMessage());
                    lastMsgObj.put("lastMsgTime", date.getTime());
                    database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                    database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                }
                else{
                    HashMap<String, Object> lastMsgObj = new HashMap<>();

                    lastMsgObj.put("lastMsgTime",null);
                    database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                    database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);
                }
                database.getReference().child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .child(randomKey)
                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        database.getReference().child("chats")
                                .child(receiverRoom)
                                .child("messages")
                                .child(randomKey)
                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {

                            }
                        });


                    }
                });
                if(checkBox.isChecked()){
                    new Handler().postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            //Write your code here
                            database.getReference().child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(randomKey).
                                    removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    database.getReference().child("chats")
                                            .child(receiverRoom)
                                            .child("messages")
                                            .child(randomKey).
                                            removeValue();
                                }
                            });
                        }
                    }, 10000); //Timer is in ms here.
                }
            }
        });


        binding.voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");
                try {
                    startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(), "Sorry! Your device doesn't support speech input",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(name);
        binding.location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(ChatActivity.this, "Sending Location Details", Toast.LENGTH_LONG).show();
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                if (ActivityCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, new CancellationToken() {
                    @Override
                    public boolean isCancellationRequested() {
                        return false;
                    }
                    @NonNull
                    @Override
                    public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                        return null;
                    }
                }).addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                                String Lmessage ="http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                            String encryptedMessage = null;
                            try {
                                encryptedMessage = encrypt(Lmessage,secretkey);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Date date = new Date();
                            TextView mTextField = binding.timer;

                            if (checkBox.isChecked()){
                                feeling = 1;
                            }
                            if (!checkBox.isChecked()){
                                feeling = -1;
                            }
                            Message message = new Message(encryptedMessage,senderUid,date.getTime(),feeling);
                            binding.messageBox.setText("");
                            String randomKey = database.getReference().push().getKey();
                            if(!checkBox.isChecked()){
                                HashMap<String, Object> lastMsgObj = new HashMap<>();
                                lastMsgObj.put("lastMsg",message.getMessage());
                                lastMsgObj.put("lastMsgTime", date.getTime());
                                database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                                database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                            }
                            else{
                                HashMap<String, Object> lastMsgObj = new HashMap<>();

                                lastMsgObj.put("lastMsgTime",null);
                                database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                                database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);
                            }
                            database.getReference().child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(randomKey)
                                    .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    database.getReference().child("chats")
                                            .child(receiverRoom)
                                            .child("messages")
                                            .child(randomKey)
                                            .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {

                                        }
                                    });


                                }
                            });
                            if(checkBox.isChecked()){
                                new Handler().postDelayed(new Runnable() {

                                    @Override
                                    public void run() {
                                        //Write your code here
                                        database.getReference().child("chats")
                                                .child(senderRoom)
                                                .child("messages")
                                                .child(randomKey).
                                                removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                database.getReference().child("chats")
                                                        .child(receiverRoom)
                                                        .child("messages")
                                                        .child(randomKey).
                                                        removeValue();
                                            }
                                        });
                                    }
                                }, 10000); //Timer is in ms here.
                            }
                        } else {
                            String Lmessage = "GPS was turned off.Couldn't find location";

                            String encryptedMessage = null;
                            try {
                                encryptedMessage = encrypt(Lmessage,secretkey);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Date date = new Date();
                            TextView mTextField = binding.timer;

                            if (checkBox.isChecked()){
                                feeling = 1;
                            }
                            if (!checkBox.isChecked()){
                                feeling = -1;
                            }
                            Message message = new Message(encryptedMessage,senderUid,date.getTime(),feeling);
                            binding.messageBox.setText("");
                            String randomKey = database.getReference().push().getKey();
                            if(!checkBox.isChecked()){
                                HashMap<String, Object> lastMsgObj = new HashMap<>();
                                lastMsgObj.put("lastMsg",message.getMessage());
                                lastMsgObj.put("lastMsgTime", date.getTime());
                                database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                                database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                            }
                            else{
                                HashMap<String, Object> lastMsgObj = new HashMap<>();

                                lastMsgObj.put("lastMsgTime",null);
                                database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                                database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);
                            }
                            database.getReference().child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(randomKey)
                                    .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    database.getReference().child("chats")
                                            .child(receiverRoom)
                                            .child("messages")
                                            .child(randomKey)
                                            .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {

                                        }
                                    });


                                }
                            });
                            if(checkBox.isChecked()){
                                new Handler().postDelayed(new Runnable() {

                                    @Override
                                    public void run() {
                                        //Write your code here
                                        database.getReference().child("chats")
                                                .child(senderRoom)
                                                .child("messages")
                                                .child(randomKey).
                                                removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                database.getReference().child("chats")
                                                        .child(receiverRoom)
                                                        .child("messages")
                                                        .child(randomKey).
                                                        removeValue();
                                            }
                                        });
                                    }
                                }, 10000); //Timer is in ms here.
                            }
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("Check: ", "OnFailure");
                        String Lmessage = "GPS was turned off.Couldn't find location";
                        String messageTxt = binding.messageBox.getText().toString();
                        String encryptedMessage = null;
                        try {
                            encryptedMessage = encrypt(messageTxt,secretkey);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }

                        Date date = new Date();
                        TextView mTextField = binding.timer;

                        if (checkBox.isChecked()){
                            feeling = 1;
                        }
                        if (!checkBox.isChecked()){
                            feeling = -1;
                        }
                        Message message = new Message(encryptedMessage,senderUid,date.getTime(),feeling);
                        binding.messageBox.setText("");
                        String randomKey = database.getReference().push().getKey();
                        if(!checkBox.isChecked()){
                            HashMap<String, Object> lastMsgObj = new HashMap<>();
                            lastMsgObj.put("lastMsg",message.getMessage());
                            lastMsgObj.put("lastMsgTime", date.getTime());
                            database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                            database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                        }
                        else{
                            HashMap<String, Object> lastMsgObj = new HashMap<>();

                            lastMsgObj.put("lastMsgTime",null);
                            database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                            database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);
                        }
                        database.getReference().child("chats")
                                .child(senderRoom)
                                .child("messages")
                                .child(randomKey)
                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                database.getReference().child("chats")
                                        .child(receiverRoom)
                                        .child("messages")
                                        .child(randomKey)
                                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {

                                    }
                                });


                            }
                        });
                        if(checkBox.isChecked()){
                            new Handler().postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    //Write your code here
                                    database.getReference().child("chats")
                                            .child(senderRoom)
                                            .child("messages")
                                            .child(randomKey).
                                            removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            database.getReference().child("chats")
                                                    .child(receiverRoom)
                                                    .child("messages")
                                                    .child(randomKey).
                                                    removeValue();
                                        }
                                    });
                                }
                            }, 20000); //Timer is in ms here.
                        }
                    }
                });


            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String speechText = binding.messageBox.getText().toString() + "\n" + result.get(0);
                    binding.messageBox.setText(speechText);
                }
                break;
            }
        }
    }

    public  String SECRET_KEY = "aesEncryptionKey";

    public String encrypt(String data, String password_text) throws Exception {
        String encryptedvalue="";
        try {

            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "AES");;
            //Log.d("NIKHIL", "encrypt key:" + key.toString());
            Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");//creating an object
            c.init(Cipher.ENCRYPT_MODE, key);//initialisation
            byte[] encVal = c.doFinal(data.getBytes("UTF-8"));
            //Encrypts or decrypts data in a single-part operation, or finishes a multiple-part operation
            encryptedvalue = Base64.encodeToString(encVal, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Base64-encode the given data and return a newly allocated String with the result.
        //It's basically a way of encoding arbitrary binary data in ASCII text. It takes 4 characters per 3 bytes of data,
        // plus potentially a bit of padding at the end.
        //Essentially each 6 bits of the input is encoded in a 64-character alphabet.
        //The "standard" alphabet uses A-Z, a-z, 0-9 and + and /, with = as a padding character. There are URL-safe variants.
        return encryptedvalue;

    }




}