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

import net.stalemate.client.AssetLoader;
import net.stalemate.client.ClientGame;
import net.stalemate.client.ClientMapLoader;
import net.stalemate.client.SpecialTeamReprReg;
import net.stalemate.client.config.ButtonTooltips;
import net.stalemate.client.config.Grass32ConfigClient;
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
    private final ClientDataManager clDataManager;
    private final KeyboardInput in_client;
    private final JDesktopPane p;
    @SuppressWarnings("FieldCanBeLocal") private final Font monogram;
    private final Font monogram_button;
    private final Font monogram_button_small;
    private final MListener m;
    private boolean focus_desktop_pane = false;

    private final Image empty;

    private boolean isdead = false; // it is here because of weird issues with javax.swing

    private Image minimap = null;

    private final JFrame frame;
    private boolean do_offset = false;

    private boolean hasFirstPackedBeenReceived = false;

    public JFrame getFrame(){return frame;}

    /***
     * Ids of buttons to be rendered
     */
    String[][] id_array = {{null, null, null},
            {null, null, null},
            {null, null, null}};


    public ClientDataManager getClDataManager() {
        return clDataManager;
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

    private int camera_speed = 1;

    enum OffsetDirection{
        None,
        Left,
        Right,
        Up,
        Down
    }

    private OffsetDirection offset_direction = OffsetDirection.None;

    ArrayList<ArrayList<Image>> map_to_render = new ArrayList<>();
    ArrayList<ArrayList<BufferedImage>> fog_of_war = new ArrayList<>();
    // selector range
    ArrayList<ArrayList<BufferedImage>> _selr = new ArrayList<>();
    ArrayList<ArrayList<Image>> entity_render = new ArrayList<>();
    ArrayList<ArrayList<BufferedImage>> unit_data_ar = new ArrayList<>();
    ArrayList<Image> buttons = new ArrayList<>();
    ArrayList<Image> queue = null;
    ArrayList<String> binds = new ArrayList<>();
    ArrayList<String> unit_times = new ArrayList<>();
    ArrayList<String> chat = new ArrayList<>();
    Image unit_img = null;
    int mp = 0;
    boolean is_it_your_turn = false;

    BufferedImage placeholder_ui;
    BufferedImage placeholder_ui_2;
    BufferedImage panel;
    BufferedImage military_points;

    BufferedImage selector;

    int sel_x_frame;
    int sel_y_frame;

    int x_prev = 0;
    int y_prev = 0;
    // do render preview
    boolean do_render_prev = false;

    private int smallify_button = -1;
    private int smallify_button_renders = 3;

    private EscapeMenu escapeMenu = null;

    public int cam_x;
    public int cam_y;

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
            if (!focus_desktop_pane) {
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
                        if (binds != null){
                            int btn_idx = 0;
                            for (String bind: binds){
                                if (Objects.equals(bind != null ? bind.toLowerCase(Locale.ROOT) : "",String.valueOf(e.getKeyChar()).toLowerCase(Locale.ROOT))){
                                    smallify_button = btn_idx;
                                    smallify_button_renders = 3;
                                }
                                btn_idx++;
                            }
                        }
                    } else if (e.getKeyCode() == KeyboardBindMapper.lock_camera) {
                        unsafeLock.lock();
                        if (!dis_offset) {
                            dis_offset = true;
                            offset_direction = OffsetDirection.None;
                        } else
                            dis_offset = false;
                        unsafeLock.unlock();
                    } else if (e.getKeyCode() == KeyboardBindMapper.goto_first_built_base) {
                        keysInQueue.add("SHIFT");
                    } else if (e.getKeyCode() == KeyboardBindMapper.chat) {
                        isTypingChatMessage = true;
                    } else if (e.getKeyCode() == KeyEvent.VK_F1) {
                        keysInQueue.add("TAB");
                    } else if (e.getKeyCode() == KeyEvent.VK_F10 && !isTypingChatMessage) {
                        spawnEscapeMenu();
                    } else if (e.getKeyCode() == KeyboardBindMapper.cam_speed_up){
                        if (camera_speed * 2 != 8) {
                            camera_speed *= 2;
                        }
                    } else if (e.getKeyCode() == KeyboardBindMapper.cam_speed_down){
                        if (camera_speed != 1) {
                            camera_speed /= 2;
                        }
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
            }
            lock.unlock();
        }

        @Override
        public void keyReleased(KeyEvent e) {

        }
    }

    public static class ClientDataManager {
        private final InGameUI interface_;
        private final BufferedImage fog_of_war;
        private final ClientMapLoader mapLoader = new ClientMapLoader();
        private final BufferedImage skull;
        private final BufferedImage shovel;

        private final HashMap<String, Image> entity_scaled_cache = new HashMap<>();
        private final HashMap<String, Image> button_scaled_cache = new HashMap<>();
        private final HashMap<BufferedImage, Image> queue_scaled_cache = new HashMap<>();
        private final HashMap<BufferedImage, Image> big_unit_texture_cache = new HashMap<>();
        private final HashMap<String, Image> map_scaled_cache = new HashMap<>();

        private int tsel_x;
        private int tsel_y;

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

            Those are in packet in order to teleport camera to player's first base
            c = cam
            s = selector
            "cbas_x" : 0,
            "cbas_y" : 0,
            "sbas_x" : 0,
            "sbas_y" : 0,

            "entity_data" : [
                {
                    "type" : "unit" | "entity",
                    "rgb" : int,
                    "stats" : [...],
                    "transparent" : bool,
                    "texture" : str
                    "flip" : bool
                    "x" : int,
                    "y" : int
                }

            ]
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

        public ClientDataManager(InGameUI interface_) {
            fog_of_war = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics2D = fog_of_war.createGraphics();
            graphics2D.setColor(new Color(0,0,0, 0.5F));
            graphics2D.fillRect(0, 0, 64, 64);
            graphics2D.dispose();
            this.interface_ = interface_;

            shovel = AssetLoader.load("assets/shovel.png");
            skull = AssetLoader.load("assets/skull.png");
        }

        public Image unit2image(ClientGame.ClientUnit e){
            BufferedImage image;

            String cached = e.getTextureLoc();

            if (e.isFlip()){
                cached = cached+"+flip";
            }

            if (SpecialTeamReprReg.getTeamRepr(e.getTextureLoc()) != null){
                cached = cached+"+rgb"+e.getTeamColor();
            }

            if (e.isTransparent()){
                cached = cached+"+transparent";
            }

            if (entity_scaled_cache.containsKey(cached)){
                return entity_scaled_cache.get(cached);
            }
            else {
                if (SpecialTeamReprReg.getTeamRepr(e.getTextureLoc()) != null) {
                    image = new BufferedImage(e.getTexture().getWidth(), e.getTexture().getHeight(), e.getTexture().getType());
                    Graphics g = image.getGraphics();
                    g.drawImage(e.getTexture(), 0, 0, null);
                    g.dispose();

                    SpecialTeamReprReg.TeamRepr teamRepr = SpecialTeamReprReg.getTeamRepr(e.getTextureLoc());
                    for (int[] c : teamRepr.getCoords()) {
                        image.setRGB(c[0], c[1], e.getTeamColor().getRGB());
                    }
                }
                else {
                    image = e.getTexture();
                }

                if (e.isFlip()) {
                    // Flip the image
                    AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
                    tx.translate(-image.getWidth(), 0);
                    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                    image = op.filter(image, null);
                }

                if (e.isTransparent()) {
                    BufferedImage clone = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB_PRE);
                    Graphics2D graphics = clone.createGraphics();

                    graphics.setBackground(new Color(0x00FFFFFF, true));
                    graphics.clearRect(0, 0, clone.getWidth(), clone.getHeight());

                    graphics.drawImage(image, 0, 0, null);

                    graphics.dispose();

                    for (int y_ = 0; y_ < 32; y_++) {
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
                Image img_d = image.getScaledInstance(64, 64, Image.SCALE_FAST);
                entity_scaled_cache.put(cached, img_d);
                return img_d;
            }
        }

        public void updateData(ArrayList<String> chat, ClientGame.ClientEntity[][] _entities,
                               boolean[][] fog_of_war, boolean[][] selr, ClientGame.ClientSelectedUnit selectedUnit, int mp,
                               boolean is_it_your_turn, ClientMapLoader clMapLoader, Image minimap) {
            if (!clMapLoader.isMapLoaded()){
                return;
            }
            try {
                this.interface_.unsafeLock.lock();

                this.interface_.hasFirstPackedBeenReceived = true;

                this.interface_.minimap = minimap;

                if (!mapLoader.isMapLoaded()){
                    mapLoader.loadFromOther(clMapLoader);
                }

                ArrayList<ArrayList<String>> map_textures = mapLoader.getMap(this.interface_.cam_x, this.interface_.cam_y);

                ArrayList<Image> buttons = new ArrayList<>();
                ArrayList<String> binds = new ArrayList<>();

                Image selected_unit_image = null;
                PropertiesToRender propertiesToRender = null;
                BufferedImage selector = null;

                ArrayList<Image> unit_queue = null;
                ArrayList<String> unit_queue_turn_time = new ArrayList<>();

                String[][] button_ids = {{null, null, null}, {null, null, null}, {null, null, null}};

                if (selectedUnit != null) {
                    if (big_unit_texture_cache.containsKey(selectedUnit.getTexture())) {
                        selected_unit_image = big_unit_texture_cache.get(selectedUnit.getTexture());
                    }
                    else{
                        Image f = selectedUnit.getTexture().getScaledInstance(128,128,Image.SCALE_FAST);
                        selected_unit_image = f;
                        big_unit_texture_cache.put(selectedUnit.getTexture(), f);
                    }
                    propertiesToRender = new PropertiesToRender(selectedUnit.getProperties());
                    selector = selectedUnit.getISelectorButtonPress();

                    // Deal with unit queue

                    if (selectedUnit.getQueue() != null) {
                        unit_queue = new ArrayList<>();
                        for (ClientGame.ClientSelectedUnit.ClientUnitQueueElement queueElement : selectedUnit.getQueue()) {
                            if (queue_scaled_cache.containsKey(queueElement.getTexture()))
                            unit_queue.add(queue_scaled_cache.get(queueElement.getTexture()));
                            else{
                                Image f = queueElement.getTexture().getScaledInstance(64, 64, Image.SCALE_FAST);
                                queue_scaled_cache.put(queueElement.getTexture(), f);
                                unit_queue.add(f);

                            }
                            unit_queue_turn_time.add("" + queueElement.getTurn_time());
                        }
                    }

                    // Deal with the buttons

                    int by = 0;
                    int bx = 0;
                    for (ClientGame.ClientSelectedUnit.ClientButton button : selectedUnit.getButtons()) {
                        if (bx == 3) {
                            bx = 0;
                            by++;
                        }

                        if (button != null) {
                            button_ids[by][bx] = button.getId();

                            if (button_scaled_cache.containsKey(button.getId())){
                                buttons.add(button_scaled_cache.get(button.getId()));
                            }
                            else{
                                Image f = button.getImage().getScaledInstance(64,64,Image.SCALE_FAST);
                                button_scaled_cache.put(button.getId(), f);
                                buttons.add(f);
                            }

                            binds.add(button.getBind());
                        } else {
                            buttons.add(null);
                            binds.add(null);
                        }

                        bx++;
                    }
                }

                if (selector == null)
                    selector = AssetLoader.load("assets/ui/selectors/ui_select.png");

                // deal with map
                ArrayList<ArrayList<Image>> map = new ArrayList<>();
                int y = 0;
                for (ArrayList<String> x_row : map_textures) {
                    map.add(new ArrayList<>());
                    for (String texture : x_row) {
                        if (texture == null) {
                            map.get(y).add(null);
                        }
                        else {
                            if (map_scaled_cache.containsKey(texture)){
                                map.get(y).add(map_scaled_cache.get(texture));
                            }
                            else{
                                Image f = AssetLoader.load(texture).getScaledInstance(64, 64, Image.SCALE_FAST);
                                map_scaled_cache.put(texture, f);
                                map.get(y).add(f);
                            }

                        }
                    }
                    y++;
                }

                // Deal with entities
                ArrayList<ArrayList<Image>> entities = new ArrayList<>();
                y = 0;
                int x__ = 0;
                for (ClientGame.ClientEntity[] x_row : _entities) {
                    entities.add(new ArrayList<>());
                    for (ClientGame.ClientEntity centity: x_row) {
                        if (centity == null) {
                            entities.get(y).add(null);
                        } else {
                            if (centity instanceof ClientGame.ClientUnit ucentity) {
                                Image image = unit2image(ucentity);

                                entities.get(y).add(image);
                            } else {
                                entities.get(y).add(centity.getTexture().getScaledInstance(64, 64, Image.SCALE_FAST));
                            }
                        }

                        x__++;
                    }
                    x__ = 0;
                    y++;
                }

                // Fog of war
                ArrayList<ArrayList<BufferedImage>> fog_of_war_ = new ArrayList<>();
                y = 0;
                for (boolean[] x_row : fog_of_war) {
                    fog_of_war_.add(new ArrayList<>());
                    for (boolean texture : x_row) {
                        if (!texture) {
                            fog_of_war_.get(y).add(this.fog_of_war);
                        } else {
                            fog_of_war_.get(y).add(null);
                        }
                    }
                    y++;
                }

                // sel_r

                BufferedImage slr_img = AssetLoader.load("selr");

                ArrayList<ArrayList<BufferedImage>> sel_r = new ArrayList<>();
                y = 0;
                for (boolean[] x_row : selr) {
                    sel_r.add(new ArrayList<>());
                    for (boolean texture : x_row) {
                        if (texture) {
                            sel_r.get(y).add(slr_img);
                        } else {
                            sel_r.get(y).add(null);
                        }
                    }
                    y++;
                }

                // Create those team showing thingies
                ArrayList<ArrayList<BufferedImage>> unit_data_ar = new ArrayList<>();
                y = 0;
                for (ClientGame.ClientEntity[] row : _entities){
                    unit_data_ar.add(new ArrayList<>());
                    for (ClientGame.ClientEntity centity: row) {
                        if (centity instanceof ClientGame.ClientUnit ucentity) {

                            // Calculate hash
                            Color rgb_team = ucentity.getTeamColor();
                            boolean has_unit_su_enabled = ucentity.getSu() != -1 && ucentity.getMaxSu() != -1 && ucentity.getSu() != 0 && ucentity.getMaxSu() != 0;
                            Object[] hash_ar = new Object[]{rgb_team,
                                    ((float) ucentity.getHp()) / ((float) ucentity.getMaxHp()),
                                    (has_unit_su_enabled) ? ((float) ucentity.getSu()) / ((float) ucentity.getMaxSu()) : -1,
                                    0};

                            // takes indicators into account
                            if (ucentity.getSu() < ucentity.getMaxSu() * 0.4 && has_unit_su_enabled) {
                                hash_ar[3] = -1;
                            } else if (ucentity.getEt() > 0 && !(ucentity.getSu() < ucentity.getMaxSu() * 0.4 && has_unit_su_enabled)) {
                                hash_ar[3] = ucentity.getEt();
                            }

                            if (!cachedUnitDataArImgs.containsKey(Arrays.hashCode(hash_ar))) {

                                BufferedImage clone = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB_PRE);
                                Graphics2D graphics = clone.createGraphics();

                                graphics.setBackground(new Color(0x00FFFFFF, true));
                                graphics.clearRect(0, 0, clone.getWidth(), clone.getHeight());

                                graphics.setColor(rgb_team);

                                graphics.drawLine(23, 0, 31, 0);
                                graphics.drawLine(31, 0, 31, 8);

                                graphics.drawLine(0, 31, 0, 23);
                                graphics.drawLine(8, 31, 0, 31);

                                graphics.setColor(new Color(148, 0, 21));

                                int hp = ucentity.getHp();
                                int max_hp = ucentity.getMaxHp();

                                if (hp != -1 && max_hp != -1 && hp != 0 && max_hp != 0) {
                                    graphics.drawLine(22, 30, 22 + (int) (9f * ((float) (hp) / (float) (max_hp))), 30);
                                }

                                graphics.setColor(Color.YELLOW);
                                int su = ucentity.getSu();
                                int max_su = ucentity.getMaxSu();

                                if (has_unit_su_enabled) {
                                    graphics.drawLine(22, 31, 22 + (int) (9f * ((float) (su) / (float) (max_su))), 31);
                                }

                                graphics.dispose();

                                int et = ucentity.getEt();
                                // Draw shovel
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

                                Image scaled = clone.getScaledInstance(64, 64, Image.SCALE_FAST);
                                clone = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB_PRE);
                                graphics = clone.createGraphics();
                                graphics.setBackground(new Color(0x00FFFFFF, true));
                                graphics.drawImage(scaled, 0, 0, null);
                                // Draw skull to indicate that unit is under supplied
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
                            } else
                                unit_data_ar.get(y).add(cachedUnitDataArImgs.get(Arrays.hashCode(hash_ar)).image);
                        }
                        else {
                            unit_data_ar.get(y).add(null);
                        }
                    }
                    y++;
                }

                // if no new sel_x and sel_y have been received in a long time fall back to previous
                this.interface_.sel_x_frame = (tsel_x - (this.interface_.cam_x+6))*64 + 6*64;
                this.interface_.sel_y_frame = (tsel_y - (this.interface_.cam_y+2))*64 + 2*64;

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
                this.interface_.chat = chat;
                this.interface_._selr = sel_r;
                this.interface_.unsafeLock.unlock();

                if (cachedUnitDataArImgs.size() > 100){
                    cachedUnitDataArImgs.clear();
                }

                interface_.unsafeLock.unlock();
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }

            interface_.unsafeLock.unlock();
        }

        public void setSelectorData(int sel_x, int sel_y){
            interface_.unsafeLock.lock();
            tsel_x = sel_x;
            tsel_y = sel_y;
            int sel_x_frame = (sel_x - (this.interface_.cam_x+6))*64 + 6*64;
            int sel_y_frame = (sel_y - (this.interface_.cam_y+2))*64 + 2*64;

            this.interface_.sel_x_frame = sel_x_frame;
            this.interface_.sel_y_frame = sel_y_frame;
            interface_.unsafeLock.unlock();
        }

    }

    class MListener extends MouseAdapter{
        @Override
        public void mouseClicked(MouseEvent e) {
            unsafeLock.lock();
            if (!focus_desktop_pane) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    int y = 0;
                    int x = 0;
                    int button_idx = 0;
                    for (String bind : binds) {
                        if (e.getX() >= (10 * 64) + (x * 64) && e.getY() >= (6 * 64) + (y * 64)
                                && e.getX() <= (11 * 64) + (x * 64) && e.getY() <= (7 * 64) + (y * 64)) {
                            if (bind != null) {
                                InGameUI.this.smallify_button = button_idx;
                                InGameUI.this.smallify_button_renders = 3;
                                in_client.keysInQueue.add(bind);
                                break;
                            }
                        }
                        x++;
                        if (x == 3) {
                            x = 0;
                            y++;
                        }
                        button_idx++;
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

                            in_client.keysInQueue.add("ENTER");
                        }
                    }
                }
                else if (e.getButton() == MouseEvent.BUTTON2) {
                    in_client.keysInQueue.add("ESCAPE");
                } else {
                    in_client.keysInQueue.add("ENTER");
                }
            }
            unsafeLock.unlock();
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

    public InGameUI(JFrame frame){
        super(null);
        ButtonTooltips.init();
        PropertiesMatcher.loadPropertyMatcher();
        clDataManager = new ClientDataManager(this);

        this.frame = frame;
        this.frame.setMinimumSize(new Dimension(832+14,576+32+6));
        this.frame.setSize(new Dimension(832+32,576+32+6));
        this.m = new MListener();

        this.addMouseMotionListener(m);
        this.addMouseListener(m);

        in_client = new KeyboardInput();
        this.addKeyListener(in_client);
        this.requestFocusInWindow();

        p = new JDesktopPane();
        p.setBackground(new Color(0x00FFFFFF, true));
        p.setVisible(true);
        p.setSize(new Dimension(832+14,576));
        p.setPreferredSize(new Dimension(832+14,576));
        p.setFocusable(true);
        this.add(p);

        this.setMinimumSize(new Dimension(832+14,576+32+6));
        this.setSize(new Dimension(832+32,576+32+6));

        this.frame.setResizable(false);
        this.frame.add(this);

        this.frame.pack();

        monogram = AssetLoader.getMonogram();
        monogram_button = monogram.deriveFont(((float)(16)));
        monogram_button_small = monogram.deriveFont(((float)(14.4)));

        UIManager.put("ToolTip.background", Color.BLACK);
        UIManager.put("ToolTip.foreground", Color.WHITE);

        placeholder_ui = AssetLoader.load("assets/placeholder_ui.png");
        placeholder_ui_2 = AssetLoader.load("assets/placeholder_ui_2.png");
        panel = AssetLoader.load("assets/panel.png");
        selector = AssetLoader.load("assets/ui/selectors/ui_select.png");
        military_points = AssetLoader.load("assets/mp.png");

        this.frame.setIconImage(AssetLoader.load("assets/ui/selectors/ui_attack.png"));

        empty = AssetLoader.load("empty.png").getScaledInstance(64, 64, Image.SCALE_FAST);

        this.setFocusable(true);
        this.requestFocus();
    }

    public void clFrame(){
        unsafeLock.lock();
        for (JInternalFrame frame: p.getAllFrames()){
            frame.dispose();
        }
        this.remove(p);
        this.removeKeyListener(in_client);
        this.removeMouseListener(m);
        this.removeMouseMotionListener(m);
        frame.remove(this);
        isdead = true;
        unsafeLock.unlock();
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

    private void terminateEscapeMenu(){
        p.remove(escapeMenu);
        escapeMenu.dispose();
        escapeMenu = null;
        focus_desktop_pane = false;
        this.addKeyListener(in_client);
        this.addMouseListener(m);
        this.addMouseMotionListener(m);
        this.requestFocus();
    }

    private void prepareDesktopPane(){
        this.removeKeyListener(in_client);
        this.removeMouseListener(m);
        this.removeMouseMotionListener(m);
        offset_direction = OffsetDirection.None;
        focus_desktop_pane = true;
    }

    public void inGameUIUpdate(){
        unsafeLock.lock();
        if (showResults){
            spawnEscapeMenu = false;
            if (escapeMenu != null){
                terminateEscapeMenu();
            }

            resultMenu.setLocation((frame.getWidth()-resultMenu.getWidth())/2, (frame.getHeight()-resultMenu.getHeight())/2);
            p.add(resultMenu);
            showResults = false;
            prepareDesktopPane();
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
            spawnEscapeMenu = false;
            escapeMenu.setVisible(true);
            prepareDesktopPane();
        }

        if (escapeMenu != null){
            if (escapeMenu.getStatus() == 3){
                terminateEscapeMenu();
            }
        }
        if (escapeMenu != null){
            if (escapeMenu.getStatus() == 1)
                termicon = true;
        }

        if (offset_direction == OffsetDirection.Left){
            if (offset_x >= -64 && clDataManager.mapLoader.isMapLoaded()) {
                offset_x -= 4*2*camera_speed;

                if (offset_x <= -64) {
                    if (cam_x + 5 >= 0) {
                        cam_x--;
                        offset_x = 0;
                    }
                }
            }
        }
        else if (offset_direction == OffsetDirection.Right){
            if (offset_x <= 64 && clDataManager.mapLoader.isMapLoaded()) {
                offset_x += 4*2*camera_speed;

                if (offset_x >= 64) {
                    if (cam_x + 7 < clDataManager.getMapLoader().getWidth()) {
                        cam_x++;
                        offset_x = 0;
                    }
                }
            }
        }

        if (offset_x > 64){
            offset_x = 64;
        }
        if (offset_x < -64){
            offset_x = -64;
        }


        if (offset_direction == OffsetDirection.Up){
            if (offset_y >= -64 && clDataManager.mapLoader.isMapLoaded()) {
                offset_y -= 4*2*camera_speed;

                if (offset_y <= -64){
                    if (cam_y + 1 >= 0) {
                        cam_y--;
                        offset_y = 0;
                    }
                }
            }
        }
        if (offset_direction == OffsetDirection.Down){
            if (offset_y <= 64 && clDataManager.mapLoader.isMapLoaded()) {
                offset_y += 4*2*camera_speed;

                if (offset_y >= 64){
                    if (cam_y + 3 < clDataManager.getMapLoader().getHeight()) {
                        cam_y++;
                        offset_y = 0;
                    }
                }
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
        if (isdead)
        {
            return;
        }
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
            g.drawImage(panel, 0, 384, null);

            if (hasFirstPackedBeenReceived) {
                BufferedImage bufferedImage = new BufferedImage(13 * 64, 5 * 64, BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D g2 = bufferedImage.createGraphics();

                int y = 0;
                for (ArrayList<Image> row_x : map_to_render) {
                    int x_count = 0;
                    for (Image x : row_x) {
                        g2.drawImage(x != null ? x : empty, 64 * (x_count - 1) - offset_x, 64 * (y - 1) - offset_y, null);
                        x_count++;
                    }
                    y++;
                }

                renderImages2(entity_render, offset_x, offset_y, g2);
                renderImages(fog_of_war, offset_x, offset_y, g2);
                renderImages(unit_data_ar, offset_x, offset_y, g2);
                renderImages(_selr, offset_x, offset_y, g2);

                // Preview selector
                if (selector != null && do_render_prev && do_offset) {
                    g2.drawImage(selector.getScaledInstance(64, 64, Image.SCALE_FAST), x_prev, y_prev - 64, null);
                }

                // Normal Selector
                if (selector != null)
                    g2.drawImage(selector.getScaledInstance(64, 64, Image.SCALE_FAST), sel_x_frame - offset_x, sel_y_frame - offset_y, null);

                g2.dispose();
                g.drawImage(bufferedImage, 0, 64, null);

                // Render the buttons
                int i = 0;
                int x = 0;
                y = 0;
                int btn_idx = 0;
                for (Image button : buttons) {
                    if (button != null) {
                        if (btn_idx == smallify_button){
                            g.drawImage(button.getScaledInstance(57, 57, Image.SCALE_FAST), 640 + x + 3, 384 + y + 3, null);
                            if (Grass32ConfigClient.doSteelButtonOverlay())
                                g.drawImage(AssetLoader.load("assets/ui/buttons/steel_button_overlay_2.png").getScaledInstance(57, 57, Image.SCALE_FAST), 640 + x + 3, 384 + y + 3, null);
                        }
                        else {
                            g.drawImage(button, 640 + x, 384 + y, null);
                            if (Grass32ConfigClient.doSteelButtonOverlay())
                                g.drawImage(AssetLoader.load("assets/ui/buttons/steel_button_overlay_2.png"), 640 + x, 384 + y, null);
                        }
                    }
                    i++;
                    x += 64;
                    if (i == 3) {
                        i = 0;
                        x = 0;
                        y += 64;
                    }
                    btn_idx++;
                }

                // Render minimap
                if (minimap != null && queue == null)
                    g.drawImage(minimap, 16, 400, null);

                // Render the queue
                if (queue != null) {
                    i = 0;
                    x = 0;
                    y = 0;
                    for (Image m : queue) {
                        if (m != null) {
                            g.drawImage(m, x, 384 + y, null);
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
                if (!Grass32ConfigClient.doSteelButtonOverlay())
                    g.setColor(Color.BLACK);
                else
                    g.setColor(Color.WHITE);
                g.setFont(monogram_button);

                i = 0;
                x = 0;
                y = 0;
                btn_idx = 0;
                for (String bind : binds) {
                    if (bind != null) {
                        if (btn_idx == smallify_button) {
                            g.setFont(monogram_button_small);
                            g.drawString(bind, 640 + x + 8 + 3, 383 + y + 11 + 3);
                            smallify_button_renders--;
                            if (smallify_button_renders == 0)
                                smallify_button = -1;
                            g.setFont(monogram_button);
                        }
                        else{
                            g.drawString(bind, 640 + x + 8, 383 + y + 11);
                        }
                    }
                    i++;
                    x += 64;
                    if (i == 3) {
                        i = 0;
                        x = 0;
                        y += 64;
                    }
                    btn_idx++;
                }

                // Render the time of production
                g.setColor(Color.BLACK);
                g.setFont(monogram_button);

                i = 0;
                x = 0;
                y = 0;
                for (String bind : unit_times) {
                    if (bind != null) {
                        g.drawString(bind, x + 8, 383 + y + 11);
                    }
                    i++;
                    x += 64;
                    if (i == 3) {
                        i = 0;
                        x = 0;
                        y += 64;
                    }
                }

                // Render the unit
                if (unit_img != null)
                    g.drawImage(unit_img, 192 + 32, 384 + 32, null);

                // Render the stats
                if (propertiesToRender != null && monogram != null) {
                    // Find name of a unit
                    ClientSideProperty name = null;
                    for (ClientSideProperty property : propertiesToRender.properties) {
                        if (property.key().equals("name")) {
                            name = property;
                        }
                    }
                    @SuppressWarnings("unchecked") ArrayList<ClientSideProperty> properties = (ArrayList<ClientSideProperty>) propertiesToRender.properties.clone();
                    properties.remove(name);
                    PropertiesToRender propertiesToRender2 = new PropertiesToRender(properties);

                    g.setColor(Color.BLACK);
                    g.setFont(monogram.deriveFont((float) (32)).deriveFont(Font.BOLD));

                    // Get font char size
                    FontMetrics metrics = g.getFontMetrics(monogram.deriveFont((float) (27)).deriveFont(Font.BOLD));
                    int width = metrics.stringWidth("A");

                    if (name == null) {
                        name = new ClientSideProperty("name", "");
                    }

                    int h = ((224 - (name.value().length() * width)) / 2);

                    g.drawString(name.value(), 192 + 32 + 64 + 128 + h, 384 + 30); // 416-640

                    Font monogram23 = monogram.deriveFont(Font.PLAIN, 23f);

                    g.setFont(monogram23);

                    int y__ = 1;
                    for (ClientSideProperty clientSideProperty : propertiesToRender2.properties) {
                        if (clientSideProperty.key().equals("uname")){
                            if (clientSideProperty.value().isEmpty()){
                                continue;
                            }
                            g.setFont(monogram.deriveFont(Font.PLAIN, 16f));

                            FontMetrics metrics_16 = g.getFontMetrics();
                            int width_16 = metrics_16.stringWidth("A");

                            int h2 = ((224 - ((clientSideProperty.value().length()+4) * width_16)) / 2);

                            g.drawString("<" + clientSideProperty.value() + ">", 192 + 32 + 64 + 128 + h2, 384 + 43 + 13 * 11);
                            g.setFont(monogram23);

                            continue;
                        }

                        if (PropertiesMatcher.matchKeyToString(clientSideProperty.key()) != null) {
                            if (!Objects.equals("true", clientSideProperty.value()))
                                g.drawString(PropertiesMatcher.matchKeyToString(clientSideProperty.key()) + ": " + clientSideProperty.value(), 192 + 32 + 64 + 128, 384 + 43 + 13 * y__);
                            else
                                g.drawString("(" + PropertiesMatcher.matchKeyToString(clientSideProperty.key()) + ")", 192 + 32 + 64 + 128, 384 + 43 + 13 * y__);
                            y__++;
                        }
                    }
                }

                g.setColor(Color.BLACK);
                if (monogram != null)
                    g.setFont(monogram.deriveFont((float) (19)));
                if (military_points != null)
                    g.drawImage(military_points.getScaledInstance(17, 17, Image.SCALE_FAST), 20, 10, null);
                g.drawString("" + mp, 40, 22);
                g.drawString(is_it_your_turn ? "It is your turn" : "It is not your turn", 20, 40);

                g.drawString("Camera speed: " + camera_speed, 690, 22);

                if (selector != null && do_render_prev) {
                    if (!do_offset)
                        g.drawImage(AssetLoader.load("bhighlight"), (x_prev * 64), (y_prev * 64), null);
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
                for (String msg : chat) {
                    if (monogram != null) {
                        g.setColor(Color.WHITE);
                        g.setFont(monogram.deriveFont((float) (15)).deriveFont(Font.BOLD));
                    }
                    g.drawString(msg, 500, 233 + (y * 10));
                    y++;
                }

                if (p != null) {
                    // Evil things to get JDesktopPanel to work
                    BufferedImage clone = new BufferedImage(832 + 32, 576 + 32 + 6, BufferedImage.TYPE_INT_ARGB_PRE);
                    Graphics2D graphics = clone.createGraphics();
                    if (focus_desktop_pane) {
                        graphics.setColor(new Color(0, 0, 0, 0.5F));
                        graphics.fillRect(0, 0, 832 + 32, 576 + 32 + 6);
                    }
                    p.printAll(graphics);
                    graphics.dispose();
                    g.drawImage(clone, 0, 0, null);
                }
            }
            else {
                g.setColor(Color.BLACK);
                g.drawRect(0,64, 64*13, 64*5);
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

    private void renderImages2(ArrayList<ArrayList<Image>> buffered_images, int offset_x, int offset_y, Graphics2D g2) {
        int y;
        y = 0;
        for (ArrayList<Image> row_x: buffered_images){
            int x_count = 0;
            for (Image x : row_x){
                if (x != null)
                    g2.drawImage(x, 64*(x_count-1)-offset_x, 64*(y-1)-offset_y, null);
                x_count++;
            }
            y++;
        }
    }
}
