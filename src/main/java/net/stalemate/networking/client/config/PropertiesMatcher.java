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

package net.stalemate.networking.client.config;

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
