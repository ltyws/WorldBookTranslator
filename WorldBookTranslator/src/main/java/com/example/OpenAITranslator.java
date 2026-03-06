package com.example;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OpenAITranslator {
    private String apiUrl;
    private String apiKey;
    private String model;
    private OkHttpClient client;
    private static final int TIMEOUT_SECONDS = 180;
    private static final int MAX_RETRIES = 1;
    
    public OpenAITranslator(String apiUrl, String apiKey, String model) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();
    }
    
    public String translate(String text) throws IOException {
        int retries = 0;
        while (true) {
            try {
                // 构建请求体
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", model);
                
                JSONArray messages = new JSONArray();
                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", "你是一个专业的翻译助手，请将以下文本从英文翻译成中文，保持原文的格式和结构，不要添加任何额外内容。注意：请确保翻译后的文本不会破坏JSON格式，特别是引号和特殊字符。");
                messages.put(systemMessage);
                
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", text);
                messages.put(userMessage);
                
                requestBody.put("messages", messages);
                requestBody.put("temperature", 0.7);
                
                // 构建请求
                RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
                );
                
                Request request = new Request.Builder()
                    .url(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();
                
                // 发送请求并处理响应
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                    
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    String translatedText = message.getString("content");
                    
                    // 确保翻译后的文本不会破坏JSON格式
                    translatedText = escapeJsonString(translatedText);
                    
                    return translatedText;
                }
            } catch (IOException e) {
                if (retries < MAX_RETRIES) {
                    retries++;
                    System.out.println("翻译请求超时，正在重试... (" + retries + "/" + MAX_RETRIES + ")");
                    // 等待一段时间后重试
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("重试被中断", ie);
                    }
                } else {
                    throw e;
                }
            }
        }
    }
    
    // 转义JSON字符串中的特殊字符
    private String escapeJsonString(String input) {
        // 转义双引号
        input = input.replace("\"", "\\\"");
        // 转义反斜杠
        input = input.replace("\\", "\\\\");
        // 转义换行符
        input = input.replace("\n", "\\n");
        // 转义回车符
        input = input.replace("\r", "\\r");
        // 转义制表符
        input = input.replace("\t", "\\t");
        // 转义退格符
        input = input.replace("\b", "\\b");
        // 转义换页符
        input = input.replace("\f", "\\f");
        return input;
    }
}