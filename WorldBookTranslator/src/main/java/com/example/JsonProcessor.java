package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;

public class JsonProcessor {
    private ObjectMapper mapper;
    
    public JsonProcessor() {
        this.mapper = new ObjectMapper();
    }
    
    public JsonNode readJsonFile(String filePath) throws IOException {
        return mapper.readTree(new File(filePath));
    }
    
    public void writeJsonFile(JsonNode node, String filePath) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), node);
    }
    
    public ObjectNode getObjectNode(JsonNode node) {
        return (ObjectNode) node;
    }
}