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
import net.stalemate.networking.server.ConnectionHandler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.stalemate.log.MakeLog.makeLog;

public class ButtonTooltips {
    private static HashMap<String, HashMap<String, String>> tooltips = new HashMap<>();
    private static Logger LOGGER = makeLog(Logger.getLogger(ButtonTooltips.class.getSimpleName()));

    @SuppressWarnings("unchecked")
    public static void init(){
        try {
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

            try {
                tooltips = objectMapper.readValue(new File("config/button_tooltips.yaml"), HashMap.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e){
            LOGGER.log(Level.WARNING,"Failed to load button tooltips");
        }
    }

    public static String getTooltip(String id){
        return (tooltips.get("tooltips")).get(id);
    }

}
