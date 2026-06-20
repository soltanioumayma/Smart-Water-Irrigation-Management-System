package com.example.smart_water_projet.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smart_water_projet.R;
import com.example.smart_water_projet.chat.ChatAdapter;
import com.example.smart_water_projet.chat.ChatMessage;
import com.example.smart_water_projet.chat.FarmerAssistantEngine;
import com.example.smart_water_projet.chat.GeminiChatClient;
import com.example.smart_water_projet.databinding.FragmentFarmerAssistantBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Call;

public class FarmerAssistantFragment extends Fragment {

    private FragmentFarmerAssistantBinding binding;
    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private ExecutorService chatExecutor;
    private final AtomicReference<Call> inFlightCall = new AtomicReference<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFarmerAssistantBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatExecutor = Executors.newSingleThreadExecutor();

        adapter = new ChatAdapter(messages);
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        binding.rvAssistantChat.setLayoutManager(lm);
        binding.rvAssistantChat.setAdapter(adapter);

        if (messages.isEmpty()) {
            messages.add(new ChatMessage(ChatAdapter.TYPE_BOT,
                    getString(R.string.assistant_welcome_bot)));
            adapter.notifyItemInserted(0);
        }

        binding.btnAssistantSend.setOnClickListener(v -> sendQuestion());

        binding.etAssistantQuestion.setOnEditorActionListener((TextView tv, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendQuestion();
                return true;
            }
            return false;
        });
    }

    private void sendQuestion() {
        if (binding == null) return;

        CharSequence cs = binding.etAssistantQuestion.getText();
        String q = cs != null ? cs.toString().trim() : "";
        if (q.isEmpty()) return;

        binding.etAssistantQuestion.setText("");
        hideKeyboard();

        messages.add(new ChatMessage(ChatAdapter.TYPE_USER, q));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToEnd();

        String apiKey = GeminiChatClient.getConfiguredApiKey();
        if (apiKey.isEmpty()) {
            String offline = FarmerAssistantEngine.answer(q);
            messages.add(new ChatMessage(ChatAdapter.TYPE_BOT,
                    getString(R.string.assistant_gemini_missing_key) + "\n\n" + offline));
            adapter.notifyItemInserted(messages.size() - 1);
            scrollToEnd();
            return;
        }

        setLoadingUi(true);

        final List<ChatMessage> snapshot = new ArrayList<>(messages);
        final String emptyReply = getString(R.string.assistant_gemini_empty_reply);
        final String errFmt = getString(R.string.assistant_gemini_error);
        final String fallbackPrefix = getString(R.string.assistant_fallback_prefix);

        chatExecutor.execute(() -> {
            String reply;
            try {
                reply = GeminiChatClient.generateReply(snapshot, apiKey, inFlightCall);
                if (reply == null || reply.trim().isEmpty()) {
                    reply = emptyReply;
                }
            } catch (Exception e) {
                String fallback = FarmerAssistantEngine.answer(q);
                reply = String.format(errFmt, e.getMessage())
                        + "\n\n" + fallbackPrefix + fallback;
            }

            String finalReply = reply;
            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                setLoadingUi(false);
                messages.add(new ChatMessage(ChatAdapter.TYPE_BOT, finalReply));
                adapter.notifyItemInserted(messages.size() - 1);
                scrollToEnd();
            });
        });
    }

    private void setLoadingUi(boolean loading) {
        if (binding == null) return;
        binding.btnAssistantSend.setEnabled(!loading);
        binding.etAssistantQuestion.setEnabled(!loading);
    }

    private void scrollToEnd() {
        if (binding == null) return;
        int last = messages.size() - 1;
        if (last >= 0) {
            binding.rvAssistantChat.scrollToPosition(last);
        }
    }

    private void hideKeyboard() {
        if (binding == null) return;
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.etAssistantQuestion.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroyView() {
        Call c = inFlightCall.getAndSet(null);
        if (c != null) {
            c.cancel();
        }
        if (chatExecutor != null) {
            chatExecutor.shutdownNow();
            chatExecutor = null;
        }
        binding = null;
        super.onDestroyView();
    }
}
