package com.campuscare.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.models.Message;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {
    private List<Message> messages;
    public MessageAdapter(List<Message> messages) { this.messages = messages; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Message m = messages.get(pos);
        if (m.isSent) {
            h.llReceived.setVisibility(View.GONE);
            h.llSent.setVisibility(View.VISIBLE);
            h.tvSent.setText(m.text);
        } else {
            h.llSent.setVisibility(View.GONE);
            h.llReceived.setVisibility(View.VISIBLE);
            h.tvReceived.setText(m.text);
        }
    }

    @Override public int getItemCount() { return messages.size(); }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout llReceived, llSent;
        TextView tvReceived, tvSent;
        VH(View v) {
            super(v);
            llReceived = v.findViewById(R.id.ll_received);
            llSent = v.findViewById(R.id.ll_sent);
            tvReceived = v.findViewById(R.id.tv_received);
            tvSent = v.findViewById(R.id.tv_sent);
        }
    }
}
