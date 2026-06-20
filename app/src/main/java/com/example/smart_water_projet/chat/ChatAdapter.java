package com.example.smart_water_projet.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_water_projet.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_USER = ChatMessage.TYPE_USER;
    public static final int TYPE_BOT  = ChatMessage.TYPE_BOT;

    private final List<ChatMessage> items;

    public ChatAdapter(List<ChatMessage> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View v = inflater.inflate(R.layout.item_chat_row_user, parent, false);
            return new BubbleHolder(v);
        }
        View v = inflater.inflate(R.layout.item_chat_row_bot, parent, false);
        return new BubbleHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BubbleHolder h = (BubbleHolder) holder;
        h.tv.setText(items.get(position).text);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BubbleHolder extends RecyclerView.ViewHolder {
        final TextView tv;

        BubbleHolder(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvChatBubble);
        }
    }
}
