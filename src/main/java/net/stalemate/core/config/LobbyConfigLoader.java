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

package net.stalemate.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.stalemate.networking.server.lobby_management.Lobby;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class LobbyConfigLoader {

    @SuppressWarnings("unchecked")
    public static ArrayList<Lobby> loadLobbiesFromConfig(){
        try {
            ArrayList<Lobby> lobbies = new ArrayList<>();

            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

            HashMap<String, Object> config_map = objectMapper.readValue(new File("config/server_lobby_config.yaml"), HashMap.class);

            for (HashMap<String, Object> map: (ArrayList<HashMap<String, Object>>)config_map.get("lobbies")){
                lobbies.add(new Lobby((String)map.get("map"), (ArrayList<String>) map.get("next_maps")));
            }

            return lobbies;
        } catch (Exception e){
            System.err.println("Failed to load lobby configuration.");
        }
        return new ArrayList<>();
    }
}
