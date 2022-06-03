package com.example.securedmessagingapp.Adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.securedmessagingapp.Activities.ChatActivity;
import com.example.securedmessagingapp.Models.User;
import com.example.securedmessagingapp.R;
import com.example.securedmessagingapp.databinding.RowConversationBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UsersViewHolder>{

    Context context;
    ArrayList<User> users;
    String decryptedMsg;
    public UsersAdapter(Context context,ArrayList<User> users){
        this.context=context;
        this.users=users;
    }


    @NonNull
    @Override
    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.row_conversation,parent,false);
        return new UsersViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {
        User user = users.get(position);
        String senderId = FirebaseAuth.getInstance().getUid();
        String senderRoom = senderId + user.getUid();
        String receiverRoom = user.getUid() + senderId;
        String mainKey = senderRoom+receiverRoom;
        FirebaseDatabase.getInstance().getReference()
                .child("chats")
                .child(senderRoom)

                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String lastMsg = snapshot.child("lastMsg").getValue(String.class);
                            Long time = snapshot.child("lastMsgTime").getValue(Long.class);
                            if (time == null){
                                try {
                                    decryptedMsg = decrypt(lastMsg, mainKey);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                holder.binding.lastMsg.setText(decryptedMsg);
                                holder.binding.msgTime.setText(" ");
                            }
                            else {
                                Date date = new Date(time);
                                DateFormat formatter = new SimpleDateFormat("hh:mm aa");
                                formatter.setTimeZone(TimeZone.getTimeZone("GMT+5:45"));
                                String dateFormatted = formatter.format(date);
                                try {
                                    decryptedMsg = decrypt(lastMsg, mainKey);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                holder.binding.lastMsg.setText(decryptedMsg);
                                holder.binding.msgTime.setText(dateFormatted);
                            }

                        }else {
                            holder.binding.lastMsg.setText("Tap to chat");
                            holder.binding.msgTime.setText(" ");

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        holder.binding.username.setText(user.getName());

        Glide.with(context).load(user.getProfileImage())
                .placeholder(R.drawable.user)
                .into(holder.binding.profile);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("name",user.getName());
                intent.putExtra("uid",user.getUid());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public class UsersViewHolder extends RecyclerView.ViewHolder{

        RowConversationBinding binding;

        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = RowConversationBinding.bind(itemView);
        }
    }
    public String SECRET_KEY = "aesEncryptionKey";
    public String decrypt(String data, String password_text) throws Exception {
        String decryptedvalue = "";
        try {
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "AES");
            // Log.d("NIKHIL", "encrypt key:" + key.toString());
            Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedvalue = Base64.decode(data, Base64.DEFAULT);//pehle vo base64 me encoded tha, to decode to karna padega na
            byte[] decvalue = c.doFinal(decodedvalue);//final decoding operation
            decryptedvalue = new String(decvalue, "UTF-8");
        }catch (Exception e){
            e.printStackTrace();
        }
        //converting bytes into string
        return decryptedvalue;
    }
}
