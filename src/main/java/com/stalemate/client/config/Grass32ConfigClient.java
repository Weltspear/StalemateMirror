package com.stalemate.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.util.HashMap;

public class Grass32ConfigClient {
    private static String nickname = "unnamed";
    private static int timeout = 30;
    private static int lobby_timeout = 120;

    @SuppressWarnings("unchecked")
    public static void loadGrass32(){
        try {

            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

            HashMap<String, Object> config_map = objectMapper.readValue(new File("grass32"), HashMap.class);

            HashMap<String, Object> client_config =
                    (HashMap<String, Object>) ((HashMap<String, Object>) (config_map.get("config"))).get("client");

            nickname = (String) client_config.get("nickname");
            timeout = (int) client_config.get("timeout");
            lobby_timeout = (int) client_config.get("lobby_timeout");

        } catch (Exception e){
            System.err.println("Failed to load grass32.");
        }
    }

    public static String getNickname() {
        return nickname;
    }

    public static int getTimeout() {
        return timeout;
    }

    public static int getLobbyTimeout() {
        return lobby_timeout;
    }
}
