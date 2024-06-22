package com.email.mailclient.model;

public class EmailMessage {
    private int id;
    private String created_at;
    private String sender;
    private String senderName;
    private String receiver;
    private String receiverName;

    private int reply;
    private String owner;
    private int isRead;
    private String attachment;
    private String subject;
    private String content;
    private String deleted_at;

    public EmailMessage() {
        id = -1;
        created_at = "";
        sender = "";
        senderName = "";
        receiver = "";
        receiverName = "";
        reply = -1;
        owner = "";
        isRead = 0;
        attachment = "";
        subject = "";
        content = "";
        deleted_at = "";
    }

    public EmailMessage(int id, String created_at, String sender, String receiver, int reply, String owner, int isRead, String attachment, String subject, String content, String deleted_at) {
        this.id = id;
        this.created_at = created_at;
        this.sender = sender;
        this.receiver = receiver;
        this.reply = reply;
        this.owner = owner;
        this.isRead = isRead;
        this.attachment = attachment;
        this.subject = subject;
        this.content = content;
        this.deleted_at = deleted_at;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }


    public int getReply() {
        return reply;
    }

    public void setReply(int reply) {
        this.reply = reply;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getIsRead() {
        return isRead;
    }

    public void setIsRead(int isRead) {
        this.isRead = isRead;
    }

    public Boolean isRead() {
        if (isRead == 0) {
            return false;
        } else {
            return true;
        }
    }


    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDeleted_at() {
        return deleted_at;
    }

    public void setDeleted_at(String deleted_at) {
        this.deleted_at = deleted_at;
    }
}
