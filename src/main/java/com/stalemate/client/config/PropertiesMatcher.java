package com.stalemate.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.util.HashMap;

public class PropertiesMatcher {
    private static HashMap<String, String> properties = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static void loadPropertyMatcher(){
        try {

            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

            properties = (HashMap<String, String>) objectMapper.readValue(new File("config/properties.yaml"), HashMap.class).get("properties");

        } catch (Exception e){
            System.err.println("Failed to load grass32.");
        }
    }

    public static String matchKeyToString(String key){
        if (properties.containsKey(key)){
            return properties.get(key);
        }
        return null;
    }
}
