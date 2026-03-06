package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CacheManager {
    private static final String CACHE_FILE = "translation_cache.json";
    private ObjectMapper mapper;
    
    public CacheManager() {
        this.mapper = new ObjectMapper();
    }
    
    // 保存翻译进度
    public void saveCache(JsonNode rootNode, Set<String> processedEntries) throws IOException {
        ObjectNode cacheNode = mapper.createObjectNode();
        cacheNode.set("data", rootNode);
        
        // 保存已处理的条目
        ObjectNode processedNode = mapper.createObjectNode();
        for (String entryId : processedEntries) {
            processedNode.put(entryId, true);
        }
        cacheNode.set("processed", processedNode);
        
        // 写入缓存文件
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(CACHE_FILE), cacheNode);
        System.out.println("缓存已保存");
    }
    
    // 加载缓存
    public CacheData loadCache() throws IOException {
        File cacheFile = new File(CACHE_FILE);
        if (!cacheFile.exists()) {
            return null;
        }
        
        JsonNode cacheNode = mapper.readTree(cacheFile);
        JsonNode dataNode = cacheNode.get("data");
        JsonNode processedNode = cacheNode.get("processed");
        
        Set<String> processedEntries = new HashSet<>();
        if (processedNode != null && processedNode.isObject()) {
            Iterator<String> fieldNames = processedNode.fieldNames();
            while (fieldNames.hasNext()) {
                processedEntries.add(fieldNames.next());
            }
        }
        
        System.out.println("缓存已加载，已处理 " + processedEntries.size() + " 个条目");
        return new CacheData(dataNode, processedEntries);
    }
    
    // 清除缓存
    public void clearCache() {
        File cacheFile = new File(CACHE_FILE);
        if (cacheFile.exists()) {
            cacheFile.delete();
            System.out.println("缓存已清除");
        }
    }
    
    // 缓存数据类
    public static class CacheData {
        private JsonNode rootNode;
        private Set<String> processedEntries;
        
        public CacheData(JsonNode rootNode, Set<String> processedEntries) {
            this.rootNode = rootNode;
            this.processedEntries = processedEntries;
        }
        
        public JsonNode getRootNode() {
            return rootNode;
        }
        
        public Set<String> getProcessedEntries() {
            return processedEntries;
        }
    }
}