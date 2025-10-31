package org.example;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.Collections;

public class DeepSeekClient02 {
    /**
     * 请求API地址
     */
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    /**
     * 你在DeepSeek官网申请的API KEY，注意不要泄露给他人！
     */
    private static String API_KEY = "sk-bee2886b58d44f76b28b82df50aea92a";
    private final OkHttpClient client = new OkHttpClient();

    public String getResponse(String apiKey, String prompt) throws IOException {
        // 构建请求体
        DeepSeekRequestModel.Message message = DeepSeekRequestModel.Message.builder()
                .role("user")
                .content(prompt).build();
        DeepSeekRequestModel requestBody = DeepSeekRequestModel.builder()
                .model("deepseek-chat")
                .messages(Collections.singletonList(message))
                .build();

        //构建请求体json：{"messages":[{"content":"你好，DeepSeek！","role":"user"}],"model":"deepseek-chat"}
        String jsonBody = JSON.toJSONString(requestBody);
        // 创建HTTP请求
        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        // 发送请求并处理响应
        try (Response response = client.newCall(request).execute()) {
            //如果响应成功，并且返回体有内容，就输出内容，否则表示响应失败
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
            throw new IOException("Unexpected code " + response);
        }
    }

    public static void main(String[] args) {
        String question = "你好，DeepSeek！";
        try {
            String response = new DeepSeekClient02().getResponse(API_KEY, question);
            System.out.println(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


