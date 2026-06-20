package com.example.smart_water_projet.chat;

/**
 * Message affiché dans le fil de discussion.
 */
public final class ChatMessage {

    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT  = 1;

    public final int    type;
    public final String text;

    public ChatMessage(int type, String text) {
        this.type = type;
        this.text = text != null ? text : "";
    }
}
