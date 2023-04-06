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
import net.libutils.error.Expect;
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
    private Color teamDoingTurnColor = Color.WHITE;
    private String teamDoingTurnNick = "";

    public String getMapPath() {
        return map_path;
    }

    private String map_path;

    private final ReentrantLock lock = new ReentrantLock();

    private boolean blinkM = true;
    private int blinkMT = 0;
    private final ArrayList<Integer[]> coords_atk = new ArrayList<>();

    public ClientGame(ClientMapLoader mapLoader){
        this.mapLoader = mapLoader;
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
        private final int x;
        private final int y;
        private final int sel_r;

        public BufferedImage getISelectorButtonPress() {
            return iselectorbutton_press;
        }

        public ClientSelectedUnit(ArrayList<ClientSideProperty> properties, BufferedImage texture,
                                  ArrayList<ClientUnitQueueElement> queue, ClientButton[] buttons,
                                  BufferedImage iselectorbutton_press, int x, int y, int sel_r) {
            this.properties = properties;
            this.texture = texture;
            this.queue = queue;
            this.buttons = buttons;
            this.iselectorbutton_press = iselectorbutton_press;
            this.x = x;
            this.y = y;
            this.sel_r = sel_r;
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

        public int getSelR() {
            return sel_r;
        }

        public int getY() {
            return y;
        }

        public int getX() {
            return x;
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

        private final String texture_n;
        private final int hp;
        private final int max_hp;
        private final int su;
        private final int max_su;
        private final int et;

        private final Color team_color;
        private final boolean transparent;
        private final int fog_of_war_range;
        private final boolean is_our;

        public ClientUnit(int x, int y, boolean flip, BufferedImage texture, String texture_n, int rgb,
                          ArrayList<Integer> stats, boolean transparent,
                          int fog_of_war_range, boolean is_our) {
            super(x, y, flip, texture);
            this.texture_n = texture_n;

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

        public String getTextureLoc() {
            return texture_n;
        }
    }

    @SuppressWarnings("unchecked")
    public Expect<String, ?> load(String json) {
        lock.lock();

        try {
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
                for (ArrayList<String> p : unready_properties) {
                    clientSideProperties.add(new ClientSideProperty(p.get(0), p.get(1)));
                }

                // Add "big unit texture"
                BufferedImage unit_texture = AssetLoader.load((String) selected_unit.get("texture"));

                // x, y, selector range
                int x = (int) selected_unit.get("x");
                int y = (int) selected_unit.get("y");

                int sel_r = 0;
                if (selected_unit.containsKey("sel_r")){
                    sel_r = (int) selected_unit.get("sel_r");
                }

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
                        } else {
                            unit_queue.add(null);
                        }
                    }
                }

                BufferedImage selector = null;

                // Selector texture thingy
                if (((boolean) selected_unit.get("iselectorbutton_press"))) {
                    // iselectorbutton_data_texture
                    selector = AssetLoader.load((String) selected_unit.get("iselectorbutton_data_selector_texture"));
                }
                if (selector == null) {
                    selector = AssetLoader.load("assets/ui/selectors/ui_select.png");
                }

                int rgb = (int) selected_unit.get("rgb");

                if (SpecialTeamReprReg.getTeamRepr((String) selected_unit.get("texture")) != null){
                    SpecialTeamReprReg.TeamRepr teamRepr = SpecialTeamReprReg.getTeamRepr((String) selected_unit.get("texture"));
                    for (int[] c: teamRepr.getCoords()){
                        unit_texture.setRGB(c[0], c[1], rgb);
                    }
                }

                selectedUnit = new ClientSelectedUnit(clientSideProperties, unit_texture, unit_queue, buttons, selector, x, y, sel_r);

            }

            // Load Entities

            entities = new ArrayList<>();

            for (HashMap<String, Object> _entity : entity_data) {
                String type = (String) _entity.get("type");
                int x = (int) _entity.get("x");
                int y = (int) _entity.get("y");
                boolean flip = (boolean) _entity.get("flip");
                BufferedImage texture = AssetLoader.load((String) _entity.get("texture"));

                if (Objects.equals(type, "entity")) {
                    ClientEntity entity = new ClientEntity(x, y, flip, texture);
                    entities.add(entity);
                } else {
                    ArrayList<Integer> stats = (ArrayList<Integer>) _entity.get("stats");
                    boolean transparent = (boolean) _entity.get("transparent");
                    boolean is_our = (boolean) _entity.get("is_our");
                    int rgb = (int) _entity.get("rgb");
                    int fog_of_war_range = (int) _entity.get("fog_of_war_range");

                    ClientUnit unit = new ClientUnit(x, y, flip, texture, (String) _entity.get("texture"), rgb, stats, transparent, fog_of_war_range, is_our);
                    entities.add(unit);
                }

            }

            mp = (int) data_map.get("mp");
            is_it_your_turn = (boolean) data_map.get("is_it_your_turn");

            // Build fog of war
            fog_of_war = new boolean[mapLoader.getHeight()][mapLoader.getWidth()];

            for (ClientEntity entity : entities) {
                if (entity instanceof ClientUnit u) {
                    if (u.is_our) {
                        for (int y = u.getY() - u.getFogOfWarRange(); y <= u.getY() + u.getFogOfWarRange(); y++) {
                            for (int x = u.getX() - u.getFogOfWarRange(); x <= u.getX() + u.getFogOfWarRange(); x++) {
                                if (x >= 0 && y >= 0 && x < fog_of_war.length && y < fog_of_war[0].length) {
                                    fog_of_war[y][x] = true;
                                }
                            }
                        }
                    }
                }
            }

            chat = (ArrayList<String>) data_map.get("chat");

            if (data_map.containsKey("atk_tracker")){
                ArrayList<Object> atk_tracker = (ArrayList<Object>) data_map.get("atk_tracker");
                for (Object atko: atk_tracker){
                    boolean isalr = false;
                    Integer[] atk = new Integer[3];
                    ((ArrayList<Integer>) atko).toArray(atk);

                    for (Integer[] atk2: coords_atk){
                        if (Objects.equals(atk2[0], atk[0]) && Objects.equals(atk2[1], atk[1])){
                            atk2[2] += 200;
                            isalr = true;
                        }
                    }

                    if (!isalr){
                        coords_atk.add(new Integer[]{atk[0], atk[1], 200});
                    }
                }
            }

            teamDoingTurnColor = new Color((int)data_map.get("team_doing_turn_color"));
            teamDoingTurnNick = ((String)data_map.get("team_doing_turn_nick"));
        } catch (JsonProcessingException e){
            lock.unlock();
            return new Expect<>(() -> "Failed to parse JSON");
        } catch (ClassCastException | NullPointerException e){
            lock.unlock();
            e.printStackTrace();
            return new Expect<>(() -> "Incorrect packet format");
        }

        lock.unlock();
        return new Expect<>("");
    }

    public Object[] buildView(int x, int y, float scale){
        try {
            lock.lock();
            Object[] fog_of_war_and_entities = new Object[3];
            boolean[][] fog_of_war_s = new boolean[(int)Math.ceil(3*scale)*2+1][(int)Math.ceil(7*scale)*2+1];
            ClientEntity[][] entities_s = new ClientEntity[(int)Math.ceil(3*scale)*2+1][(int)Math.ceil(7*scale)*2+1];
            boolean[][] sel_range = new boolean[(int)Math.ceil(3*scale)*2+1][(int)Math.ceil(7*scale)*2+1];

            // y + 3
            // x + 7

            int y_center = y + (int)Math.ceil(3*scale);
            int x_center = x + (int)Math.ceil(7*scale);

            for (ClientEntity entity : entities) {
                if (entity.getY() >= y_center - (int)Math.ceil(3*scale) - 1 && entity.getY() < y_center + (int)Math.ceil(3*scale)) {
                    if (entity.getX() >= x_center - (int)Math.ceil(7*scale) - 1 && entity.getX() < x_center + (int)Math.ceil(7*scale)) {
                        if (entity instanceof ClientUnit u) {
                            if (!u.isTransparent())
                                entities_s[entity.getY() - y + 1][entity.getX() - x + 1] = entity;
                        }
                    }
                }
            }

            for (ClientEntity entity : entities) {
                if (entity.getY() >= y_center - (int)Math.ceil(3*scale) - 1 && entity.getY() < y_center + (int)Math.ceil(3*scale)) {
                    if (entity.getX() >= x_center - (int)Math.ceil(7*scale) - 1 && entity.getX() < x_center + (int)Math.ceil(7*scale)) {
                        if (entities_s[entity.getY() - y + 1][entity.getX() - x + 1] == null)
                            entities_s[entity.getY() - y + 1][entity.getX() - x + 1] = entity;
                    }
                }
            }

            for (int _y = y_center - (int)Math.ceil(3*scale) - 1; _y <  y_center + (int)Math.ceil(3*scale); _y++) {
                for (int _x = x_center - (int)Math.ceil(7*scale) - 1 ; _x < x_center + (int)Math.ceil(7*scale); _x++) {
                    if (_x >= 0 && _y >= 0 && _y < mapLoader.getHeight() && _x < mapLoader.getWidth()) {
                        boolean s = fog_of_war[_y][_x];
                        fog_of_war_s[_y - y + 1][_x - x + 1] = s;
                    }
                }
            }

            if (selectedUnit != null){
                if (selectedUnit.getSelR() > 0){
                    for (int _y = y_center - (int)Math.ceil(3*scale) - 1; _y <  y_center + (int)Math.ceil(3*scale); _y++) {
                        for (int _x = x_center - (int)Math.ceil(7*scale) - 1 ; _x < x_center + (int)Math.ceil(7*scale); _x++) {
                            if (_x >= 0 && _y >= 0 && _y < mapLoader.getHeight() && _x < mapLoader.getWidth()) {
                                if (_x > selectedUnit.getX() || _y > selectedUnit.getY() ||
                                        _x < selectedUnit.getX() || _y < selectedUnit.getY()){
                                    if (_x <= selectedUnit.getX() + selectedUnit.getSelR() &&
                                        _x >= selectedUnit.getX() - selectedUnit.getSelR()){
                                        if (_y <= selectedUnit.getY() + selectedUnit.getSelR() &&
                                                _y >= selectedUnit.getY() - selectedUnit.getSelR()){
                                            int arny = _y - y + 1;
                                            int arnx = _x - x + 1;
                                            if (arnx >= 0 && arnx < 15 && arny >= 0 && arny < 7){
                                                sel_range[arny][arnx] = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            fog_of_war_and_entities[0] = entities_s;
            fog_of_war_and_entities[1] = fog_of_war_s;
            fog_of_war_and_entities[2] = sel_range;
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

    public ClientMapLoader getClMapLoader(){
        try {
            lock.lock();
            return mapLoader;
        } finally {
            lock.unlock();
        }
    }

    public Color getTeamDoingTurnColor() {
        try {
            lock.lock();
            return teamDoingTurnColor;
        } finally {
            lock.unlock();
        }
    }

    public String getTeamDoingTurnNick() {
        try {
            lock.lock();
            return teamDoingTurnNick;
        } finally {
            lock.unlock();
        }
    }

    public Image drawMinimap(int cam_x, int cam_y){ // +30 -30
        lock.lock();
        if (!mapLoader.isMapLoaded()){
            lock.unlock();
            return null;
        }

        BufferedImage minimap = new BufferedImage(mapLoader.getWidth(), mapLoader.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        BufferedImage minimapLimit = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB_PRE);

        ArrayList<ArrayList<String>> entire_map = mapLoader.getEntireMap();

        for (int y = 0; y < mapLoader.getHeight(); y++){
            for (int x = 0; x < mapLoader.getWidth(); x++){
                minimap.setRGB(x, y, AssetLoader.load(entire_map.get(y).get(x)).getRGB(15, 15));
            }
        }

        // Attacked units blink

        for (ClientEntity entity: entities){
            if (entity instanceof ClientUnit u){
                minimap.setRGB(u.getX(), u.getY(), u.getTeamColor().getRGB());
                if (blinkM) {
                    Integer[] coordrm = null;

                    for (Integer[] coord : coords_atk) {
                        if (coord[0] == u.getX() && coord[1] == u.getY()) {
                            minimap.setRGB(u.getX(), u.getY(), new Color(255, 255, 153).getRGB());
                            coord[2]--;
                            if (coord[2] == 0){
                                coordrm = coord;
                            }
                            break;
                        }
                    }

                    if (coordrm != null)
                        coords_atk.remove(coordrm);
                }
            }
        }

        BufferedImage empty = AssetLoader.load("empty.png");
        int ergb = empty.getRGB(0,0);

        for (int y = 0; y < mapLoader.getHeight(); y++){
            for (int x = 0; x < mapLoader.getWidth(); x++){
                if (!fog_of_war[y][x]) {
                    Color c = new Color(minimap.getRGB(x, y));
                    Color d = new Color((int) (c.getRed()*0.25), (int) (c.getGreen()*0.25), (int) (c.getBlue()*0.25));
                    minimap.setRGB(x, y, d.getRGB());
                }
            }
        }

        int y2 = 0;
        int x2 = 0;
        for (int y = cam_y-30; y < cam_y+30; y++){
            for (int x = cam_x-30; x < cam_x+30; x++){
                if (y < 0 || x < 0 || x >= minimap.getWidth() || y >= minimap.getHeight()){
                    minimapLimit.setRGB(x2, y2, Color.BLACK.getRGB());
                }
                else{
                    minimapLimit.setRGB(x2, y2, minimap.getRGB(x, y));
                }
                x2++;
            }
            x2 = 0;
            y2++;
        }

        // blink deblink
        blinkMT++;
        if (blinkMT == 20){
            blinkM = !blinkM;
            blinkMT = 0;
        }

        lock.unlock();
        return minimapLimit.getScaledInstance(160, 160, Image.SCALE_FAST);

    }



}
