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

package net.stalemate.server.core.map_system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import net.stalemate.server.core.Entity;
import net.stalemate.server.core.MapObject;
import net.stalemate.server.core.Unit;
import net.stalemate.server.core.gamemode.IGamemode;
import net.stalemate.server.core.gamemode.gamemodes.CaptureTheCity;
import net.stalemate.server.core.gamemode.gamemodes.Sandbox;
import net.stalemate.server.core.gamemode.gamemodes.Versus;
import net.stalemate.server.core.units.*;
import net.stalemate.server.core.units.buildings.Fortification;
import net.stalemate.server.core.units.buildings.MilitaryTentBase;
import net.stalemate.server.core.units.buildings.TankFactory;
import net.stalemate.server.core.util.IGameController;
import net.stalemate.server.core.controller.Game;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

// todo: check what happens if loaded incorrect map
// todo: make it don't load too small or too big maps
@SuppressWarnings("unchecked")
public class MapLoader {

    public static class EntityRegistry{
        private static final HashMap<String, Class<? extends Entity>> entity_registry = new HashMap<>();

        /***
         * Add an Entity to registry with (int x, int y, IGameController g) constructor
         */
        public static void addEntity(String id, Class<? extends Entity> entity){
            entity_registry.put(id, entity);
        }

        public static Entity constructEntity(String id, int x, int y, IGameController gameController){
            try {
                return entity_registry.get(id).getConstructor(new Class[] { int.class, int.class, IGameController.class}).newInstance(x, y, gameController);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
                return null;
            }
        }

    }

    public static class GamemodeRegistry{
        private static final HashMap<String, Class<? extends IGamemode>> gamemode_registry = new HashMap<>();

        public static void addGamemode(String id, Class<? extends IGamemode> gamemode){
            gamemode_registry.put(id, gamemode);
        }

        public static IGamemode constructGamemode(String id){
            try {
                return gamemode_registry.get(id).getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    static {
        EntityRegistry.addEntity("base", MilitaryTentBase.class);
        EntityRegistry.addEntity("fortification", Fortification.class);
        // addEntity("supply_station", SupplyStation.class);
        EntityRegistry.addEntity("tank_factory", TankFactory.class);

        EntityRegistry.addEntity("infantry", Infantry.class);
        EntityRegistry.addEntity("engineer", EngineerUnit.class);
        EntityRegistry.addEntity("anti_tank", AntiTank.class);
        EntityRegistry.addEntity("artillery", Artillery.class);
        EntityRegistry.addEntity("light_tank", LightTank.class);
        EntityRegistry.addEntity("heavy_tank", HeavyTank.class);
        EntityRegistry.addEntity("motorized_unit", MotorizedUnit.class);
        EntityRegistry.addEntity("fighter", Fighter.class);

        GamemodeRegistry.addGamemode("versus", Versus.class);
        GamemodeRegistry.addGamemode("dev", Sandbox.class);
        GamemodeRegistry.addGamemode("ctc", CaptureTheCity.class);
    }

    @SuppressWarnings("unchecked")
    public static Game load(String map_path){
        try {
            PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                    .build();
            ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
            HashMap<String, Object> map = null;
            try {
                map = objectMapper.readValue(new File(map_path), HashMap.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            assert map != null;
            HashMap<String, Object> map_data = (HashMap<String, Object>) map.get("map_data");

            HashMap<String, Object> aparams;

            if (map_data.containsKey("aparams")){
                if (map_data.get("aparams") instanceof HashMap){
                    aparams = (HashMap<String, Object>) map_data.get("aparams");
                }
                else{
                    aparams = new HashMap<>();
                }
            }
            else{
                aparams = new HashMap<>();
            }

            // Load tileset
            HashMap<String, MapObject> tileset = new HashMap<>();
            ArrayList<HashMap<String, Object>> tileset_loaded = (ArrayList<HashMap<String, Object>>) map_data.get("tileset_data");

            for (HashMap<String, Object> tile : tileset_loaded) {
                tileset.put((String) tile.get("name"), new MapObject(((String) tile.get("tileDir")), ((boolean) tile.get("isPassable"))));
            }

            // Load map
            ArrayList<ArrayList<String>> loaded_map = (ArrayList<ArrayList<String>>) map.get("mpobjects");
            ArrayList<ArrayList<MapObject>> map_ = new ArrayList<>();

            int y = 0;
            for (ArrayList<String> row : loaded_map) {
                map_.add(new ArrayList<>());
                for (String x : row) {
                    map_.get(y).add(tileset.get(x));
                }
                y++;
            }

            // Load teams

            ArrayList<Game.Team> teams = new ArrayList<>();
            HashMap<String, Game.Team> team_id_hashmap = new HashMap<>();
            for (HashMap<String, Object> team : (ArrayList<HashMap<String, Object>>) map_data.get("team_data")) {
                Game.Team t = new Game.Team(new Color(((ArrayList<Integer>) (team.get("rgb"))).get(0), ((ArrayList<Integer>) (team.get("rgb"))).get(1), ((ArrayList<Integer>) (team.get("rgb"))).get(2)));
                if ((boolean) team.get("enable_dev")) {
                    t.setDev();
                }
                teams.add(t);
                team_id_hashmap.put((String) team.get("id"), t);
            }

            // Entity loading

            Game g = new Game(map_, teams, aparams);
            g.setMode(GamemodeRegistry.constructGamemode((String) map_data.get("mode")));

            for (HashMap<String, Object> entity : (ArrayList<HashMap<String, Object>>) map.get("entity_data")) {
                Entity ent = EntityRegistry.constructEntity((String) entity.get("id"), (int) entity.get("x"), (int) entity.get("y"), g);

                if (entity.get("class").equals("unit")) {
                    team_id_hashmap.get((String) entity.get("team")).getTeamUnits().add((Unit) ent);
                }
                g.forceAddEntity(ent);
            }

            return g;
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Failed to load map");
            System.exit(-1);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static int getMapPlayerCount(String map_path){
        try {
            PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                    .build();
            ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
            HashMap<String, Object> map = null;
            try {
                map = objectMapper.readValue(new File(map_path), HashMap.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            assert map != null;
            HashMap<String, Object> map_data = (HashMap<String, Object>) map.get("map_data");
            ArrayList<HashMap<String, Object>> teams = (ArrayList<HashMap<String, Object>>) map_data.get("team_data");

            int player_count = 0;
            for (HashMap<String, Object> team: teams){
                if (!((boolean) team.get("enable_dev"))){
                    player_count++;
                }
            }
            return player_count;
        } catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    public static String getMapName(String map_path){
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .build();
        ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
        HashMap<String, Object> map = null;
        try {
            map = objectMapper.readValue(new File(map_path), HashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert map != null;
        HashMap<String, Object> map_data = (HashMap<String, Object>) map.get("map_data");
        return (String) map_data.get("name");
    }
}
