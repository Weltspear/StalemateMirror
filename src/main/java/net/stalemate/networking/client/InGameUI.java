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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import net.stalemate.networking.client.config.ButtonTooltips;
import net.stalemate.networking.client.config.PropertiesMatcher;
import net.stalemate.networking.client.property.ClientSideProperty;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InGameUI extends JPanel {
    private final ClientDataRenderer renderer;
    private final KeyboardInput in_client;
    @SuppressWarnings("FieldCanBeLocal") private Font basis33;
    private Font basis33_button;

    private final JFrame frame;
    public JFrame getFrame(){return frame;}

    String[][] id_array = {{null, null, null},
                        {null, null, null},
                        {null, null, null}};


    public ClientDataRenderer getRenderer() {
        return renderer;
    }

    public KeyboardInput getInput() {
        return in_client;
    }

    public record PropertiesToRender(ArrayList<ClientSideProperty> properties){};

    private PropertiesToRender propertiesToRender = null;

    protected volatile boolean unsafe_;

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

    BufferedImage selector;

    final BufferedImage texture_missing;

    int sel_x_frame;
    int sel_y_frame;

    int cam_sel_mode;

    int x_prev = 0;
    int y_prev = 0;
    boolean do_render_prev = false;


    public void setRender(ArrayList<ArrayList<BufferedImage>> map_to_render, ArrayList<ArrayList<BufferedImage>> fog_of_war, ArrayList<ArrayList<BufferedImage>> entity_render,
                          ArrayList<BufferedImage> buttons, ArrayList<String> binds, BufferedImage unit_img, PropertiesToRender propertiesToRender, BufferedImage selector,
                          ArrayList<ArrayList<BufferedImage>> unit_data_ar, ArrayList<BufferedImage> unit_queue, ArrayList<String> unit_times,
                          int mp, boolean is_it_your_turn, String[][] button_ids, int sel_x_frame, int sel_y_frame, int cam_sel_mode, ArrayList<String> chat){
        while (unsafe_) {
            Thread.onSpinWait();
        }
        this.map_to_render = map_to_render;
        this.fog_of_war = fog_of_war;
        this.entity_render = entity_render;
        this.buttons = buttons;
        this.binds = binds;
        this.unit_img = unit_img;
        this.propertiesToRender = propertiesToRender;
        this.selector = selector;
        this.unit_data_ar = unit_data_ar;
        this.queue = unit_queue;
        this.unit_times = unit_times;
        this.mp = mp;
        this.is_it_your_turn = is_it_your_turn;
        this.id_array = button_ids;
        this.sel_x_frame = sel_x_frame;
        this.sel_y_frame = sel_y_frame;
        this.cam_sel_mode = cam_sel_mode;
        this.chat = chat;
    }

    public static class KeyboardInput implements KeyListener{

        private final ConcurrentLinkedQueue<String> keysInQueue = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<String> chatMSGS = new ConcurrentLinkedQueue<>();
        private String currentMSG = "";
        private boolean isTypingChatMessage = false;
        private volatile boolean isBusy = false;

        public String getCurrentMSG(){
            return currentMSG;
        }

        public boolean isTypingChatMessage(){
            return isTypingChatMessage;
        }

        public ConcurrentLinkedQueue<String> getChatMSGS(){
            return chatMSGS;
        }

        public synchronized ConcurrentLinkedQueue<String> getQueue(){
            while (isBusy) {
                Thread.onSpinWait();
            }
            return keysInQueue;
        }

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            isBusy = true;
            if (!isTypingChatMessage) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    keysInQueue.add("UP");
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    keysInQueue.add("DOWN");
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    keysInQueue.add("LEFT");
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    keysInQueue.add("RIGHT");
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    keysInQueue.add("ENTER");
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    keysInQueue.add("ESCAPE");
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    keysInQueue.add("SPACE");
                } else if ("qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM".contains(String.valueOf(e.getKeyChar()))) {
                    keysInQueue.add(String.valueOf(e.getKeyChar()));
                } else if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                    keysInQueue.add("CTRL");
                } else if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                    keysInQueue.add("SHIFT");
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SLASH) {
                    isTypingChatMessage = true;
                }
            } else{
                if (" qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM<>=-()[]{}\"';:.,1234567890@#$%^&*/\\".contains(String.valueOf(e.getKeyChar()))) {
                    currentMSG += String.valueOf(e.getKeyChar());
                }
                else if (e.getKeyCode() == KeyEvent.VK_ESCAPE){
                    currentMSG = "";
                    isTypingChatMessage = false;
                }
                else if (e.getKeyCode() == KeyEvent.VK_ENTER){
                    isTypingChatMessage = false;
                    chatMSGS.add(currentMSG);
                    currentMSG = "";
                }
                else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE){
                    if (currentMSG.length()-1 >= 0)
                    currentMSG = currentMSG.substring(0, currentMSG.length() - 1);
                }
            }
            isBusy = false;
        }

        @Override
        public void keyReleased(KeyEvent e) {

        }
    }

    public static class ClientDataRenderer {
        private final InGameUI interface_;
        private final BufferedImage fog_of_war;
        private final HashMap<String, BufferedImage> loaded_images = new HashMap<>();
        private final ClientMapLoader mapLoader = new ClientMapLoader();

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
        }

        @SuppressWarnings("unchecked")
        public synchronized void change_render_data(String json, int CamSelMode) throws ClientMapLoader.ClientMapLoaderException {
            try {
                PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                        .build();
                ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
                Map<String, Object> data_map = objectMapper.readValue(json, Map.class);

                mapLoader.load((String) data_map.get("map_path"));

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
                        try {
                            loaded_images.put((String) selected_unit.get("texture"), ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource((String) selected_unit.get("texture")))));
                        } catch (IOException e) {
                            e.printStackTrace();
                            texture_missing = true;
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
                                try {
                                    loaded_images.put(texture, ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource(texture))));
                                } catch (IOException | NullPointerException e) {
                                    buttons.add(this.interface_.texture_missing);
                                    binds.add((String) b.get("bind"));
                                    button_ids[y][x] = (String) b.get("id");
                                    x++;
                                    if (x == 3){
                                        y += 1;
                                        x = 0;
                                    }
                                    continue;
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
                                    try {
                                        loaded_images.put(texture, ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource(texture))));
                                    } catch (IOException | NullPointerException e) {
                                        unit_queue.add(this.interface_.texture_missing);
                                        continue;
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
                            try {
                                loaded_images.put((String) selected_unit.get("iselectorbutton_data_selector_texture"), ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource((String) selected_unit.get("iselectorbutton_data_selector_texture")))));
                            } catch (IOException e) {
                                loaded_images.put((String) selected_unit.get("iselectorbutton_data_selector_texture"), this.interface_.texture_missing);
                            }
                        }

                        selector = loaded_images.get((String)selected_unit.get("iselectorbutton_data_selector_texture"));
                    }
                }

                if (selector == null){
                    if (!loaded_images.containsKey("assets/ui/selectors/ui_select.png")) {
                        try {
                            loaded_images.put("assets/ui/selectors/ui_select.png", ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource("assets/ui/selectors/ui_select.png"))));
                        } catch (IOException e) {
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
                        try {
                            try {
                                if (texture == null) {
                                    map.get(y).add(null);
                                } else {
                                    if (!loaded_images.containsKey(texture)) {
                                        loaded_images.put(texture, ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource(texture))));
                                    }
                                    map.get(y).add(loaded_images.get(texture));
                                }
                            } catch (NullPointerException e) {
                                map.get(y).add(this.interface_.texture_missing);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
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
                            try {
                                if (texture == null) {
                                    entities.get(y).add(null);
                                } else {
                                    if (!loaded_images.containsKey(texture)) {
                                        loaded_images.put(texture, ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource(texture))));
                                    }

                                    if (unit_data_ar_.get(y).get(x__) != null){
                                        HashMap<String, Object> unit_data = unit_data_ar_.get(y).get(x__);
                                        if ((boolean) (unit_data.get("flip"))){
                                            // Flip the image
                                            AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
                                            tx.translate(-loaded_images.get(texture).getWidth(null), 0);
                                            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                                            BufferedImage image = op.filter(loaded_images.get(texture), null);

                                            entities.get(y).add(image);
                                        }
                                        else{
                                            entities.get(y).add(loaded_images.get(texture));
                                        }
                                    } else{
                                        entities.get(y).add(loaded_images.get(texture));
                                    }
                                }
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                                entities.get(y).add(this.interface_.texture_missing);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
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
                            ArrayList<Integer> rgb_team = (ArrayList<Integer>) unit_data.get("rgb");
                            Color color = new Color(rgb_team.get(0), rgb_team.get(1), rgb_team.get(2));

                            BufferedImage clone = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB_PRE);
                            Graphics2D graphics = clone.createGraphics();

                            graphics.setBackground(new Color(0x00FFFFFF, true));
                            graphics.clearRect(0, 0, clone.getWidth(), clone.getHeight());

                            graphics.setColor(color);

                            graphics.drawLine(23, 0, 31, 0);
                            graphics.drawLine(31, 0, 31 ,8);

                            graphics.drawLine( 0, 31, 0, 23);
                            graphics.drawLine(8,31, 0, 31);

                            graphics.setColor(new Color(148, 0, 21));

                            ArrayList<Integer> stats = (ArrayList<Integer>) unit_data.get("stats");
                            int hp = stats.get(0);
                            int max_hp = stats.get(1);

                            if (hp != -1 && max_hp != -1 && hp != 0 && max_hp != 0) {
                                graphics.drawLine(22, 30, 22 + (int) (9f * ((float) (hp) / (float) (max_hp))), 30);
                            }

                            graphics.setColor(Color.YELLOW);
                            int su = stats.get(2);
                            int max_su = stats.get(3);

                            if (su != -1 && max_su != -1 && su != 0 && max_su != 0){
                                graphics.drawLine(22, 31, 22+(int)(9f*((float)(su)/(float)(max_su))), 31);
                            }

                            graphics.dispose();

                            unit_data_ar.get(y).add(clone);
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
                int sel_y_frame = (sel_y - ((int)data_map.get("y")+2))*64 + 3*64;

                interface_.setRender(map, fog_of_war_, entities, buttons, binds, selected_unit_image, propertiesToRender, selector, unit_data_ar, unit_queue, unit_queue_turn_time, mp, is_it_your_turn, button_ids, sel_x_frame, sel_y_frame, CamSelMode, chat);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

        }

    }

    class MListener extends MouseAdapter{
        @Override
        public void mouseClicked(MouseEvent e) {
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
                if ((e.getX() >= 0 && e.getX() <= 832) &&
                        (e.getY() >= 64 && e.getY() <= 384)){
                    int x_selector = 448;
                    int y_selector = 256;

                    int x_diff = cam_sel_mode == 0 ? e.getX() - x_selector: sel_x_frame - e.getX();
                    int y_diff = cam_sel_mode == 0 ? e.getY() - y_selector: sel_y_frame - e.getY();

                    int right_mv = (int)Math.ceil((float)x_diff/64);
                    int down_mv = (int)Math.ceil((float) y_diff/64);

                    for (int m1 = 0; m1 < Math.abs(right_mv); m1++){
                        if (right_mv > 0){
                            in_client.keysInQueue.add(cam_sel_mode == 0 ? "RIGHT": "LEFT");
                        }
                        else if (right_mv < 0){
                            in_client.keysInQueue.add(cam_sel_mode == 0 ? "LEFT": "RIGHT");
                        }
                    }

                    for (int m2 = 0; m2 < Math.abs(down_mv); m2++){
                        if (down_mv > 0){
                            in_client.keysInQueue.add(cam_sel_mode == 0 ? "DOWN": "UP");
                        }
                        else if (down_mv < 0){
                            in_client.keysInQueue.add(cam_sel_mode == 0 ? "UP": "DOWN");
                        }
                    }
                }
            } else if (e.getButton() == MouseEvent.BUTTON2){
                in_client.keysInQueue.add("ESCAPE");
            } else {
                in_client.keysInQueue.add("ENTER");
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            boolean clearTooltip = false;
            int y = 0;
            for (String[] row: id_array) {
                int x = 0;
                for (String point: row) {
                    if (e.getX() >= (10 * 64) + (x * 64) && e.getY() >= (6 * 64) + (y * 64)
                        && e.getX() <= (11 * 64) + (x * 64) && e.getY() <= (7 * 64) + (y * 64)) {
                        if (ButtonTooltips.getTooltip(point) != null)
                            InGameUI.this.setToolTipText("<html><font face=\"basis33\" size=4>" + ButtonTooltips.getTooltip(point) + "</font></html>");
                        else
                            InGameUI.this.setToolTipText(null);
                        clearTooltip = true;
                    }
                    x++;
                }
                y++;
            }

            if ((((64 < e.getY())&&( e.getY() < 6 * 64)) && ((0 < e.getX())&&(e.getX() < 13 * 64))) || (((10 * 64 < e.getX())&&(e.getX() < 13 * 64)) && ((6 * 64 < e.getY())&&( e.getY() < 9 * 64)))){
                InGameUI.this.x_prev = e.getX() / 64;
                InGameUI.this.y_prev = e.getY() / 64;
                InGameUI.this.do_render_prev = true;
            }
            else {
                InGameUI.this.do_render_prev = false;
            }

            if (!clearTooltip) {
                InGameUI.this.setToolTipText(null);
            }
        }
    }

    public InGameUI(){
        ButtonTooltips.init();
        PropertiesMatcher.loadPropertyMatcher();
        renderer = new ClientDataRenderer(this);

        frame = new JFrame("Stalemate");
        frame.setMinimumSize(new Dimension(832+14,576+32+6));
        frame.setSize(new Dimension(832+32,576+32+6));
        MListener m = new MListener();

        this.addMouseMotionListener(m);
        this.addMouseListener(m);

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

        try {
            basis33 = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getClassLoader().getResource("basis33/basis33.ttf")).openStream());
            basis33_button = basis33.deriveFont(((float)(14))).deriveFont(Font.BOLD);

        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }

        UIManager.put("ToolTip.background", Color.BLACK);
        UIManager.put("ToolTip.foreground", Color.WHITE);

        // because tooltip font stopped working correctly of html for some reason
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(basis33);

        try {
            placeholder_ui = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource("assets/placeholder_ui.png")));
            placeholder_ui_2 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource("assets/placeholder_ui_2.png")));
            panel = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource("assets/panel.png")));
            selector = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource("assets/ui/selectors/ui_select.png")));
        }
        catch (IOException e) {
            placeholder_ui = texture_missing;
            placeholder_ui_2 = texture_missing;
            panel = texture_missing;
            selector = texture_missing;
        }
    }

    public void paint(Graphics g)
    {
        g.clearRect(0, 0, 900, 900);
        try {
            unsafe_ = true;
            g.drawImage(placeholder_ui, 0, 384, null);
            g.drawImage(placeholder_ui_2, 0, 0, null);
            g.drawImage(panel, 640, 384, null);
            if (queue != null)
            g.drawImage(panel, 0, 384, null);

            int y = 0;
            for (ArrayList<BufferedImage> row_x: map_to_render){
                int x_count = 0;
                for (BufferedImage x : row_x){
                    g.drawImage(x != null ? x.getScaledInstance(64, 64, Image.SCALE_FAST) : ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource("empty.png"))).getScaledInstance(64, 64, Image.SCALE_FAST), 64*x_count, 64+64*y, null);
                    x_count++;
                }
                y++;
            }

            y = 0;
            for (ArrayList<BufferedImage> row_x: entity_render){
                int x_count = 0;
                for (BufferedImage x : row_x){
                    if (x != null)
                    g.drawImage(x.getScaledInstance(64, 64, Image.SCALE_FAST), 64*x_count, 64+64*y, null);
                    x_count++;
                }
                y++;
            }

            y = 0;
            for (ArrayList<BufferedImage> row_x_: fog_of_war){
                int x_count = 0;
                for (BufferedImage x : row_x_){
                    if (x != null) {
                        g.drawImage(x.getScaledInstance(64, 64, Image.SCALE_FAST), 64 * x_count, 64 + 64 * y, null);
                    }
                    x_count++;
                }
                y++;
            }

            // Render unit_data_ar
            y = 0;
            for (ArrayList<BufferedImage> row_x: unit_data_ar){
                int x_count = 0;
                for (BufferedImage x_ : row_x){
                    if (x_ != null)
                        g.drawImage(x_.getScaledInstance(64, 64, Image.SCALE_FAST), 64*x_count, 64+64*y, null);
                    x_count++;
                }
                y++;
            }

            if (selector != null)
            g.drawImage(selector.getScaledInstance(64, 64, Image.SCALE_FAST), sel_x_frame,sel_y_frame, null);

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
            g.setFont(basis33_button);

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
            g.setFont(basis33_button);

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
            if (propertiesToRender != null){
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
                g.setFont(basis33.deriveFont((float)(27)).deriveFont(Font.BOLD));

                // Get font char size
                FontMetrics metrics = g.getFontMetrics(basis33.deriveFont((float)(27)).deriveFont(Font.BOLD));
                int width = metrics.stringWidth("A");

                int h = ((224 - (name.value().length() * width))/2);

                g.drawString(name.value(),192+32+64+128+h, 384+30); // 416-640

                g.setFont(basis33.deriveFont((float)(20)));

                int y__ = 1;
                for (ClientSideProperty clientSideProperty: propertiesToRender2.properties){
                    if (PropertiesMatcher.matchKeyToString(clientSideProperty.key()) != null){
                        g.drawString(PropertiesMatcher.matchKeyToString(clientSideProperty.key()) + ": " + clientSideProperty.value(), 192+32+64+128, 384+43+13*y__);
                        y__++;
                    }
                }
            }

            g.setColor(Color.BLACK);
            if (basis33 != null)
            g.setFont(basis33.deriveFont((float)(15)));
            g.drawString("Military Points: " + mp, 20, 20);
            g.drawString(is_it_your_turn ? "It is your turn" : "It is not your turn", 20, 40);


            if (selector != null && do_render_prev){
                g.drawImage(selector.getScaledInstance(64, 64, Image.SCALE_FAST), x_prev*64,y_prev*64,null);
            }

            // Render currently written chat message
            if (in_client.isTypingChatMessage()) {
                if (basis33 != null) {
                    g.setColor(Color.WHITE);
                    g.setFont(basis33.deriveFont((float) (15)).deriveFont(Font.BOLD));
                }
                String m = "[Chat]: " + in_client.getCurrentMSG();
                g.drawString(m, 500, 383 - 40);
            }

            // Render chat
            y = 0;
            for (String msg: chat){
                if (basis33 != null) {
                    g.setColor(Color.WHITE);
                    g.setFont(basis33.deriveFont((float) (15)).deriveFont(Font.BOLD));
                }
                g.drawString(msg, 500, 233 + (y * 10));
                y++;
            }

            unsafe_ = false;
            g.dispose();
        } catch (Exception e){
            e.printStackTrace();
            unsafe_ = false;
        }


    }
}
