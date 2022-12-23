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

package net.stalemate.client.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import net.libutils.error.Expect;
import net.stalemate.client.AssetLoader;
import net.stalemate.client.ClientMapLoader;
import net.stalemate.client.config.ButtonTooltips;
import net.stalemate.client.config.KeyboardBindMapper;
import net.stalemate.client.config.PropertiesMatcher;
import net.stalemate.client.property.ClientSideProperty;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class InGameUI extends JPanel {
    private final ClientDataRenderer renderer;
    private final KeyboardInput in_client;
    private final JDesktopPane p;
    @SuppressWarnings("FieldCanBeLocal") private final Font monogram;
    private final Font monogram_button;
    private boolean focus_desktop_pane = false;

    private final JFrame frame;
    private boolean do_offset = false;

    public JFrame getFrame(){return frame;}

    /***
     * Ids of buttons to be rendered
     */
    String[][] id_array = {{null, null, null},
                        {null, null, null},
                        {null, null, null}};


    public ClientDataRenderer getRenderer() {
        return renderer;
    }

    public KeyboardInput getInput() {
        return in_client;
    }

    public record PropertiesToRender(ArrayList<ClientSideProperty> properties){}

    private PropertiesToRender propertiesToRender = null;

    public final ReentrantLock unsafeLock = new ReentrantLock();

    private int offset_x = 0;
    private int offset_y = 0;
    private boolean dis_offset = false; // if true camera is locked

    enum OffsetDirection{
        None,
        Left,
        Right,
        Up,
        Down
    }

    private OffsetDirection offset_direction = OffsetDirection.None;

    ArrayList<ArrayList<BufferedImage>> map_to_render = new ArrayList<>();
    ArrayList<ArrayList<BufferedImage>> fog_of_war = new ArrayList<>();
    ArrayList<ArrayList<BufferedImage>> entity_render = new ArrayList<>();
    ArrayList<ArrayList<BufferedImage>> unit_data_ar = new ArrayList<>();
    ArrayList<BufferedImage> buttons = new ArrayList<>();
    ArrayList<BufferedImage> queue = null;
    ArrayList<String> binds = new ArrayList<>();
    ArrayList<String> unit_times = new ArrayList<>();
    ArrayList<String> chat = new ArrayList<>();
    BufferedImage unit_img = null;
    int mp = 0;
    boolean is_it_your_turn = false;

    BufferedImage placeholder_ui;
    BufferedImage placeholder_ui_2;
    BufferedImage panel;
    BufferedImage military_points;

    BufferedImage selector;

    final BufferedImage texture_missing;

    int sel_x_frame;
    int sel_y_frame;

    int x_prev = 0;
    int y_prev = 0;
    boolean do_render_prev = false;

    private EscapeMenu escapeMenu = null;

    public class KeyboardInput implements KeyListener {

        private final ConcurrentLinkedQueue<String> keysInQueue = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<String> chatMSGS = new ConcurrentLinkedQueue<>();
        private String currentMSG = "";
        private boolean isTypingChatMessage = false;
        private final ReentrantLock lock = new ReentrantLock();

        public String getCurrentMSG() {
            return currentMSG;
        }

        public boolean isTypingChatMessage() {
            return isTypingChatMessage;
        }

        public ConcurrentLinkedQueue<String> getChatMSGS() {
            return chatMSGS;
        }

        public synchronized ConcurrentLinkedQueue<String> getQueue() {
            lock.lock();
            try {
                return keysInQueue;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            lock.lock();

            if (!isTypingChatMessage) {
                if (e.getKeyCode() == KeyboardBindMapper.move_up) {
                    keysInQueue.add("UP");
                } else if (e.getKeyCode() == KeyboardBindMapper.move_down) {
                    keysInQueue.add("DOWN");
                } else if (e.getKeyCode() == KeyboardBindMapper.move_left) {
                    keysInQueue.add("LEFT");
                } else if (e.getKeyCode() == KeyboardBindMapper.move_right) {
                    keysInQueue.add("RIGHT");
                } else if (e.getKeyCode() == KeyboardBindMapper.confirm) {
                    keysInQueue.add("ENTER");
                } else if (e.getKeyCode() == KeyboardBindMapper.escape) {
                    keysInQueue.add("ESCAPE");
                } else if (e.getKeyCode() == KeyboardBindMapper.finish_turn) {
                    keysInQueue.add("SPACE");
                } else if ("qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM".contains(String.valueOf(e.getKeyChar()))) {
                    keysInQueue.add(String.valueOf(e.getKeyChar()));
                } else if (e.getKeyCode() == KeyboardBindMapper.lock_camera) {
                    unsafeLock.lock();
                    if (!dis_offset) {
                        dis_offset = true;
                        offset_direction = OffsetDirection.None;
                    }
                    else
                        dis_offset = false;
                    unsafeLock.unlock();
                } else if (e.getKeyCode() == KeyboardBindMapper.goto_first_built_base) {
                    keysInQueue.add("SHIFT");
                } else if (e.getKeyCode() == KeyboardBindMapper.chat) {
                    isTypingChatMessage = true;
                } else if (e.getKeyCode() == KeyEvent.VK_F1){
                    keysInQueue.add("TAB");
                }
                else if (e.getKeyCode() == KeyEvent.VK_F10 && !isTypingChatMessage) {
                    spawnEscapeMenu();
                }
            } else {
                if (" qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM<>=-()[]{}\"';:.,1234567890@#$%^&*/\\?".contains(String.valueOf(e.getKeyChar()))) {
                    currentMSG += String.valueOf(e.getKeyChar());
                } else if (e.getKeyCode() == KeyboardBindMapper.escape) {
                    currentMSG = "";
                    isTypingChatMessage = false;
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    isTypingChatMessage = false;
                    if (!currentMSG.isEmpty())
                        chatMSGS.add(currentMSG);
                    currentMSG = "";
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    if (currentMSG.length() - 1 >= 0)
                        currentMSG = currentMSG.substring(0, currentMSG.length() - 1);
                }
            }

            lock.unlock();
        }

        @Override
        public void keyReleased(KeyEvent e) {

        }

        // spaghetti code gaming
        public int getOffsetX(){
            unsafeLock.lock();
            try {
                return offset_x;
            }
            finally {unsafeLock.unlock();}
        }

        public int getOffsetY(){
            unsafeLock.lock();
            try {
                return offset_y;
            }finally {unsafeLock.unlock();}
        }
    }

    public static class ClientDataRenderer {
        private final InGameUI interface_;
        private final BufferedImage fog_of_war;
        private final HashMap<String, BufferedImage> loaded_images = new HashMap<>();
        private final ClientMapLoader mapLoader = new ClientMapLoader();
        private final BufferedImage skull;
        private final BufferedImage shovel;

        static class CachedBufferedImage{
            public BufferedImage image;

            public CachedBufferedImage(BufferedImage i){
                image = i;
            }
        }

        public ClientMapLoader getMapLoader(){
            return mapLoader;
        }

        private final HashMap<Integer, CachedBufferedImage> cachedUnitDataArImgs = new HashMap<>();

        /*
        {
            "type" : "RenderRequest",

            "x" : 0,
            "y" : 0,

            "map_path" : path,

            "sel_x" : 0,
            "sel_y" : 0,

            "map_textures" : [ ... ] <- 2D array of MapObject's textures 12x5
            "fog_of_war" : [ ... ] <- 2D array of integers 1/0 to signalise whether there is fog of war or isn't 12x5
            "entity_render" : [ ... ] <- 2D array of Entity textures 12x5
            "unit_data_ar" : [ ... ] UnitData Example: {"team_rgb" : [0,0,0], "stats" : [0 HP, 0 MAX_HP, 0 SU, 0 MAX_SU]}, "flip" : false}
            "minimap_data" : {
                "fog_fo_war" : [[]], 1/0 2d array
                "units" : [[[0, 0, 0]]], Unit's team rgb
                "tiles" : [[true, true]], Whether tile is passable or not
            }

            "selected_unit_data" : { <- If 0 it means that there is no unit selected
                   "buttons" : [ 0, 0, 0, {"id" : "id_here" , "texture" : "texture_here", "mode" : 1 -> StandardButton, "bind" : "A"}, ... ,
                                                                                                       2 -> SelectorButton
                   "queue" : [0, {"texture" : "", "turn_time" : 0}],

                   NOTE: If buttons are -1 it means unit doesn't have buttons
                   "properties" : <Properties>
                   "texture" : "texture_here.png"

                   "iselectorbutton_press" : true,
                   "iselectorbutton_data_selector_texture" : "texture_here",

                   NOTE: -1 means hidden statistic
            }

            "chat" : []
        }


         */

        public ClientDataRenderer(InGameUI interface_) {
            fog_of_war = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics2D = fog_of_war.createGraphics();
            graphics2D.setColor(new Color(0,0,0, 0.5F));
            graphics2D.fillRect(0, 0, 32, 32);
            graphics2D.dispose();
            this.interface_ = interface_;

            shovel = AssetLoader.load("assets/shovel.png");
            skull = AssetLoader.load("assets/skull.png");
        }

        @SuppressWarnings("unchecked")
        public synchronized Expect<String, ?> change_render_data(String json, boolean[] resetOffsets) {
            try {
                this.interface_.unsafeLock.lock();
                PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                        .build();
                ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
                Map<String, Object> data_map = objectMapper.readValue(json, Map.class);

                Expect<String, ?> m_ = mapLoader.load((String) data_map.get("map_path"));
                if (m_.isNone()){
                    return m_;
                }

                ArrayList<ArrayList<String>> map_textures = mapLoader.getMap((Integer) data_map.get("x"), (Integer)data_map.get("y"));
                ArrayList<ArrayList<String>> entity_render = (ArrayList<ArrayList<String>>) data_map.get("entity_render");
                ArrayList<ArrayList<Integer>> fog_of_war = (ArrayList<ArrayList<Integer>>) data_map.get("fog_of_war");
                ArrayList<ArrayList<HashMap<String, Object>>> unit_data_ar_ = (ArrayList<ArrayList<HashMap<String, Object>>>) data_map.get("unit_data_ar");
                ArrayList<String> chat = (ArrayList<String>) data_map.get("chat");

                HashMap<String, Object> selected_unit;
                ArrayList<BufferedImage> buttons = new ArrayList<>();
                ArrayList<BufferedImage> unit_queue = null;
                ArrayList<String> binds = new ArrayList<>();
                ArrayList<String> unit_queue_turn_time = new ArrayList<>();
                BufferedImage selected_unit_image = null;
                PropertiesToRender propertiesToRender = null;
                BufferedImage selector = null;

                String[][] button_ids ={{null, null, null},{null, null, null},{null, null, null}};
                int mp;

                mp = (int) data_map.get("mp");
                boolean is_it_your_turn = (boolean) data_map.get("is_it_your_turn");
                if (!(data_map.get("selected_unit_data") instanceof Integer)) {
                    selected_unit = (HashMap<String, Object>) data_map.get("selected_unit_data");

                    // Create PropertiesToRender
                    ArrayList<ArrayList<String>> unready_properties = (ArrayList<ArrayList<String>>) selected_unit.get("properties");
                    ArrayList<ClientSideProperty> clientSideProperties = new ArrayList<>();
                    for (ArrayList<String> p : unready_properties){
                        clientSideProperties.add(new ClientSideProperty(p.get(0), p.get(1)));
                    }
                    propertiesToRender = new PropertiesToRender(clientSideProperties);

                    // Add "big unit texture"
                    boolean texture_missing = false;
                    if (!loaded_images.containsKey((String)(selected_unit.get("texture")))) {
                        if (AssetLoader.load((String) selected_unit.get("texture")) == null){
                            texture_missing = true;
                        }
                        else {
                            loaded_images.put((String) selected_unit.get("texture"), AssetLoader.load((String) selected_unit.get("texture")));
                        }
                    }
                    if (!texture_missing){ selected_unit_image = loaded_images.get((String)(selected_unit.get("texture")));}

                    // Get the buttons
                    ArrayList<Object> buttons_ = (ArrayList<Object>) selected_unit.get("buttons");

                    // Add textures
                    int y = 0;
                    int x = 0;
                    for (Object button : buttons_) {

                        if (!(button instanceof Integer)) {
                            HashMap<String, Object> b = (HashMap<String, Object>) button;

                            // Add textures to ArrayList
                            String texture = (String) b.get("texture");
                            if (!loaded_images.containsKey(texture)) {
                                if (AssetLoader.load(texture) == null) {
                                    buttons.add(this.interface_.texture_missing);
                                    binds.add((String) b.get("bind"));
                                    button_ids[y][x] = (String) b.get("id");
                                    x++;
                                    if (x == 3){
                                        y += 1;
                                        x = 0;
                                    }
                                    continue;
                                } else{
                                    loaded_images.put(texture, AssetLoader.load(texture));
                                }
                            }
                            buttons.add(loaded_images.get(texture));
                            binds.add(((String) b.get("bind")).toUpperCase());
                            button_ids[y][x] = (String) b.get("id");
                        } else {
                            buttons.add(null);
                            binds.add(null);
                        }
                        x++;
                        if (x == 3){
                            y += 1;
                            x = 0;
                        }
                    }

                    // Create queue
                    if (!(selected_unit.get("queue") instanceof Integer)) {
                        unit_queue = new ArrayList<>();
                        ArrayList<Object> queue_ = (ArrayList<Object>) selected_unit.get("queue");

                        for (Object member : queue_) {
                            if (!(member instanceof Integer)) {
                                HashMap<String, Object> m = (HashMap<String, Object>) member;

                                // Add textures to ArrayList
                                String texture = (String) m.get("texture");
                                if (!loaded_images.containsKey(texture)) {
                                    if (AssetLoader.load(texture) == null) {
                                        unit_queue.add(this.interface_.texture_missing);
                                        continue;
                                    }
                                    else {
                                        loaded_images.put(texture, AssetLoader.load(texture));
                                    }
                                }
                                unit_queue.add(loaded_images.get(texture));
                                unit_queue_turn_time.add(((Integer) m.get("turn_time")).toString());
                            } else {
                                unit_queue.add(null);
                                unit_queue_turn_time.add(" ");
                            }
                        }
                    }

                    // Selector texture thingy

                    if (((boolean) selected_unit.get("iselectorbutton_press"))){
                        // iselectorbutton_data_texture
                        if (!loaded_images.containsKey((String)selected_unit.get("iselectorbutton_data_selector_texture"))) {
                            if (AssetLoader.load((String) selected_unit.get("iselectorbutton_data_selector_texture")) == null){
                                loaded_images.put((String) selected_unit.get("iselectorbutton_data_selector_texture"), this.interface_.texture_missing);
                            }
                            else {
                                loaded_images.put((String) selected_unit.get("iselectorbutton_data_selector_texture"), AssetLoader.load((String) selected_unit.get("iselectorbutton_data_selector_texture")));
                            }
                        }

                        selector = loaded_images.get((String)selected_unit.get("iselectorbutton_data_selector_texture"));
                    }
                }

                if (selector == null){
                    if (!loaded_images.containsKey("assets/ui/selectors/ui_select.png")) {
                        if (AssetLoader.load("assets/ui/selectors/ui_select.png") != null) {
                            loaded_images.put("assets/ui/selectors/ui_select.png", AssetLoader.load("assets/ui/selectors/ui_select.png"));
                        } else {
                            loaded_images.put("assets/ui/selectors/ui_select.png", this.interface_.texture_missing);
                        }
                    }

                    selector = loaded_images.get("assets/ui/selectors/ui_select.png");
                }

                ArrayList<ArrayList<BufferedImage>> map = new ArrayList<>();
                int y = 0;
                for (ArrayList<String> x_row : map_textures) {
                    map.add(new ArrayList<>());
                    for (String texture : x_row) {
                        if (texture == null) {
                            map.get(y).add(null);
                        }
                        else {
                            if (!loaded_images.containsKey(texture)) {
                                if (AssetLoader.load(texture) == null){
                                    map.get(y).add(this.interface_.texture_missing);
                                    continue;
                                }
                                loaded_images.put(texture, AssetLoader.load(texture));
                            }
                            map.get(y).add(loaded_images.get(texture));
                        }
                    }
                    y++;
                }

                ArrayList<ArrayList<BufferedImage>> entities = new ArrayList<>();
                y = 0;
                int x__ = 0;
                for (ArrayList<String> x_row : entity_render) {
                    entities.add(new ArrayList<>());
                    for (String texture : x_row) {
                        try {
                            if (texture == null) {
                                entities.get(y).add(null);
                            } else {
                                if (!loaded_images.containsKey(texture)) {
                                    if (AssetLoader.load(texture) == null){
                                        x__++;
                                        continue;
                                    }
                                    loaded_images.put(texture, AssetLoader.load(texture));
                                }

                                if (unit_data_ar_.get(y).get(x__) != null){
                                    HashMap<String, Object> unit_data = unit_data_ar_.get(y).get(x__);
                                    BufferedImage image;
                                    if ((boolean) (unit_data.get("flip"))){
                                        // Flip the image
                                        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
                                        tx.translate(-loaded_images.get(texture).getWidth(), 0);
                                        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                                        image = op.filter(loaded_images.get(texture), null);
                                    }
                                    else{
                                        image = loaded_images.get(texture);
                                    }
                                    if ((boolean)(unit_data.get("transparent"))){
                                        BufferedImage clone = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB_PRE);
                                        Graphics2D graphics = clone.createGraphics();

                                        graphics.setBackground(new Color(0x00FFFFFF, true));
                                        graphics.clearRect(0, 0, clone.getWidth(), clone.getHeight());

                                        graphics.drawImage(image,0,0,null);

                                        graphics.dispose();

                                        for (int y_ = 0; y_ < 32; y_++){
                                            for (int x_ = 0; x_ < 32; x_++) {

                                                Color original = new Color((new Color(clone.getRGB(x_, y_))).getRed(),
                                                        (new Color(clone.getRGB(x_, y_))).getGreen(),
                                                        (new Color(clone.getRGB(x_, y_))).getBlue(),
                                                        (new Color(clone.getRGB(x_, y_), true)).getAlpha());

                                                clone.setRGB(x_, y_, (new Color(original.getRed(),
                                                        original.getGreen(),
                                                        original.getBlue(),
                                                        (int) (original.getAlpha() * 0.5)).getRGB()));

                                            }
                                        }

                                        image = clone;
                                    }
                                    entities.get(y).add(image);
                                } else{
                                    entities.get(y).add(loaded_images.get(texture));
                                }
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                            entities.get(y).add(this.interface_.texture_missing);
                        }
                        x__++;
                    }
                    x__ = 0;
                    y++;
                }

                ArrayList<ArrayList<BufferedImage>> fog_of_war_ = new ArrayList<>();
                y = 0;
                for (ArrayList<Integer> x_row : fog_of_war) {
                    fog_of_war_.add(new ArrayList<>());
                    for (int texture : x_row) {
                        if (texture == 1) {
                            fog_of_war_.get(y).add(this.fog_of_war);
                        } else {
                            fog_of_war_.get(y).add(null);
                        }
                    }
                    y++;
                }

                // Create those team showing thingies
                ArrayList<ArrayList<BufferedImage>> unit_data_ar = new ArrayList<>();
                y = 0;
                for (ArrayList<HashMap<String, Object>> row : unit_data_ar_){
                    unit_data_ar.add(new ArrayList<>());
                    for (HashMap<String, Object> unit_data: row){

                        if (unit_data != null) {
                            // calculate hash
                            ArrayList<Integer> rgb_team = (ArrayList<Integer>) unit_data.get("rgb");
                            ArrayList<Integer> stats = (ArrayList<Integer>) unit_data.get("stats");
                            boolean has_unit_su_enabled = stats.get(2) != -1 && stats.get(3) != -1 && stats.get(2) != 0 && stats.get(3) != 0;
                            Object[] hash_ar = new Object[]{new Color(rgb_team.get(0), rgb_team.get(1), rgb_team.get(2)),
                                    ((float)stats.get(0))/((float)stats.get(1)),
                                    (has_unit_su_enabled) ? ((float)stats.get(2))/((float)stats.get(3)): -1,
                                    0};

                            // takes indicators into account
                            if (stats.get(2) < stats.get(3) * 0.4 && has_unit_su_enabled){
                                hash_ar[3] = -1;
                            }
                            else if (stats.get(4) > 0 && !(stats.get(2) < stats.get(3) * 0.4 && has_unit_su_enabled)){
                                hash_ar[3] = stats.get(4);
                            }

                            if (!cachedUnitDataArImgs.containsKey(Arrays.hashCode(hash_ar))) {
                                Color color = new Color(rgb_team.get(0), rgb_team.get(1), rgb_team.get(2));

                                BufferedImage clone = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB_PRE);
                                Graphics2D graphics = clone.createGraphics();

                                graphics.setBackground(new Color(0x00FFFFFF, true));
                                graphics.clearRect(0, 0, clone.getWidth(), clone.getHeight());

                                graphics.setColor(color);

                                graphics.drawLine(23, 0, 31, 0);
                                graphics.drawLine(31, 0, 31, 8);

                                graphics.drawLine(0, 31, 0, 23);
                                graphics.drawLine(8, 31, 0, 31);

                                graphics.setColor(new Color(148, 0, 21));

                                int hp = stats.get(0);
                                int max_hp = stats.get(1);

                                if (hp != -1 && max_hp != -1 && hp != 0 && max_hp != 0) {
                                    graphics.drawLine(22, 30, 22 + (int) (9f * ((float) (hp) / (float) (max_hp))), 30);
                                }

                                graphics.setColor(Color.YELLOW);
                                int su = stats.get(2);
                                int max_su = stats.get(3);

                                if (has_unit_su_enabled) {
                                    graphics.drawLine(22, 31, 22 + (int) (9f * ((float) (su) / (float) (max_su))), 31);
                                }

                                graphics.dispose();

                                int et = stats.get(4);
                                // draw shovel
                                BufferedImage shovel_col = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB_PRE);
                                if (et > 0) {
                                    Graphics2D shovel_graphics = shovel_col.createGraphics();
                                    graphics.setBackground(new Color(0x00FFFFFF, true));
                                    shovel_graphics.drawImage(shovel, 0, 0, null);

                                    for (int y_ = 0; y_ < 16; y_++) {
                                        for (int x_ = 0; x_ < 16; x_++) {
                                            if (shovel_col.getRGB(x_, y_) == Color.BLACK.getRGB()) {
                                                shovel_col.setRGB(x_, y_, et == 1 ? Color.RED.getRGB() :
                                                        et == 2 ? Color.YELLOW.getRGB() : et == 3 ? Color.GREEN.getRGB() : 0)
                                                ;
                                            }
                                        }
                                    }
                                    shovel_graphics.dispose();
                                }

                                // draw the shovel
                                Image scaled = clone.getScaledInstance(64, 64, Image.SCALE_FAST);
                                clone = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB_PRE);
                                graphics = clone.createGraphics();
                                graphics.setBackground(new Color(0x00FFFFFF, true));
                                graphics.drawImage(scaled, 0, 0, null);
                                // render the skull to indicate that unit is under supplied
                                if (su < max_su * 0.4 && has_unit_su_enabled) {
                                    BufferedImage skull_col = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB_PRE);
                                    Graphics2D skull_graphics = skull_col.createGraphics();
                                    skull_graphics.setBackground(new Color(0x00FFFFFF, true));
                                    skull_graphics.drawImage(skull, 0, 0, null);

                                    for (int y_ = 0; y_ < 16; y_++) {
                                        for (int x_ = 0; x_ < 16; x_++) {
                                            if (skull_col.getRGB(x_, y_) == Color.BLACK.getRGB()) {
                                                skull_col.setRGB(x_, y_, Color.RED.getRGB());
                                            }
                                        }
                                    }
                                    skull_graphics.dispose();
                                    graphics.drawImage(skull_col, 0, 0, null);
                                }

                                if (et > 0 && !(su < max_su * 0.4 && has_unit_su_enabled))
                                    graphics.drawImage(shovel_col, 0, 0, null);
                                graphics.dispose();

                                unit_data_ar.get(y).add(clone);
                                cachedUnitDataArImgs.put(Arrays.hashCode(hash_ar), new CachedBufferedImage(clone));
                            }
                            else
                            unit_data_ar.get(y).add(cachedUnitDataArImgs.get(Arrays.hashCode(hash_ar)).image);
                        }
                        else {
                            unit_data_ar.get(y).add(null);
                        }
                    }
                    y++;
                }

                int sel_x = (int) data_map.get("sel_x");
                int sel_y = (int) data_map.get("sel_y");

                int sel_x_frame = (sel_x - ((int)data_map.get("x")+6))*64 + 6*64;
                int sel_y_frame = (sel_y - ((int)data_map.get("y")+2))*64 + 2*64;

                this.interface_.unsafeLock.lock();
                this.interface_.map_to_render = map;
                this.interface_.fog_of_war = fog_of_war_;
                this.interface_.entity_render = entities;
                this.interface_.buttons = buttons;
                this.interface_.binds = binds;
                this.interface_.unit_img = selected_unit_image;
                this.interface_.propertiesToRender = propertiesToRender;
                this.interface_.selector = selector;
                this.interface_.unit_data_ar = unit_data_ar;
                this.interface_.queue = unit_queue;
                this.interface_.unit_times = unit_queue_turn_time;
                this.interface_.mp = mp;
                this.interface_.is_it_your_turn = is_it_your_turn;
                this.interface_.id_array = button_ids;
                this.interface_.sel_x_frame = sel_x_frame;
                this.interface_.sel_y_frame = sel_y_frame;
                this.interface_.chat = chat;
                this.interface_.unsafeLock.unlock();

                if (resetOffsets[0]){
                    this.interface_.offset_x = 0;
                }
                if (resetOffsets[1]){
                    this.interface_.offset_y = 0;
                }

                if (cachedUnitDataArImgs.size() > 100){
                    cachedUnitDataArImgs.clear();
                }

                interface_.unsafeLock.unlock();
                return new Expect<>("");
            } catch (Exception e) {
                e.printStackTrace();
            }

            interface_.unsafeLock.unlock();
            return new Expect<>(() -> "Incorrect packet format");
        }

    }

    class MListener extends MouseAdapter{
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!focus_desktop_pane) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    int y = 0;
                    int x = 0;
                    for (String bind : binds) {
                        if (e.getX() >= (10 * 64) + (x * 64) && e.getY() >= (6 * 64) + (y * 64)
                                && e.getX() <= (11 * 64) + (x * 64) && e.getY() <= (7 * 64) + (y * 64)) {
                            if (bind != null) {
                                in_client.keysInQueue.add(bind);
                            }
                        }
                        x++;
                        if (x == 3) {
                            x = 0;
                            y++;
                        }
                    }

                    // Move cam
                    if (offset_direction == OffsetDirection.None) {
                        if ((e.getX() >= 0 && e.getX() <= 832) &&
                                (e.getY() >= 64 && e.getY() <= 384)) {
                            int x_diff = (sel_x_frame - offset_x) - e.getX();
                            int y_diff = (sel_y_frame + 64 - offset_y) - e.getY();

                            int right_mv = (int) Math.ceil((float) x_diff / 64);
                            int down_mv = (int) Math.ceil((float) y_diff / 64);

                            for (int m1 = 0; m1 < Math.abs(right_mv); m1++) {
                                if (right_mv > 0) {
                                    in_client.keysInQueue.add("LEFT");
                                } else if (right_mv < 0) {
                                    in_client.keysInQueue.add("RIGHT");
                                }
                            }

                            for (int m2 = 0; m2 < Math.abs(down_mv); m2++) {
                                if (down_mv > 0) {
                                    in_client.keysInQueue.add("UP");
                                } else if (down_mv < 0) {
                                    in_client.keysInQueue.add("DOWN");
                                }
                            }
                        }
                    }
                }
                else if (e.getButton() == MouseEvent.BUTTON2) {
                    in_client.keysInQueue.add("ESCAPE");
                } else {
                    in_client.keysInQueue.add("ENTER");
                }
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            unsafeLock.lock();
            if (!focus_desktop_pane) {
                boolean clearTooltip = false;
                int y = 0;
                for (String[] row : id_array) {
                    int x = 0;
                    for (String point : row) {
                        if (e.getX() >= (10 * 64) + (x * 64) && e.getY() >= (6 * 64) + (y * 64)
                                && e.getX() <= (11 * 64) + (x * 64) && e.getY() <= (7 * 64) + (y * 64)) {
                            if (ButtonTooltips.getTooltip(point) != null)
                                InGameUI.this.setToolTipText("<html><font face=\"monogramextended\" size=5>" + ButtonTooltips.getTooltip(point) + "</font></html>");
                            else
                                InGameUI.this.setToolTipText(null);
                            clearTooltip = true;
                        }
                        x++;
                    }
                    y++;
                }

                // move offset rightwards
                if (!dis_offset) {
                    if (e.getX() >= 12 * 64 && e.getY() >= 64 && e.getY() <= 6 * 64) {
                        offset_direction = OffsetDirection.Right;
                    } else if (e.getX() <= 64 && e.getY() >= 64 && e.getY() <= 6 * 64) {
                        offset_direction = OffsetDirection.Left;
                    } else if (e.getX() <= 12 * 64 && e.getY() >= 5 * 64 && e.getY() <= 6 * 64) {
                        offset_direction = OffsetDirection.Down;
                    } else if (e.getX() <= 12 * 64 && e.getY() >= 64 && e.getY() <= 128) {
                        offset_direction = OffsetDirection.Up;
                    } else {
                        offset_direction = OffsetDirection.None;
                    }
                }
                else {
                    offset_direction = OffsetDirection.None;
                }

                if (offset_direction == OffsetDirection.None)
                if ((((64 < e.getY()) && (e.getY() < 6 * 64)) && ((0 < e.getX()) && (e.getX() < 13 * 64))) || (((10 * 64 < e.getX()) && (e.getX() < 13 * 64)) && ((6 * 64 < e.getY()) && (e.getY() < 9 * 64)))) {
                    if (((64 < e.getY()) && (e.getY() < 6 * 64)) && ((0 < e.getX()) && (e.getX() < 13 * 64))) {

                        int x_selector = 448;
                        int y_selector = 256;

                        int x_diff = e.getX() - (x_selector - offset_x);
                        int y_diff = e.getY() - (y_selector - offset_y);

                        InGameUI.this.x_prev = x_selector + (int)(Math.floor((float)x_diff/64))*64 - offset_x;
                        InGameUI.this.y_prev = y_selector + (int)(Math.floor((float)y_diff/64))*64 - offset_y;

                        InGameUI.this.do_render_prev = true;
                        InGameUI.this.do_offset = true;
                    }
                    else {
                        InGameUI.this.x_prev = ((e.getX()) / (64));
                        InGameUI.this.y_prev = ((e.getY()) / (64));
                        InGameUI.this.do_render_prev = true;
                        InGameUI.this.do_offset = false;
                    }
                } else {
                    InGameUI.this.do_render_prev = false;
                }
                else
                    do_render_prev = false;

                if (!clearTooltip) {
                    InGameUI.this.setToolTipText(null);
                }
            }
            unsafeLock.unlock();
        }
    }

    public InGameUI(){
        super(null);
        ButtonTooltips.init();
        PropertiesMatcher.loadPropertyMatcher();
        renderer = new ClientDataRenderer(this);

        frame = new JFrame("Stalemate");
        frame.setMinimumSize(new Dimension(832+14,576+32+6));
        frame.setSize(new Dimension(832+32,576+32+6));
        MListener m = new MListener();

        this.addMouseMotionListener(m);
        this.addMouseListener(m);

        p = new JDesktopPane();
        p.setBackground(new Color(0x00FFFFFF, true));
        p.setVisible(true);
        p.setSize(new Dimension(832+14,576));
        p.setPreferredSize(new Dimension(832+14,576));
        this.add(p);

        this.setMinimumSize(new Dimension(832+14,576+32+6));
        this.setSize(new Dimension(832+32,576+32+6));

        frame.setResizable(false);
        frame.add(this);
        frame.setVisible(true);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        in_client = new KeyboardInput();
        frame.addKeyListener(in_client);

        texture_missing = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D graphics2D = texture_missing.createGraphics();
        graphics2D.setColor(Color.magenta);
        graphics2D.fillRect(0, 0, 32, 32);
        graphics2D.setColor(Color.BLACK);
        graphics2D.fillRect(0, 0, 16, 16);
        graphics2D.fillRect(16, 16, 16, 16);
        graphics2D.dispose();

        BufferedImage empty = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB_PRE);
        graphics2D = empty.createGraphics();
        graphics2D.setColor(Color.BLACK);
        graphics2D.fillRect(0, 0, 32, 32);
        graphics2D.dispose();
        renderer.loaded_images.put("empty.png", empty);

        monogram = AssetLoader.getMonogram();
        monogram_button = monogram.deriveFont(((float)(16)));

        UIManager.put("ToolTip.background", Color.BLACK);
        UIManager.put("ToolTip.foreground", Color.WHITE);

        placeholder_ui = AssetLoader.load("assets/placeholder_ui.png") != null ? AssetLoader.load("assets/placeholder_ui.png") : texture_missing;
        placeholder_ui_2 = AssetLoader.load("assets/placeholder_ui_2.png") != null ? AssetLoader.load("assets/placeholder_ui_2.png") : texture_missing;
        panel = AssetLoader.load("assets/panel.png") != null ? AssetLoader.load("assets/panel.png") : texture_missing;
        selector = AssetLoader.load("assets/ui/selectors/ui_select.png") != null ? AssetLoader.load("assets/ui/selectors/ui_select.png") : texture_missing;
        military_points = AssetLoader.load("assets/mp.png") != null ? AssetLoader.load("assets/mp.png") : texture_missing;

        frame.setIconImage(AssetLoader.load("assets/ui/selectors/ui_attack.png"));
    }

    private volatile boolean spawnEscapeMenu = false;

    public void spawnEscapeMenu(){
        unsafeLock.lock();
        spawnEscapeMenu = true;
        unsafeLock.unlock();
    }

    private volatile boolean termicon = false;
    public volatile boolean showResults = false;
    public ResultMenu resultMenu = null;

    public void setResults(String res){
        unsafeLock.lock();
        showResults = true;
        resultMenu = new ResultMenu(res);
        unsafeLock.unlock();
    }

    public boolean isTermicon(){
        return termicon;
    }

    public void inGameUIUpdate(){
        unsafeLock.lock();
        if (showResults){
            spawnEscapeMenu = false;
            if (escapeMenu != null){
                p.remove(escapeMenu);
                escapeMenu = null;
                focus_desktop_pane = false;
                frame.addKeyListener(in_client);
                frame.requestFocus();
            }

            resultMenu.setLocation((frame.getWidth()-resultMenu.getWidth())/2, (frame.getHeight()-resultMenu.getHeight())/2);
            p.add(resultMenu);
            focus_desktop_pane = true;
            showResults = false;
            frame.removeKeyListener(in_client);
        }

        if (resultMenu != null){
            if (resultMenu.status == 1){
                termicon = true;
            }
        }

        if (spawnEscapeMenu){
            escapeMenu = new EscapeMenu();
            escapeMenu.setLocation((frame.getWidth()-escapeMenu.getWidth())/2, (frame.getHeight()-escapeMenu.getHeight())/2);
            p.add(escapeMenu);
            focus_desktop_pane = true;
            spawnEscapeMenu = false;
            frame.removeKeyListener(in_client);
        }

        if (escapeMenu != null){
            if (escapeMenu.getStatus() == 3){
                p.remove(escapeMenu);
                escapeMenu = null;
                focus_desktop_pane = false;
                frame.addKeyListener(in_client);
                frame.requestFocus();
            }
        }
        if (escapeMenu != null){
            if (escapeMenu.getStatus() == 1)
            termicon = true;
        }

        if (offset_direction == OffsetDirection.Left){
            if (offset_x >= -64) {
                offset_x -= 4*2;
            }
        }
        else if (offset_direction == OffsetDirection.Right){
            if (offset_x <= 64) {
                offset_x += 4*2;
            }
        }

        if (offset_x > 64){
            offset_x = 64;
        }
        if (offset_x < -64){
            offset_x = -64;
        }


        if (offset_direction == OffsetDirection.Up){
            if (offset_y >= -64) {
                offset_y -= 4*2;
            }
        }
        if (offset_direction == OffsetDirection.Down){
            if (offset_y <= 64) {
                offset_y += 4*2;
            }
        }

        if (offset_y > 64){
            offset_y = 64;
        }
        if (offset_y < -64){
            offset_y = -64;
        }

        if (offset_direction != OffsetDirection.None){
            do_render_prev = false;
        }

        unsafeLock.unlock();
    }

    public void paint(Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        g.clearRect(0, 0, 900, 900);
        unsafeLock.lock();
        try {
            g.drawImage(placeholder_ui, 0, 384, null);
            g.drawImage(placeholder_ui_2, 0, 0, null);
            g.drawImage(panel, 640, 384, null);
            if (queue != null)
            g.drawImage(panel, 0, 384, null);

            BufferedImage bufferedImage = new BufferedImage(13*64, 5*64, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g2 = bufferedImage.createGraphics();

            int y = 0;
            for (ArrayList<BufferedImage> row_x: map_to_render){
                int x_count = 0;
                for (BufferedImage x : row_x){
                    g2.drawImage(x != null ? x.getScaledInstance(64, 64, Image.SCALE_FAST) : Objects.requireNonNull(AssetLoader.load("empty.png")).getScaledInstance(64, 64, Image.SCALE_FAST), 64*(x_count-1)-offset_x, 64*(y-1)-offset_y, null);
                    x_count++;
                }
                y++;
            }

            renderImagesScaled(entity_render, offset_x, offset_y, g2);
            renderImagesScaled(fog_of_war, offset_x, offset_y, g2);
            renderImages(unit_data_ar, offset_x, offset_y, g2);

            if (selector != null && do_render_prev && do_offset){
                g2.drawImage(selector.getScaledInstance(64, 64, Image.SCALE_FAST), x_prev, y_prev-64,null);
            }

            if (selector != null)
                g2.drawImage(selector.getScaledInstance(64, 64, Image.SCALE_FAST), sel_x_frame-offset_x,sel_y_frame-offset_y, null);

            g2.dispose();
            g.drawImage(bufferedImage, 0, 64, null);

            // Render the buttons
            int i = 0;
            int x = 0;
            y = 0;
            for (BufferedImage button : buttons){
                if (button != null){
                    g.drawImage(button.getScaledInstance(64, 64, Image.SCALE_FAST), 640+x, 384+y, null);
                }
                i++;
                x+=64;
                if (i == 3){
                    i = 0;
                    x = 0;
                    y += 64;
                }
            }

            // Render the queue
            if (queue != null) {
                i = 0;
                x = 0;
                y = 0;
                for (BufferedImage m : queue) {
                    if (m != null) {
                        g.drawImage(m.getScaledInstance(64, 64, Image.SCALE_FAST), x, 384 + y, null);
                    }
                    i++;
                    x += 64;
                    if (i == 3) {
                        i = 0;
                        x = 0;
                        y += 64;
                    }
                }
            }

            // Render the binds
            g.setColor(Color.BLACK);
            g.setFont(monogram_button);

            i = 0;
            x = 0;
            y = 0;
            for (String bind : binds){
                if (bind != null){
                    // g.drawImage(bind.getScaledInstance(64, 64, Image.SCALE_FAST), 640+x, 384+y, null);
                    g.drawString(bind, 640+x+8, 383+y+11);
                }
                i++;
                x+=64;
                if (i == 3){
                    i = 0;
                    x = 0;
                    y += 64;
                }
            }

            // Render the time of production
            g.setColor(Color.BLACK);
            g.setFont(monogram_button);

            i = 0;
            x = 0;
            y = 0;
            for (String bind : unit_times){
                if (bind != null){
                    g.drawString(bind, x+8, 383+y+11);
                }
                i++;
                x+=64;
                if (i == 3){
                    i = 0;
                    x = 0;
                    y += 64;
                }
            }

            // Render the unit
            if (unit_img != null) g.drawImage(unit_img.getScaledInstance(128, 128, Image.SCALE_FAST), 192+32, 384+32, null);

            // Render the stats
            if (propertiesToRender != null && monogram != null){
                // Find name of a unit
                ClientSideProperty name = null;
                for (ClientSideProperty property: propertiesToRender.properties){
                    if (property.key().equals("name")){
                        name = property;
                    }
                }
                @SuppressWarnings("unchecked") ArrayList<ClientSideProperty> properties = (ArrayList<ClientSideProperty>) propertiesToRender.properties.clone();
                properties.remove(name);
                PropertiesToRender propertiesToRender2 = new PropertiesToRender(properties);

                g.setColor(Color.BLACK);
                g.setFont(monogram.deriveFont((float)(32)).deriveFont(Font.BOLD));

                // Get font char size
                FontMetrics metrics = g.getFontMetrics(monogram.deriveFont((float)(27)).deriveFont(Font.BOLD));
                int width = metrics.stringWidth("A");

                if (name == null){
                    name = new ClientSideProperty("name", "");
                }

                int h = ((224 - (name.value().length() * width))/2);

                g.drawString(name.value(),192+32+64+128+h, 384+30); // 416-640


                g.setFont(monogram.deriveFont(Font.PLAIN, 23f));

                int y__ = 1;
                for (ClientSideProperty clientSideProperty: propertiesToRender2.properties){
                    if (PropertiesMatcher.matchKeyToString(clientSideProperty.key()) != null){
                        if (!Objects.equals("true", clientSideProperty.value()))
                        g.drawString(PropertiesMatcher.matchKeyToString(clientSideProperty.key()) + ": " + clientSideProperty.value(), 192+32+64+128, 384+43+13*y__);
                        else
                            g.drawString("(" + PropertiesMatcher.matchKeyToString(clientSideProperty.key()) + ")", 192+32+64+128, 384+43+13*y__);
                        y__++;
                    }
                }
            }

            g.setColor(Color.BLACK);
            if (monogram != null)
            g.setFont(monogram.deriveFont((float)(19)));
            if (military_points != null)
            g.drawImage(military_points.getScaledInstance(17,17,Image.SCALE_FAST), 20, 10, null);
            g.drawString(""+mp, 40, 22);
            g.drawString(is_it_your_turn ? "It is your turn" : "It is not your turn", 20, 40);


            if (selector != null && do_render_prev){
                if (!do_offset)
                    g.drawImage(selector.getScaledInstance(64, 64, Image.SCALE_FAST), (x_prev*64), (y_prev*64),null);
            }

            // Render currently written chat message
            if (in_client != null)
            if (in_client.isTypingChatMessage()) {
                if (monogram != null) {
                    g.setColor(Color.WHITE);
                    g.setFont(monogram.deriveFont((float) (15)).deriveFont(Font.BOLD));
                }
                String m = "[Chat]: " + in_client.getCurrentMSG();
                g.drawString(m, 500, 383 - 40);
            }

            // Render chat
            y = 0;
            for (String msg: chat){
                if (monogram != null) {
                    g.setColor(Color.WHITE);
                    g.setFont(monogram.deriveFont((float) (15)).deriveFont(Font.BOLD));
                }
                g.drawString(msg, 500, 233 + (y * 10));
                y++;
            }

            if (p != null){
                // Evil things to get JDesktopPanel to work
                BufferedImage clone = new BufferedImage(832+32,576+32+6, BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D graphics = clone.createGraphics();
                if (focus_desktop_pane) {
                    graphics.setColor(new Color(0, 0, 0, 0.5F));
                    graphics.fillRect(0, 0, 832 + 32, 576 + 32 + 6);
                }
                p.printAll(graphics);
                graphics.dispose();
                g.drawImage(clone, 0, 0, null);
            }

            g.dispose();
        } catch (Exception e){
            e.printStackTrace();
        }
        unsafeLock.unlock();
    }

    private void renderImagesScaled(ArrayList<ArrayList<BufferedImage>> buffered_images, int offset_x, int offset_y, Graphics2D g2) {
        int y;
        y = 0;
        for (ArrayList<BufferedImage> row_x: buffered_images){
            int x_count = 0;
            for (BufferedImage x : row_x){
                if (x != null)
                    g2.drawImage(x.getScaledInstance(64, 64, Image.SCALE_FAST), 64*(x_count-1)-offset_x, 64*(y-1)-offset_y, null);
                x_count++;
            }
            y++;
        }
    }

    private void renderImages(ArrayList<ArrayList<BufferedImage>> buffered_images, int offset_x, int offset_y, Graphics2D g2) {
        int y;
        y = 0;
        for (ArrayList<BufferedImage> row_x: buffered_images){
            int x_count = 0;
            for (BufferedImage x : row_x){
                if (x != null)
                    g2.drawImage(x, 64*(x_count-1)-offset_x, 64*(y-1)-offset_y, null);
                x_count++;
            }
            y++;
        }
    }
}
