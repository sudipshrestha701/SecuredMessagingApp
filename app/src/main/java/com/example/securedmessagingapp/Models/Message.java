package com.example.securedmessagingapp.Models;

public class Message {
    private String messageId, message, senderId;
    private long timestamp;
    private long feeling;

    public Message(String message, String senderId, long timestamp,long feeling) {
        this.message = message;
        this.feeling = feeling;
        this.senderId = senderId;
        this.timestamp = timestamp;
    }



    public Message() {
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getFeeling() {
        return feeling;
    }

    public void setFeeling(long feeling) {
        this.feeling = feeling;
    }
}
