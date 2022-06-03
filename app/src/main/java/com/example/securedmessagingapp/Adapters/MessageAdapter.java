package com.example.securedmessagingapp.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securedmessagingapp.Models.Message;
import com.example.securedmessagingapp.R;
import com.example.securedmessagingapp.databinding.DeleteDialogBinding;
import com.example.securedmessagingapp.databinding.ItemReceivedBinding;
import com.example.securedmessagingapp.databinding.ItemSentBinding;
import com.github.pgreze.reactions.ReactionPopup;
import com.github.pgreze.reactions.ReactionsConfig;
import com.github.pgreze.reactions.ReactionsConfigBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class MessageAdapter extends RecyclerView.Adapter{

    final int ITEM_SENT = 1;
    final int ITEM_RECEIVED = 2;

    String senderRoom;
    String receiverRoom;
    String key;
    Context context;
    ArrayList<Message> messages;

    public MessageAdapter(Context context, ArrayList<Message> messages, String senderRoom, String receiverRoom ,String key){
        this.context = context;
        this.messages = messages;
        this.senderRoom = senderRoom;
        this.receiverRoom = receiverRoom;
        this.key = key;

    }

    public String messageTxt;
    public String decryptedMessage;
    String mainKey=key;
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType==ITEM_SENT){
            View view = LayoutInflater.from(context).inflate(R.layout.item_sent,parent,false);
            return new SentViewHolder(view);
        }else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_received,parent,false);
            return new ReceiverViewHolder(view);
        }

    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if(FirebaseAuth.getInstance().getUid().equals(message.getSenderId())){
            return ITEM_SENT;
        }else {
            return ITEM_RECEIVED;
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        int reaction[]=new int[]{
                R.drawable.like,
                R.drawable.heart,
                R.drawable.laughing,
                R.drawable.wow,
                R.drawable.sad,
                R.drawable.angry
        };

        ReactionsConfig config = new ReactionsConfigBuilder(context)
                .withReactions(reaction)
                .build();

        ReactionPopup popup = new ReactionPopup(context, config, (pos) -> {
            if(pos<0)
                return false;
            if (holder.getClass()== SentViewHolder.class ){
                SentViewHolder viewHolder = (SentViewHolder)holder;
                viewHolder.binding.feeling.setImageResource(reaction[pos]);
               // viewHolder.binding.feeling.setVisibility(View.VISIBLE);
                new CountDownTimer(10000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        SentViewHolder viewHolder = (SentViewHolder)holder;
                        viewHolder.binding.mtimeout.setText("disappearing in:" +  (millisUntilFinished/1000));
                        viewHolder.binding.mtimeout.setVisibility(View.VISIBLE);
                        //here you can have your logic to set text to edittext
                    }
                    public void onFinish() {
                        SentViewHolder viewHolder = (SentViewHolder)holder;
                        viewHolder.binding.mtimeout.setVisibility(View.INVISIBLE);
                    }
                }.start();
            }
            else{
                ReceiverViewHolder viewHolder = (ReceiverViewHolder) holder;
                viewHolder.binding.feeling.setImageResource(reaction[pos]);
                //viewHolder.binding.feeling.setVisibility(View.VISIBLE);
                new CountDownTimer(10000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        SentViewHolder viewHolder = (SentViewHolder)holder;
                        viewHolder.binding.mtimeout.setText("disappearing in:" +  (millisUntilFinished/1000));
                        viewHolder.binding.mtimeout.setVisibility(View.VISIBLE);
                        //here you can have your logic to set text to edittext
                    }
                    public void onFinish() {
                        SentViewHolder viewHolder = (SentViewHolder)holder;
                        viewHolder.binding.mtimeout.setVisibility(View.INVISIBLE);
                    }
                }.start();
            }

            message.setFeeling(pos);

            FirebaseDatabase.getInstance().getReference()
                    .child("chats")
                    .child(senderRoom)
                    .child("messages")
                    .child(message.getMessageId()).setValue(message);

            FirebaseDatabase.getInstance().getReference()
                    .child("chats")
                    .child(receiverRoom)
                    .child("messages")
                    .child(message.getMessageId()).setValue(message);



            return true; // true is closing popup, false is requesting a new selection
        });

        if(holder.getClass()== SentViewHolder.class ){
            messageTxt = message.getMessage();
            SentViewHolder viewHolder = (SentViewHolder)holder;
            try {
                decryptedMessage = decrypt(messageTxt,mainKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
            viewHolder.binding.message.setText(decryptedMessage);

            if(message.getFeeling()>=0){
                viewHolder.binding.feeling.setImageResource(reaction[(int)message.getFeeling()]);
                //viewHolder.binding.feeling.setVisibility(View.VISIBLE);
                new CountDownTimer(20000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        SentViewHolder viewHolder = (SentViewHolder)holder;
                        viewHolder.binding.mtimeout.setText("disappearing in:" +  (millisUntilFinished/1000));
                        viewHolder.binding.mtimeout.setVisibility(View.VISIBLE);
                        //here you can have your logic to set text to edittext
                    }
                    public void onFinish() {
                        SentViewHolder viewHolder = (SentViewHolder)holder;
                        viewHolder.binding.mtimeout.setVisibility(View.INVISIBLE);
                    }
                }.start();
            }
            else{
                viewHolder.binding.feeling.setVisibility(View.GONE);
            }

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    messageTxt = message.getMessage();
                    try {
                        decryptedMessage = decrypt(messageTxt,mainKey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (URLUtil.isValidUrl(decryptedMessage)){

                        String uri = String.format(decryptedMessage);
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        context.startActivity(intent);
                    }
                }
            });
            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    View view = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null);
                    DeleteDialogBinding binding = DeleteDialogBinding.bind(view);
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Delete Message")
                            .setView(binding.getRoot())
                            .create();



                    binding.delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(null);
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(receiverRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(null);
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("lastMsg")
                                    .setValue(null);
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(receiverRoom)
                                    .child("lastMsg")
                                    .setValue(null);
                            dialog.dismiss();

                        }
                    });

                    binding.cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();

                    return false;

                }
            });
        }else {
            messageTxt = message.getMessage();
            ReceiverViewHolder viewHolder = (ReceiverViewHolder) holder;
            try {
                decryptedMessage = decrypt(messageTxt,mainKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
            viewHolder.binding.message.setText(decryptedMessage);

            if(message.getFeeling()>=0){
                viewHolder.binding.feeling.setImageResource(reaction[(int)message.getFeeling()]);
                //viewHolder.binding.feeling.setVisibility(View.VISIBLE);
                new CountDownTimer(20000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        viewHolder.binding.mtimeout.setText("disappearing in:" +  (millisUntilFinished/1000));
                        viewHolder.binding.mtimeout.setVisibility(View.VISIBLE);
                        //here you can have your logic to set text to edittext
                    }
                    public void onFinish() {
                        viewHolder.binding.mtimeout.setVisibility(View.INVISIBLE);
                    }
                }.start();

                viewHolder.binding.mtimeout.setVisibility(View.VISIBLE);

            }
            else{
                viewHolder.binding.feeling.setVisibility(View.GONE);
            }
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    messageTxt = message.getMessage();
                    try {
                        decryptedMessage = decrypt(messageTxt,mainKey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (URLUtil.isValidUrl(decryptedMessage)){

                        String uri = String.format(decryptedMessage);
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        context.startActivity(intent);
                    }
                }
            });
            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    View view = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null);
                    DeleteDialogBinding binding = DeleteDialogBinding.bind(view);
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Delete Message")
                            .setView(binding.getRoot())
                            .create();



                    binding.delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(receiverRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(null);
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(null);
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("lastMsg")
                                    .setValue(null);
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(receiverRoom)
                                    .child("lastMsg")
                                    .setValue(null);
                            dialog.dismiss();
                        }
                    });

                    binding.cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();

                    return false;
                }
            });

        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class SentViewHolder extends RecyclerView.ViewHolder {


        ItemSentBinding binding;
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            binding=ItemSentBinding.bind(itemView);
        }
    }
    public class ReceiverViewHolder extends RecyclerView.ViewHolder{

        ItemReceivedBinding binding;
        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            binding=ItemReceivedBinding.bind(itemView);
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
