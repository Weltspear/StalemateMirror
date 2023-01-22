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

package net.stalemate.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import net.stalemate.client.property.ClientSideProperty;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class ClientGame {

    private int mp;
    private boolean is_it_your_turn;
    private ArrayList<ClientEntity> entities = new ArrayList<>();
    private boolean[][] fog_of_war;
    private ClientSelectedUnit selectedUnit;
    private final ClientMapLoader mapLoader;
    private ArrayList<String> chat;

    public String getMapPath() {
        return map_path;
    }

    private String map_path;

    private final ReentrantLock lock = new ReentrantLock();

    public ClientGame(ClientMapLoader mapLoader){
        this.mapLoader = mapLoader;
        //this.fog_of_war = new boolean[mapLoader.getHeight()][mapLoader.getWidth()];
    }

    public int getMp() {
        return mp;
    }

    public boolean isIsItYourTurn() {
        return is_it_your_turn;
    }


    public static class ClientSelectedUnit {
        private final ArrayList<ClientSideProperty> properties;
        private final BufferedImage texture;
        private final ArrayList<ClientUnitQueueElement> queue;
        private final ClientButton[] buttons;
        private final BufferedImage iselectorbutton_press;

        public BufferedImage getISelectorButtonPress() {
            return iselectorbutton_press;
        }

        public ClientSelectedUnit(ArrayList<ClientSideProperty> properties, BufferedImage texture,
                                  ArrayList<ClientUnitQueueElement> queue, ClientButton[] buttons,
                                  BufferedImage iselectorbutton_press) {
            this.properties = properties;
            this.texture = texture;
            this.queue = queue;
            this.buttons = buttons;
            this.iselectorbutton_press = iselectorbutton_press;
        }

        public ArrayList<ClientSideProperty> getProperties() {
            return properties;
        }

        public BufferedImage getTexture() {
            return texture;
        }

        public ArrayList<ClientUnitQueueElement> getQueue() {
            return queue;
        }

        public ClientButton[] getButtons() {
            return buttons;
        }

        public static class ClientButton{
            private final String id;
            private final String bind;
            private final BufferedImage image;
            private final Mode mode;

            public ClientButton(String id, String bind, BufferedImage image, Mode mode) {
                this.id = id;
                this.bind = bind;
                this.image = image;
                this.mode = mode;
            }

            public String getId() {
                return id;
            }

            public String getBind() {
                return bind;
            }

            public BufferedImage getImage() {
                return image;
            }

            public Mode getMode() {
                return mode;
            }

            public enum Mode{
                SelectorButton,
                StandardButton
            }

        }

        public static class ClientUnitQueueElement {
            private final BufferedImage texture;
            private final int turn_time;

            public ClientUnitQueueElement(BufferedImage texture, int turn_time) {
                this.texture = texture;
                this.turn_time = turn_time;
            }

            public BufferedImage getTexture() {
                return texture;
            }

            public int getTurn_time() {
                return turn_time;
            }
        }

    }

    public static class ClientEntity{

        private final int x;
        private final int y;
        private final boolean flip;
        private final BufferedImage texture;

        public ClientEntity(int x, int y, boolean flip, BufferedImage texture){
            this.x = x;
            this.y = y;
            this.flip = flip;
            this.texture = texture;
        }

        public BufferedImage getTexture() {
            return texture;
        }

        public boolean isFlip() {
            return flip;
        }

        public int getY() {
            return y;
        }

        public int getX() {
            return x;
        }
    }


    public static class ClientUnit extends ClientEntity{

        private final int hp;
        private final int max_hp;
        private final int su;
        private final int max_su;
        private final int et;

        private final Color team_color;
        private final boolean transparent;
        private final int fog_of_war_range;
        private final boolean is_our;

        public ClientUnit(int x, int y, boolean flip, BufferedImage texture, int rgb,
                          ArrayList<Integer> stats, boolean transparent,
                          int fog_of_war_range, boolean is_our) {
            super(x, y, flip, texture);

            hp = stats.get(0);
            max_hp = stats.get(1);
            su = stats.get(2);
            max_su = stats.get(3);
            et = stats.get(4);

            team_color = new Color(rgb);
            this.transparent = transparent;
            this.fog_of_war_range = fog_of_war_range;
            this.is_our = is_our;
        }

        public int getHp() {
            return hp;
        }

        public int getMaxHp() {
            return max_hp;
        }

        public int getSu() {
            return su;
        }

        public int getMaxSu() {
            return max_su;
        }

        public int getEt() {
            return et;
        }

        public Color getTeamColor() {
            return team_color;
        }

        public int getFogOfWarRange() {
            return fog_of_war_range;
        }

        public boolean isIs_our() {
            return is_our;
        }

        public boolean isTransparent() {
            return transparent;
        }

    }

    @SuppressWarnings("unchecked")
    public void load(String json) throws JsonProcessingException {
        lock.lock();

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .build();
        ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
        Map<String, Object> data_map = objectMapper.readValue(json, Map.class);

        map_path = (String) data_map.get("map_path");
        mapLoader.load(map_path); // This will crash anyway if there are issues with map because map is being loaded in InGameUI too
        selectedUnit = null;

        ArrayList<HashMap<String, Object>> entity_data = (ArrayList<HashMap<String, Object>>) data_map.get("entity_data");
        if (!(data_map.get("selected_unit_data") instanceof Integer)) {
            HashMap<String, Object> selected_unit = (HashMap<String, Object>) data_map.get("selected_unit_data");

            // Create PropertiesToRender
            ArrayList<ArrayList<String>> unready_properties = (ArrayList<ArrayList<String>>) selected_unit.get("properties");
            ArrayList<ClientSideProperty> clientSideProperties = new ArrayList<>();
            ArrayList<ClientSelectedUnit.ClientUnitQueueElement> unit_queue = null;
            for (ArrayList<String> p : unready_properties){
                clientSideProperties.add(new ClientSideProperty(p.get(0), p.get(1)));
            }

            // Add "big unit texture"
            BufferedImage unit_texture = AssetLoader.load((String) selected_unit.get("texture"));

            // Buttons
            ArrayList<Object> buttons_ = (ArrayList<Object>) selected_unit.get("buttons");

            ClientSelectedUnit.ClientButton[] buttons = new ClientSelectedUnit.ClientButton[9];

            int idx = 0;
            for (Object button : buttons_) {

                if (!(button instanceof Integer)) {
                    HashMap<String, Object> b = (HashMap<String, Object>) button;

                    // Add textures to ArrayList
                    BufferedImage texture = AssetLoader.load((String) b.get("texture"));
                    String id = (String) b.get("id");
                    String bind = (String) b.get("bind");
                    ClientSelectedUnit.ClientButton.Mode mode = (Integer) b.get("mode") == 1 ?
                    ClientSelectedUnit.ClientButton.Mode.StandardButton : ClientSelectedUnit.ClientButton.Mode.SelectorButton;

                    buttons[idx] = new ClientSelectedUnit.ClientButton(id, bind, texture, mode);
                }
                idx++;
            }


            if (!(selected_unit.get("queue") instanceof Integer)) {
                unit_queue = new ArrayList<>();
                ArrayList<Object> queue_ = (ArrayList<Object>) selected_unit.get("queue");

                for (Object member : queue_) {
                    if (!(member instanceof Integer)) {
                        HashMap<String, Object> m = (HashMap<String, Object>) member;

                        BufferedImage texture = AssetLoader.load((String) m.get("texture"));
                        int turn_time = (int) m.get("turn_time");

                        unit_queue.add(new ClientSelectedUnit.ClientUnitQueueElement(texture, turn_time));
                    }
                    else{
                        unit_queue.add(null);
                    }
                }
            }

            BufferedImage selector = null;

            // Selector texture thingy
            if (((boolean) selected_unit.get("iselectorbutton_press"))){
                // iselectorbutton_data_texture
                selector = AssetLoader.load((String)selected_unit.get("iselectorbutton_data_selector_texture"));
            }
            if (selector == null){
                selector = AssetLoader.load("assets/ui/selectors/ui_select.png");
            }

            selectedUnit = new ClientSelectedUnit(clientSideProperties, unit_texture, unit_queue, buttons, selector);

        }

        // Load Entities

        entities = new ArrayList<>();

        for (HashMap<String, Object> _entity: entity_data){
            String type = (String) _entity.get("type");
            int x = (int) _entity.get("x");
            int y = (int) _entity.get("y");
            boolean flip = (boolean) _entity.get("flip");
            BufferedImage texture = AssetLoader.load((String) _entity.get("texture"));

            if (Objects.equals(type, "entity")){
                ClientEntity entity = new ClientEntity(x, y, flip, texture);
                entities.add(entity);
            }
            else {
                ArrayList<Integer> stats = (ArrayList<Integer>) _entity.get("stats");
                boolean transparent = (boolean) _entity.get("transparent");
                boolean is_our = (boolean) _entity.get("is_our");
                int rgb = (int) _entity.get("rgb");
                int fog_of_war_range = (int) _entity.get("fog_of_war_range");

                ClientUnit unit = new ClientUnit(x,y,flip,texture,rgb,stats,transparent,fog_of_war_range,is_our);
                entities.add(unit);
            }

        }

        mp = (int) data_map.get("mp");
        is_it_your_turn = (boolean) data_map.get("is_it_your_turn");

        // Build fog of war
        fog_of_war = new boolean[mapLoader.getHeight()][mapLoader.getWidth()];

        for (ClientEntity entity: entities){
            if (entity instanceof ClientUnit u){
                if (u.is_our) {
                    for (int y = u.getY()-u.getFogOfWarRange(); y <= u.getY()+u.getFogOfWarRange(); y++){
                        for (int x = u.getX()-u.getFogOfWarRange(); x <= u.getX()+u.getFogOfWarRange(); x++){
                            if (x >= 0 && y >= 0 && x < fog_of_war.length && y < fog_of_war[0].length){
                                fog_of_war[y][x] = true;
                            }
                        }
                    }
                }
            }
        }

        chat = (ArrayList<String>) data_map.get("chat");

        lock.unlock();

    }

    public Object[] buildView(int x, int y){
        try {
            lock.lock();
            Object[] fog_of_war_and_entities = new Object[2];
            boolean[][] fog_of_war_s = new boolean[7][15];
            ClientEntity[][] entities_s = new ClientEntity[7][15];

            for (ClientEntity entity : entities) {
                if (entity.getY() >= y - 1 && entity.getY() < y + 6) {
                    if (entity.getX() >= x - 1 && entity.getX() < x + 14) {
                        if (entity instanceof ClientUnit u) {
                            if (!u.isTransparent())
                                entities_s[entity.getY() - y + 1][entity.getX() - x + 1] = entity;
                        }
                    }
                }
            }

            for (ClientEntity entity : entities) {
                if (entity.getY() >= y - 1 && entity.getY() < y + 6) {
                    if (entity.getX() >= x - 1 && entity.getX() < x + 14) {
                        if (entities_s[entity.getY() - y + 1][entity.getX() - x + 1] == null)
                            entities_s[entity.getY() - y + 1][entity.getX() - x + 1] = entity;
                    }
                }
            }

            for (int _y = y - 1; _y < y + 6; _y++) {
                for (int _x = x - 1; _x < x + 14; _x++) {
                    if (_x >= 0 && _y >= 0 && _y < mapLoader.getHeight() && _x < mapLoader.getWidth()) {
                        boolean s = fog_of_war[_y][_x];
                        fog_of_war_s[_y - y + 1][_x - x + 1] = s;
                    }
                }
            }

            fog_of_war_and_entities[0] = entities_s;
            fog_of_war_and_entities[1] = fog_of_war_s;
            return fog_of_war_and_entities;
        } finally {
            lock.unlock();
        }
    }

    public ArrayList<String> getChat(){
        try {
            lock.lock();
            return chat;
        } finally {
            lock.unlock();
        }
    }

    public ClientSelectedUnit getSelectedUnit(){
        try {
            lock.lock();
            return selectedUnit;
        } finally {
            lock.unlock();
        }
    }



}
