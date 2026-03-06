package com.example;

import java.io.IOException;

public class TextSplitter {
    private static final int MAX_CHUNK_SIZE = 2000; // 根据API限制调整
    
    public String splitAndTranslate(String text, OpenAITranslator translator) throws IOException {
        if (text.length() <= MAX_CHUNK_SIZE) {
            return translator.translate(text);
        }
        
        StringBuilder result = new StringBuilder();
        int start = 0;
        int end = MAX_CHUNK_SIZE;
        
        while (start < text.length()) {
            // 尝试在段落边界处拆分
            if (end < text.length()) {
                int lastNewline = text.lastIndexOf("\n", end);
                if (lastNewline > start) {
                    end = lastNewline;
                }
            }
            
            String chunk = text.substring(start, end);
            String translatedChunk = translator.translate(chunk);
            result.append(translatedChunk);
            
            start = end;
            end = Math.min(start + MAX_CHUNK_SIZE, text.length());
        }
        
        return result.toString();
    }
}