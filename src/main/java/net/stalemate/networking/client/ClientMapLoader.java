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

package net.stalemate.networking.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import net.libutils.error.ErrorResult;
import net.libutils.error.Expect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ClientMapLoader {
    private String map_path = "";
    private ArrayList<ArrayList<String>> map = new ArrayList<>();

    public static class ClientMapLoaderError implements ErrorResult {
        @Override
        public String message() {
            return "Failed to load map";
        }
    }

    public ClientMapLoader(){

    }

    private record Tile(String texture_file){};

    @SuppressWarnings("unchecked")
    public Expect<String, ClientMapLoaderError> load(String map_path) {
        try {
            if (!this.map_path.equals(map_path)) {
                PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                        .build();
                ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
                HashMap<String, Object> map = null;

                map = objectMapper.readValue(new File(map_path), HashMap.class);

                HashMap<String, Object> map_data = (HashMap<String, Object>) map.get("map_data");

                this.map_path = map_path;

                // Load tileset
                HashMap<String, Tile> tileset = new HashMap<>();
                ArrayList<HashMap<String, Object>> tileset_loaded = (ArrayList<HashMap<String, Object>>) map_data.get("tileset_data");

                for (HashMap<String, Object> tile : tileset_loaded) {
                    tileset.put((String) tile.get("name"), new Tile(((String) tile.get("tileDir"))));
                }

                // Load map
                ArrayList<ArrayList<String>> loaded_map = (ArrayList<ArrayList<String>>) map.get("mpobjects");
                this.map = new ArrayList<>();

                int y = 0;
                for (ArrayList<String> row : loaded_map) {
                    this.map.add(new ArrayList<>());

                    for (String x : row) {
                        this.map.get(y).add(tileset.get(x).texture_file);
                    }

                    y++;
                }
            }
            return new Expect<>("");
        } catch (IOException e){
            return new Expect<>(new ClientMapLoaderError());
        }
    }

    public ArrayList<ArrayList<String>> getMap(int cam_x, int cam_y) {
        ArrayList<ArrayList<String>> map_textures = new ArrayList<>();
        int y2 = 0;
        for (int y = 0; y < 5; y++){
            map_textures.add(new ArrayList<>());
            for (int x = 0; x < 13; x++){
                if (cam_x + x >= 0 && cam_y + y >= 0)
                    if (cam_y + y < this.map.size())
                        if (cam_x + x < this.map.get(cam_y+y).size())
                            map_textures.get(y2).add(this.map.get(cam_y+y).get(cam_x+x));
                        else
                            map_textures.get(y2).add("empty.png");
                    else
                        map_textures.get(y2).add("empty.png");
                else
                    map_textures.get(y2).add("empty.png");
                boolean has_space_been_filled = false;
            }
            y2++;
        }
        return map_textures;
    }
}
