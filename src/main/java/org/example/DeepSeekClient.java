package org.example;

import okhttp3.*;
import org.example.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class DeepSeekClient {
    private static final String API = "https://api.deepseek.com/v1/chat/completions";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String chat(String key, String user) throws IOException {
        return call(key, List.of(new Message("user", user)), "deepseek-chat", 0.7);
    }

    public String code(String key, String user) throws IOException {
        return call(key, List.of(new Message("user", user)), "deepseek-coder", 0.2);
    }

    public String chatWithContext(String key, List<Message> hist, String newUser) throws IOException {
        List<Message> tmp = new java.util.ArrayList<>(hist);
        tmp.add(new Message("user", newUser));
        return call(key, tmp, "deepseek-chat", 0.7);
    }

    private String call(String key, List<Message> messages, String model, double temp) throws IOException {
        // 把原来出现 DeepSeekRequest.Message 的地方全部换成 **公共 Message**
        List<Message> reqMsgs = messages.stream()
                .map(m -> Message.builder()
                        .role(m.getRole())
                        .content(m.getContent())
                        .build())
                .collect(Collectors.toList());
        DeepSeekRequest req = DeepSeekRequest.builder()
                .model(model).messages(reqMsgs)
                .temperature(temp).top_p(0.95).build();

        RequestBody body = RequestBody.create(
                mapper.writeValueAsString(req),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(API).post(body)
                .addHeader("Authorization", "Bearer " + key)
                .build();

        try (Response resp = client.newCall(request).execute()) {
            if (!resp.isSuccessful() || resp.body() == null)
                throw new IOException("HTTP " + resp.code());
            DeepSeekResponse res = mapper.readValue(resp.body().string(), DeepSeekResponse.class);
            return res.getChoices().get(0).getMessage().getContent();
        }
    }
}