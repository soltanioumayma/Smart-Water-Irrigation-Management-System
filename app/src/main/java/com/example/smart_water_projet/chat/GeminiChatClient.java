package com.example.smart_water_projet.chat;

import com.example.smart_water_projet.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Client HTTP pour Hugging Face Router API (compatible OpenAI).
 * La clé API doit être dans {@code local.properties} : {@code HF_TOKEN=...}
 * (ne jamais committer la clé dans le dépôt).
 */
public final class GeminiChatClient {

    private static final MediaType JSON =
            MediaType.parse("application/json; charset=utf-8");

    /** Modèle Hugging Face - Llama 3 8B */
    private static final String MODEL = "meta-llama/Meta-Llama-3-8B-Instruct:novita";

    private static final String SYSTEM_PROMPT =
            "Tu es l'assistant vocal/texte de l'application mobile « Aqua Smart » pour agriculteurs. "
                    + "Tu réponds en français, de façon claire et pratique. "
                    + "Tu aides sur : qualité de l'eau (pH, conductivité, température), irrigation, pompe, "
                    + "débit/volume, alertes, météo agricole, bonnes pratiques. "
                    + "Si une question sort complètement du cadre agricole / eau / app, réponds poliment en une phrase "
                    + "puis propose de revenir sur l'irrigation ou la qualité de l'eau. "
                    + "Ne révèle jamais de clé API ni d'informations confidentielles.";

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private GeminiChatClient() {}

    public static String getConfiguredApiKey() {
        return BuildConfig.HF_TOKEN != null ? BuildConfig.HF_TOKEN.trim() : "";
    }

    /**
     * @param callHolder si non null, y place le {@link Call} en cours pour pouvoir l’annuler (ex. {@code onDestroyView})
     */
    public static String generateReply(List<ChatMessage> transcript, String apiKey,
                                       AtomicReference<Call> callHolder) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("Clé API absente : ajoutez HF_TOKEN dans local.properties puis reconstruisez.");
        }

        JSONArray messages = buildMessages(transcript);

        JSONObject body = new JSONObject();
        try {
            body.put("model", MODEL);
            body.put("messages", messages);
            body.put("temperature", 0.65);
            body.put("max_tokens", 1024);
        } catch (org.json.JSONException e) {
            throw new IOException("Erreur de construction JSON : " + e.getMessage(), e);
        }

        HttpUrl url = Objects.requireNonNull(HttpUrl.parse("https://router.huggingface.co/v1/chat/completions"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(JSON, body.toString()))
                .build();

        Call call = CLIENT.newCall(request);
        if (callHolder != null) {
            callHolder.set(call);
        }

        try (Response response = call.execute()) {
            ResponseBody rb = response.body();
            String respStr = rb != null ? rb.string() : "";

            if (!response.isSuccessful()) {
                String errMsg = parseApiError(respStr);
                throw new IOException("HTTP " + response.code() + (errMsg.isEmpty() ? "" : " — " + errMsg));
            }

            return parseModelText(respStr);
        } finally {
            if (callHolder != null) {
                callHolder.set(null);
            }
        }
    }

    private static JSONArray buildMessages(List<ChatMessage> transcript) throws IOException {
        JSONArray messages = new JSONArray();

        try {
            // Add system message first
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);
            messages.put(systemMsg);

            boolean started = false;
            for (ChatMessage m : transcript) {
                if (m.type == ChatMessage.TYPE_USER) {
                    JSONObject msg = new JSONObject();
                    msg.put("role", "user");
                    msg.put("content", m.text);
                    messages.put(msg);
                    started = true;
                } else if (m.type == ChatMessage.TYPE_BOT) {
                    if (!started) {
                        continue;
                    }
                    JSONObject msg = new JSONObject();
                    msg.put("role", "assistant");
                    msg.put("content", m.text);
                    messages.put(msg);
                }
            }

            if (messages.length() == 1) { // Only system message
                throw new IOException("Aucun message utilisateur à envoyer.");
            }
            return messages;
        } catch (org.json.JSONException e) {
            throw new IOException("Erreur de construction JSON : " + e.getMessage(), e);
        }
    }

    private static String parseApiError(String json) {
        try {
            JSONObject root = new JSONObject(json);
            if (root.has("error")) {
                JSONObject err = root.getJSONObject("error");
                return err.optString("message", err.toString());
            }
        } catch (Exception ignored) {
        }
        return json.length() > 200 ? json.substring(0, 200) + "…" : json;
    }

    private static String parseModelText(String json) throws IOException {
        try {
            JSONObject root = new JSONObject(json);
            if (!root.has("choices")) {
                throw new IOException("Réponse inattendue de l'API.");
            }
            JSONArray choices = root.getJSONArray("choices");
            if (choices.length() == 0) {
                throw new IOException("Aucune réponse du modèle.");
            }
            JSONObject first = choices.getJSONObject(0);
            if (first.has("finish_reason") && "content_filter".equalsIgnoreCase(first.optString("finish_reason"))) {
                throw new IOException("Réponse filtrée pour raisons de sécurité.");
            }
            JSONObject message = first.getJSONObject("message");
            return message.optString("content", "").trim();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Analyse de la réponse impossible : " + e.getMessage(), e);
        }
    }
}
