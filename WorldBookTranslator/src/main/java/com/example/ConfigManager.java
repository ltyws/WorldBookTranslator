package com.example;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigManager {
    private String apiUrl;
    private String apiKey;
    private String model;
    
    public void loadConfig() throws IOException {
        // 检查配置文件是否存在
        File configFile = new File("config.properties");
        if (!configFile.exists()) {
            // 创建默认配置文件
            String defaultConfig = "# OpenAI API 配置\napi.url=https://api.openai.com/v1/chat/completions\napi.key=your_api_key_here\nmodel=gpt-3.5-turbo";
            Files.write(Paths.get("config.properties"), defaultConfig.getBytes());
            System.out.println("已创建默认配置文件 config.properties，请编辑并填写您的API信息");
            System.exit(1);
        }
        
        // 加载配置文件
        Config config = ConfigFactory.parseFile(configFile);
        apiUrl = config.getString("api.url");
        apiKey = config.getString("api.key");
        model = config.getString("model");
        
        // 验证配置
        if (apiKey.equals("your_api_key_here")) {
            System.out.println("请编辑 config.properties 文件并填写您的API密钥");
            System.exit(1);
        }
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public String getModel() {
        return model;
    }
}