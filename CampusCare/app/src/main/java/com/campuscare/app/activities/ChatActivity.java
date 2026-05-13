package com.campuscare.app.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.campuscare.app.R;
import com.campuscare.app.adapters.MessageAdapter;
import com.campuscare.app.models.ChatMessage;
import com.campuscare.app.models.Message;
import com.campuscare.app.models.Ticket;
import com.campuscare.app.utils.ApiClient;
import com.campuscare.app.utils.DataManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    public static final String CHANNEL_TICKET = "TICKET";
    private static final long  POLL_MS = 5_000;

    private String ticketId;
    private Long   numericTicketId;
    private Long   myNumericId;

    private List<Message>  messages = new ArrayList<>();
    private MessageAdapter adapter;
    private RecyclerView   rv;
    private EditText       etMessage;
    private long           lastSeenId = 0;

    private final Handler  pollHandler  = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = () -> { fetchMessages(); schedulePoll(); };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ticketId = getIntent().getStringExtra("ticket_id");
        if (ticketId != null) {
            try { numericTicketId = Long.parseLong(ticketId); }
            catch (NumberFormatException ignored) { numericTicketId = null; }
        }

        if (DataManager.getInstance().getCurrentUser() != null) {
            String rawId = DataManager.getInstance().getCurrentUser().id;
            if (rawId != null) {
                try {
                    myNumericId = Long.parseLong(rawId.replaceAll("^[^0-9]+", ""));
                } catch (NumberFormatException ignored) {}
            }
        }

        Ticket ticket = findTicket(ticketId);
        String myRole = DataManager.getInstance().getCurrentUser() != null
                ? DataManager.getInstance().getCurrentUser().role : "";

        if ("requester".equalsIgnoreCase(myRole)
                || "student".equalsIgnoreCase(myRole)
                || "lecturer".equalsIgnoreCase(myRole)
                || "teacher".equalsIgnoreCase(myRole)) {
            boolean assigned = ticket != null
                    && (ticket.assignedTechnicianId != null || ticket.assignedTechId != null);
            if (!assigned) {
                Toast.makeText(this,
                        "Chat is available once a technician is assigned to your request.",
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        initUI(ticket);
    }

    private void initUI(Ticket ticket) {

        TextView tvName = findViewById(R.id.tv_name);
        if (tvName != null) {
            if (ticket != null) {
                String subject = ticket.category != null ? ticket.category + " Issue" : "Issue";
                if (ticket.title != null && !ticket.title.isEmpty()) subject = ticket.title;
                tvName.setText("Case #" + ticket.getShortId() + " — " + subject);
            } else {
                tvName.setText("Ticket Chat");
            }
        }

        TextView tvBadge = findViewById(R.id.tv_channel_badge);
        if (tvBadge != null) {
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText("🔔 Group Chat  —  Requester · Tech · Admin");
        }

        rv = findViewById(R.id.rv_messages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter(messages);
        rv.setAdapter(adapter);

        etMessage = findViewById(R.id.et_message);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());
        fetchMessages();
    }

    private void fetchMessages() {
        String token = ApiClient.getAuthToken();
        if (token == null || numericTicketId == null) return;

        ApiClient.getService().getChatMessages(token, numericTicketId, CHANNEL_TICKET)
                .enqueue(new Callback<List<ChatMessage>>() {
                    @Override
                    public void onResponse(Call<List<ChatMessage>> c,
                                           Response<List<ChatMessage>> r) {
                        if (!r.isSuccessful() || r.body() == null || r.body().isEmpty()) return;

                        long maxId = r.body().stream()
                                .mapToLong(m -> m.id != null ? m.id : 0).max().orElse(0);
                        if (maxId <= lastSeenId) return;
                        lastSeenId = maxId;

                        List<Message> rebuilt = new ArrayList<>();
                        for (ChatMessage cm : r.body()) {

                            boolean mine = cm.senderId != null
                                    && myNumericId != null
                                    && cm.senderId.equals(myNumericId);

                            String text = mine
                                    ? cm.body
                                    : "[" + (cm.senderName != null ? cm.senderName : "?") + "]\n" + cm.body;
                            rebuilt.add(new Message(text, mine));
                        }

                        runOnUiThread(() -> {
                            messages.clear();
                            messages.addAll(rebuilt);
                            adapter.notifyDataSetChanged();
                            if (!messages.isEmpty()) rv.scrollToPosition(messages.size() - 1);
                        });
                    }
                    @Override public void onFailure(Call<List<ChatMessage>> c, Throwable t) {}
                });
    }

    private void sendMessage() {
        String text = etMessage != null ? etMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        messages.add(new Message(text, true));
        adapter.notifyItemInserted(messages.size() - 1);
        rv.scrollToPosition(messages.size() - 1);
        if (etMessage != null) etMessage.setText("");

        String token = ApiClient.getAuthToken();
        if (token == null || numericTicketId == null) {
            Toast.makeText(this, "Offline – message saved locally.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("body", text);
        ApiClient.getService().sendChatMessage(token, numericTicketId, CHANNEL_TICKET, body)
                .enqueue(new Callback<ChatMessage>() {
                    @Override public void onResponse(Call<ChatMessage> c, Response<ChatMessage> r) {
                        if (r.isSuccessful() && r.body() != null && r.body().id != null)
                            lastSeenId = r.body().id;
                    }
                    @Override public void onFailure(Call<ChatMessage> c, Throwable t) {
                        runOnUiThread(() -> Toast.makeText(ChatActivity.this,
                                "Send failed.", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void schedulePoll() {
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.postDelayed(pollRunnable, POLL_MS);
    }

    @Override protected void onResume()  { super.onResume();  schedulePoll(); }
    @Override protected void onPause()   { super.onPause();   pollHandler.removeCallbacks(pollRunnable); }
    @Override protected void onDestroy() { super.onDestroy(); pollHandler.removeCallbacks(pollRunnable); }

    private Ticket findTicket(String id) {
        if (id == null) return null;
        for (Ticket t : DataManager.getInstance().getAllTickets())
            if (t.id != null && t.id.equals(id)) return t;
        return null;
    }
}