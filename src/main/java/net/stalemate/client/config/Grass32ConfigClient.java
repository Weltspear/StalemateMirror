/*
 * Stalemate Game
 * Copyright (C) 2022 Weltspear
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.stalemate.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.util.HashMap;

public class Grass32ConfigClient {
    private static String nickname = "unnamed";
    private static int timeout = 30;
    private static int lobby_timeout = 120;
    private static boolean steel_button_overlay = false;

    private static String resolution = "832x576";

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
            steel_button_overlay = (boolean) client_config.get("steel_button_overlay");
            resolution = (String) client_config.get("resolution");

            HashMap<String, HashMap<String, String>> controls = (HashMap<String, HashMap<String, String>>) client_config.get("controls");

            KeyboardBindMapper.makeBinds(controls.get("keyb"));

        } catch (Exception e){
            System.err.println("Failed to load grass32.");
            e.printStackTrace();
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

    public static boolean doSteelButtonOverlay() {
        return steel_button_overlay;
    }

    public static String getResolution(){return resolution;}
}
