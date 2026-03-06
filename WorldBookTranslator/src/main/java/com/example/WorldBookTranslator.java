package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class WorldBookTranslator {
    public static void main(String[] args) {
        System.out.println("开始翻译世界书文件...");
        try {
            // 加载配置
            System.out.println("加载配置文件...");
            ConfigManager configManager = new ConfigManager();
            configManager.loadConfig();
            System.out.println("配置加载成功");
            
            // 初始化OpenAI翻译器
            System.out.println("初始化翻译器...");
            OpenAITranslator translator = new OpenAITranslator(
                configManager.getApiUrl(),
                configManager.getApiKey(),
                configManager.getModel()
            );
            System.out.println("翻译器初始化成功");
            
            // 初始化缓存管理器
            System.out.println("初始化缓存管理器...");
            CacheManager cacheManager = new CacheManager();
            
            // 初始化JSON处理器
            JsonProcessor jsonProcessor = new JsonProcessor();
            
            // 读取输入文件或缓存
            System.out.println("读取输入文件...");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode;
            Set<String> processedEntries = new HashSet<>();
            
            // 尝试加载缓存
            CacheManager.CacheData cacheData = cacheManager.loadCache();
            if (cacheData != null) {
                rootNode = cacheData.getRootNode();
                processedEntries = cacheData.getProcessedEntries();
                System.out.println("从缓存加载数据");
            } else {
                // 从原始文件加载
                File inputFile = new File("input_worldbook.json");
                if (!inputFile.exists()) {
                    System.out.println("错误：输入文件 input_worldbook.json 不存在");
                    return;
                }
                rootNode = mapper.readTree(inputFile);
                System.out.println("输入文件读取成功");
            }
            
            // 处理每个条目
            if (rootNode.has("entries")) {
                System.out.println("开始处理条目...");
                JsonNode entriesNode = rootNode.get("entries");
                Iterator<Map.Entry<String, JsonNode>> iterator = ((ObjectNode) entriesNode).fields();
                int count = 0;
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    String key = entry.getKey();
                    JsonNode entryNode = entry.getValue();
                    
                    // 跳过已处理的条目
                    if (processedEntries.contains(key)) {
                        System.out.println("跳过已处理的条目 " + key);
                        continue;
                    }
                    
                    count++;
                    System.out.println("处理条目 " + key + "...");
                    
                    try {
                        // 翻译comment
                        if (entryNode.has("comment") && !entryNode.get("comment").isNull()) {
                            String originalComment = entryNode.get("comment").asText();
                            if (!originalComment.isEmpty()) {
                                System.out.println("翻译comment...");
                                String translatedComment = translator.translate(originalComment);
                                ((ObjectNode) entryNode).put("comment", translatedComment);
                                System.out.println("comment翻译完成");
                            }
                        }
                        
                        // 翻译content
                        if (entryNode.has("content") && !entryNode.get("content").isNull()) {
                            String originalContent = entryNode.get("content").asText();
                            if (!originalContent.isEmpty()) {
                                System.out.println("翻译content...");
                                // 拆分长文本
                                TextSplitter splitter = new TextSplitter();
                                String translatedContent = splitter.splitAndTranslate(originalContent, translator);
                                ((ObjectNode) entryNode).put("content", translatedContent);
                                System.out.println("content翻译完成");
                            }
                        }
                        
                        // 翻译key
                        if (entryNode.has("key") && !entryNode.get("key").isNull() && entryNode.get("key").isArray()) {
                            System.out.println("翻译key...");
                            ArrayNode keyArray = (ArrayNode) entryNode.get("key");
                            for (int i = 0; i < keyArray.size(); i++) {
                                String originalKey = keyArray.get(i).asText();
                                if (!originalKey.isEmpty()) {
                                    String translatedKey = translator.translate(originalKey);
                                    keyArray.set(i, mapper.valueToTree(translatedKey));
                                }
                            }
                            System.out.println("key翻译完成");
                        }
                        
                        // 标记为已处理
                        processedEntries.add(key);
                        System.out.println("条目 " + key + " 处理完成");
                        
                        // 每处理5个条目保存一次缓存
                        if (count % 5 == 0) {
                            cacheManager.saveCache(rootNode, processedEntries);
                        }
                    } catch (Exception e) {
                        System.out.println("处理条目 " + key + " 时出错：" + e.getMessage());
                        e.printStackTrace();
                        // 保存当前进度
                        cacheManager.saveCache(rootNode, processedEntries);
                        throw e;
                    }
                }
                System.out.println("共处理 " + count + " 个条目");
            } else {
                System.out.println("错误：JSON文件中没有 entries 字段");
            }
            
            // 写入输出文件
            System.out.println("写入输出文件...");
            File outputFile = new File("output_worldbook_translated.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, rootNode);
            System.out.println("翻译完成！输出文件已生成：" + outputFile.getAbsolutePath());
            
            // 清除缓存
            cacheManager.clearCache();
            
        } catch (Exception e) {
            System.out.println("错误：" + e.getMessage());
            e.printStackTrace();
        }
    }
}