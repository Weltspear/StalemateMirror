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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class InGameUI extends JPanel {
    private final HashMap<Float, HashMap<Image, Image>> scale_map_cache = new HashMap<>();
    private final ClientDataManager clDataManager;
    private final KeyboardInput in_client;
    private final JDesktopPane p;
    @SuppressWarnings("FieldCanBeLocal") private final Font monogram;
    private final Font monogram_button;
    private final Font monogram_button_small;
    private final Font uname_monogram;
    private final MListener m;
    private boolean focus_desktop_pane = false;
    private HashMap<String, String> gamemodeProperties = new HashMap<>();

    private final Image empty;

    private Image minimap = null;

    public float scale = 1f;

    private final JFrame frame;
    private boolean do_offset = false;

    private boolean hasFirstPackedBeenReceived = false;
    private Color teamDoingTurnColor = Color.WHITE;
    private String teamDoingTurnNick = "";

    public int tr_width;
    public int tr_height;

    public int[][] highlights = new int[9][];

    public int[] cur_highlight = null;

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
                    if (" qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM<>=-()[]{}\"';:.,1234567890@#$%^&*/\\?_".contains(String.valueOf(e.getKeyChar()))) {
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
                    } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V){
                        try {
                            currentMSG += Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                        } catch (UnsupportedFlavorException | IOException ex) {
                            ex.printStackTrace();
                        }
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
        private final BufferedImage motorized;
        private final BufferedImage plane;

        private final HashMap<String, Image> entity_scaled_cache = new HashMap<>();
        private final HashMap<String, Image> map_scaled_cache = new HashMap<>();
        private final HashMap<String, Image> button_scaled_cache = new HashMap<>();
        private final HashMap<BufferedImage, Image> queue_scaled_cache = new HashMap<>();
        private final HashMap<BufferedImage, Image> big_unit_texture_cache = new HashMap<>();

        // true selector x
        private int tsel_x;
        // true selector y
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
            motorized = AssetLoader.load("assets/motorized.png");
            plane = AssetLoader.load("assets/plane.png");
        }

        private BufferedImage createARGBClone(Image image){
            BufferedImage clone = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D graphics = clone.createGraphics();

            graphics.setBackground(new Color(0x00FFFFFF, true));
            graphics.clearRect(0, 0, clone.getWidth(), clone.getHeight());

            graphics.drawImage(image, 0, 0, null);

            graphics.dispose();

            return clone;
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

            if (e.hasTurnEnded() && e.getAnimationState().equals("idle")){
                cached = cached+"+turnended";
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
                    BufferedImage clone = createARGBClone(image);

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

                if (e.hasTurnEnded() && e.getAnimationState().equals("idle")){
                    BufferedImage clone = createARGBClone(image);

                    for (int y_ = 0; y_ < 32; y_++) {
                        for (int x_ = 0; x_ < 32; x_++) {

                            Color original = new Color((new Color(clone.getRGB(x_, y_))).getRed(),
                                    (new Color(clone.getRGB(x_, y_))).getGreen(),
                                    (new Color(clone.getRGB(x_, y_))).getBlue(),
                                    (new Color(clone.getRGB(x_, y_), true)).getAlpha());

                            int r = (int) Math.floor(original.getRed()*0.9);
                            int g = (int) Math.floor(original.getRed()*0.9);
                            int b = (int) Math.floor(original.getRed()*0.9);

                            clone.setRGB(x_, y_, (new Color(r < 256 ? r: 255,
                                    g < 256 ? r: 255,
                                    b < 256 ? r: 255,
                                    original.getAlpha())).getRGB());

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
                               boolean is_it_your_turn, ClientMapLoader clMapLoader, Image minimap, Color teamDoingTurnColor,
                               String teamDoingTurnNick, HashMap<String, String> gamemodeProperties) {
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

                ArrayList<ArrayList<String>> map_textures = mapLoader.getMap(this.interface_.cam_x, this.interface_.cam_y, this.interface_.scale, (interface_.tr_width-832)/64, (interface_.tr_height-576)/64);

                ArrayList<Image> buttons = new ArrayList<>();
                ArrayList<String> binds = new ArrayList<>();
                int[][] highlights = new int[9][];

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
                    int bi = 0;
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

                            if (button.isHighlightEnabled()){
                                highlights[bi] = button.getHighlight();
                            }

                            binds.add(button.getBind());
                        } else {
                            buttons.add(null);
                            binds.add(null);
                        }

                        bx++;
                        bi++;
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
                                    0, ucentity.getOther(), ucentity.getOtherTeamRGB()};

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

                                // Draw "other"
                                if (ucentity.getOther() != -1){
                                    BufferedImage other = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB_PRE);
                                    Graphics2D other_graphics = other.createGraphics();
                                    other_graphics.setBackground(new Color(0x00FFFFFF, true));
                                    other_graphics.drawImage(ucentity.getOther() == 1 ? plane : motorized, 0, 0, null);

                                    for (int y_ = 0; y_ < 16; y_++) {
                                        for (int x_ = 0; x_ < 16; x_++) {
                                            if (other.getRGB(x_, y_) == Color.BLACK.getRGB()) {
                                                other.setRGB(x_, y_, ucentity.getOtherTeamRGB().getRGB());
                                            }
                                        }
                                    }

                                    graphics.drawImage(other,46, 1, null);
                                }

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
                setSelectorData(tsel_x, tsel_y);

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
                this.interface_.gamemodeProperties = gamemodeProperties;
                this.interface_.highlights = highlights;
                if (!teamDoingTurnColor.equals(Color.WHITE))
                    this.interface_.teamDoingTurnColor = teamDoingTurnColor;
                if (!Objects.equals(teamDoingTurnNick, "neutralteam")){
                    this.interface_.teamDoingTurnNick = teamDoingTurnNick;
                }
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

            int x_c = tsel_x-interface_.cam_x;
            int y_c = tsel_y-interface_.cam_y;

            int sel_x_frame = interface_.createCorrectXFromXC(x_c);
            int sel_y_frame = interface_.createCorrectYFromYC(y_c);

            this.interface_.sel_x_frame = sel_x_frame;
            this.interface_.sel_y_frame = sel_y_frame;
            interface_.unsafeLock.unlock();
        }

    }

    class MListener extends MouseAdapter{
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            super.mouseWheelMoved(e);
            unsafeLock.lock();

            if (e.getWheelRotation() == 1){
                scale -= 0.1f;
                if (scale < 0.5){
                    scale = 0.5f;
                }
            } else{
                scale += 0.1f;
                if (scale > 2){
                    scale = 2;
                }
            }

            // trick to fix prev selector when changing scale
            mouseMoved(e);
            unsafeLock.unlock();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            unsafeLock.lock();
            if (!focus_desktop_pane) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    int y = 0;
                    int x = 0;
                    int button_idx = 0;
                    for (String bind : binds) {
                        if (e.getX() >= rightPanelX() + (x * 64) && e.getY() >= rightPanelY() + (y * 64)
                                && e.getX() <= rightPanelX() + 64 + (x * 64) && e.getY() <= rightPanelY() + 64 + (y * 64)) {
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

                    // Move selector
                    if (offset_direction == OffsetDirection.None) {
                        if ((e.getX() >= 0 && e.getX() <= tr_width) &&
                                (e.getY() >= 64 && e.getY() <= rightPanelY())) {

                            int x_c = calculateXOnCamera(e.getX());
                            int y_c = calculateYOnCamera(e.getY());

                            int right_mv = clDataManager.tsel_x-(cam_x+x_c);
                            int down_mv = clDataManager.tsel_y-(cam_y+y_c);

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
                }
                else {
                    in_client.keysInQueue.add("ENTER");
                }
            }
            unsafeLock.unlock();
        }

        public int calculateXOnCamera(int x_m){
            return (int)(Math.ceil(Math.ceil((x_m+Math.ceil(offset_x/scale))/Math.ceil(64/scale))))-1;
        }

        public int calculateYOnCamera(int y_m){
            return (int)(Math.ceil(Math.ceil((y_m-64+Math.ceil(offset_y/scale))/Math.ceil(64/scale))))-1;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            unsafeLock.lock();
            cur_highlight = null;
            if (!focus_desktop_pane) {
                boolean clearTooltip = false;
                int y = 0;
                for (String[] row : id_array) {
                    int x = 0;
                    for (String point : row) {
                        if (e.getX() >= rightPanelX() + (x * 64) && e.getY() >= rightPanelY() + (y * 64)
                                && e.getX() <= rightPanelX() + 64 + (x * 64) && e.getY() <= rightPanelY() + 64 + (y * 64)) {
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
                    if (e.getX() >= tr_width - 64 && e.getY() >= 128 && e.getY() <= rightPanelY() - 64) {
                        offset_direction = OffsetDirection.Right;
                    } else if (e.getX() <= 64 && e.getY() >= 128 && e.getY() <= rightPanelY() - 64) {
                        offset_direction = OffsetDirection.Left;
                    } else if (e.getX() <= tr_width && e.getY() >= rightPanelY()-64 && e.getY() <= rightPanelY()) {
                        offset_direction = OffsetDirection.Down;
                    } else if (e.getX() <= tr_width && e.getY() >= 64 && e.getY() <= 128) {
                        offset_direction = OffsetDirection.Up;
                    } else {
                        offset_direction = OffsetDirection.None;
                    }
                }
                else {
                    offset_direction = OffsetDirection.None;
                }

                if (offset_direction == OffsetDirection.None)
                    if ((((64 < e.getY()) && (e.getY() < rightPanelY())) && ((0 < e.getX()) && (e.getX() < tr_width))) || (((rightPanelX() < e.getX()) && (e.getX() < tr_width)) && ((rightPanelY() < e.getY()) && (e.getY() < tr_height)))) {
                        if (((64 < e.getY()) && (e.getY() < rightPanelY())) && ((0 < e.getX()) && (e.getX() < tr_width))) {
                            int x_c = calculateXOnCamera(e.getX());
                            int y_c = calculateYOnCamera(e.getY());

                            InGameUI.this.x_prev = createCorrectXFromXC(x_c);
                            InGameUI.this.y_prev = createCorrectYFromYC(y_c);

                            InGameUI.this.do_render_prev = true;
                            InGameUI.this.do_offset = true;
                        }
                        else {
                            InGameUI.this.x_prev = ((e.getX()) / (64));
                            InGameUI.this.y_prev = ((e.getY()) / (64));
                            InGameUI.this.do_render_prev = true;
                            InGameUI.this.do_offset = false;

                            int hxb = x_prev-(rightPanelX()/64);
                            int hyb = y_prev-(rightPanelY()/64);
                            int hidx = (hyb*3)+hxb;

                            if (highlights[hidx] != null){
                                cur_highlight = new int[]{
                                        createCorrectXFromXC(highlights[hidx][0]-cam_x),
                                        createCorrectYFromYC(highlights[hidx][1]-cam_y),
                                        highlights[hidx][2]
                                };
                            }
                            else{
                                cur_highlight = null;
                            }
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

    /***
     * @param x_c x on camera space
     */
    public int createCorrectXFromXC(int x_c) {
        return (int) Math.ceil(x_c * (int) Math.ceil(64 / scale)) + (int) Math.ceil(-offset_x / scale);
    }

    /***
     * @param y_c y on camera space
     */
    public int createCorrectYFromYC(int y_c) {
        return (int) Math.ceil(y_c * (int) Math.ceil(64 / scale)) + (int) Math.ceil(-(offset_y) / scale);
    }

    public InGameUI(JFrame frame, int width, int height){
        super(null);
        ButtonTooltips.init();
        PropertiesMatcher.loadPropertyMatcher();
        clDataManager = new ClientDataManager(this);

        tr_width = width;
        tr_height = height;

        this.frame = frame;
        this.frame.setMinimumSize(new Dimension(tr_width+14,tr_height+32+6));
        this.frame.setSize(new Dimension(tr_width+32,tr_height+32+6));
        this.m = new MListener();

        this.addMouseMotionListener(m);
        this.addMouseListener(m);
        this.addMouseWheelListener(m);

        in_client = new KeyboardInput();
        this.addKeyListener(in_client);
        this.requestFocusInWindow();

        p = new JDesktopPane();
        p.setBackground(new Color(0x00FFFFFF, true));
        p.setVisible(true);
        p.setSize(new Dimension(tr_width+14,tr_height));
        p.setPreferredSize(new Dimension(tr_width+14,tr_height));
        p.setFocusable(true);
        this.add(p);

        this.setMinimumSize(new Dimension(tr_width+14,tr_height+32+6));
        this.setSize(new Dimension(tr_width+32,tr_height+32+6));

        this.frame.setResizable(false);
        this.frame.add(this);

        this.frame.pack();

        monogram = AssetLoader.getMonogram();
        monogram_button = monogram.deriveFont(((float)(16)));
        monogram_button_small = monogram.deriveFont(((float)(14.4)));
        uname_monogram = monogram.deriveFont(Font.PLAIN, 16f);

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
        this.removeMouseWheelListener(m);
        frame.remove(this);
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
        this.addMouseWheelListener(m);
        this.requestFocus();
    }

    private void prepareDesktopPane(){
        this.removeKeyListener(in_client);
        this.removeMouseListener(m);
        this.removeMouseMotionListener(m);
        this.removeMouseWheelListener(m);
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

    public void drawUITiles(Graphics g){
        // Top panel
        BufferedImage top_1 = AssetLoader.load("assets/ui_tile/top_1.png");
        BufferedImage top_2 = AssetLoader.load("assets/ui_tile/top_2.png");
        BufferedImage top_3 = AssetLoader.load("assets/ui_tile/top_3.png");
        // Bottom panel
        BufferedImage bot_top_fill_1 = AssetLoader.load("assets/ui_tile/bottom_top_fill.png");
        BufferedImage bot_fill_2 = AssetLoader.load("assets/ui_tile/bottom_fill_2.png");
        BufferedImage bot_left_end = AssetLoader.load("assets/ui_tile/bottom_left_end.png");
        BufferedImage bot_top_transition =  AssetLoader.load("assets/ui_tile/bottom_top_transition.png");
        BufferedImage bot_fill_1 =  AssetLoader.load("assets/ui_tile/bottom_fill_1.png");
        BufferedImage bot_right_top_corner =  AssetLoader.load("assets/ui_tile/bottom_right_top_corner.png");
        BufferedImage bot_transition =  AssetLoader.load("assets/ui_tile/bottom_transition.png");
        BufferedImage bot_top_fill_2 =  AssetLoader.load("assets/ui_tile/bottom_top_fill_2.png");
        BufferedImage bot_right_end =  AssetLoader.load("assets/ui_tile/bottom_right_end.png");
        BufferedImage bot_top_left_corner =  AssetLoader.load("assets/ui_tile/bottom_top_left_corner.png");

        int twidth = tr_width/64;
        int theight = tr_height/64;

        for (int x = 0; x < twidth; x++){
            if (x == 0){
                g.drawImage(top_1, 0, 0, null);
            }
            else if (x == twidth-1){
                g.drawImage(top_3, (x*64)-1, 0, null);
            }
            else{
                g.drawImage(top_2, (x*64)-1, 0, null);
            }
        }

        int y = theight-3; // first y index

        int transition_x = twidth-13+6;
        boolean istransition = false;

        for (int i = 0; i < 3; i++){
            for (int x = 0; x < twidth; x++){
                if (x == 0){
                    if (i == 0){
                        g.drawImage(bot_top_left_corner, 0, (y+i)*64, null);
                    }
                    else {
                        g.drawImage(bot_left_end, 0, (y+i)*64, null);
                    }
                }
                else if (x == twidth - 1){
                    if (i == 0){
                        g.drawImage(bot_right_top_corner, x*64, (y+i)*64, null);
                    }
                    else {
                        g.drawImage(bot_right_end, x*64, (y+i)*64, null);
                    }
                }
                else{
                    if (i == 0){
                        if (x == transition_x){
                            g.drawImage(bot_top_transition, x*64, (y+i)*64, null);
                            istransition = true;
                        }
                        else if (!istransition){
                            g.drawImage(bot_top_fill_1, x*64, (y+i)*64, null);
                        }
                        else {
                            g.drawImage(bot_top_fill_2, x*64, (y+i)*64, null);
                        }
                    }
                    else {
                        if (x == transition_x){
                            g.drawImage(bot_transition, x*64, (y+i)*64, null);
                            istransition = true;
                        }
                        else if (!istransition){
                            g.drawImage(bot_fill_1, x*64, (y+i)*64, null);
                        }
                        else {
                            g.drawImage(bot_fill_2, x*64, (y+i)*64, null);
                        }
                    }
                }
            }
            istransition = false;
        }

        // Draw panels
        g.drawImage(panel, (twidth-3)*64, y*64, null);
        g.drawImage(panel, 0, y*64, null);
    }

    public int rightPanelX(){
        return ((tr_width/64)-3)*64;
    }

    public int rightPanelY(){
        return ((tr_height/64)-3)*64;
    }

    public int rightPanelXEnd(){
        return rightPanelX() + 64*3;
    }

    public int rightPanelYEnd(){
        return rightPanelY() + 64*3;
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
            drawUITiles(g);

            if (hasFirstPackedBeenReceived) {
                BufferedImage bufferedImage = new BufferedImage(tr_width, tr_height-4*64, BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D g2 = bufferedImage.createGraphics();

                int y;

                renderImagesScale(map_to_render, offset_x, offset_y, scale, g2);
                renderImagesScale(entity_render, offset_x, offset_y, scale, g2);
                renderImagesScale(fog_of_war, offset_x, offset_y, scale, g2);
                renderImagesScale(unit_data_ar, offset_x, offset_y, scale, g2);
                renderImagesScale(_selr, offset_x, offset_y, scale, g2);

                if (cur_highlight != null){

                    BufferedImage h = new BufferedImage((int)Math.ceil(64/scale), (int)Math.ceil(64/scale), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g3 = h.createGraphics();
                    g3.setColor(new Color(cur_highlight[2], true));
                    g3.fillRect(0, 0, (int)Math.ceil(64/scale), (int)Math.ceil(64/scale));
                    g3.dispose();

                    g2.drawImage(h, cur_highlight[0], cur_highlight[1], null);
                }

                // Preview selector
                if (selector != null && do_render_prev && do_offset) {
                    g2.drawImage(selector.getScaledInstance((int)Math.ceil(64/scale), (int)Math.ceil(64/scale), Image.SCALE_SMOOTH), x_prev, y_prev, null);
                }

                // Normal Selector
                if (selector != null)
                    g2.drawImage(selector.getScaledInstance((int)Math.ceil(64/scale), (int)Math.ceil(64/scale), Image.SCALE_SMOOTH), sel_x_frame, sel_y_frame, null);

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
                            g.drawImage(button.getScaledInstance(57, 57, Image.SCALE_FAST), rightPanelX() + x + 3, rightPanelY() + y + 3, null);
                            if (Grass32ConfigClient.doSteelButtonOverlay())
                                g.drawImage(AssetLoader.load("assets/ui/buttons/steel_button_overlay_2.png").getScaledInstance(57, 57, Image.SCALE_FAST), rightPanelX() + x + 3, rightPanelY() + y + 3, null);
                        }
                        else {
                            g.drawImage(button, rightPanelX() + x, rightPanelY() + y, null);
                            if (Grass32ConfigClient.doSteelButtonOverlay())
                                g.drawImage(AssetLoader.load("assets/ui/buttons/steel_button_overlay_2.png"), rightPanelX() + x, rightPanelY() + y, null);
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
                    g.drawImage(minimap, 16, rightPanelY()+16, null);

                // Render the queue
                if (queue != null) {
                    i = 0;
                    x = 0;
                    y = 0;
                    for (Image m : queue) {
                        if (m != null) {
                            g.drawImage(m, x, rightPanelY() + y, null);
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
                            g.drawString(bind, rightPanelX() + x + 8 + 3, rightPanelY() - 1 + y + 11 + 3);
                            smallify_button_renders--;
                            if (smallify_button_renders == 0)
                                smallify_button = -1;
                            g.setFont(monogram_button);
                        }
                        else{
                            g.drawString(bind, rightPanelX() + x + 8, rightPanelY() - 1 + y + 11);
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
                        g.drawString(bind, x + 8, rightPanelY() - 1 + y + 11);
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
                    g.drawImage(unit_img, rightPanelX() - 7*64 + 32, rightPanelY() + 32, null);

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
                    FontMetrics metrics = g.getFontMetrics(monogram.deriveFont((float) (32)).deriveFont(Font.BOLD));
                    int width = metrics.stringWidth("A");

                    if (name == null) {
                        name = new ClientSideProperty("name", "");
                    }

                    int h = ((224 - (name.value().length() * width)) / 2);

                    g.drawString(name.value(), rightPanelX() - 64*7 + 32 + 64 + 128 + h, rightPanelY() + 30); // 416-640

                    Font monogram23 = monogram.deriveFont(Font.PLAIN, 23f);

                    g.setFont(monogram23);

                    int y__ = 1;
                    for (ClientSideProperty clientSideProperty : propertiesToRender2.properties) {
                        if (clientSideProperty.key().equals("uname")){
                            if (clientSideProperty.value().isEmpty()){
                                continue;
                            }
                            g.setFont(uname_monogram);

                            FontMetrics metrics_16 = g.getFontMetrics();
                            int width_16 = metrics_16.stringWidth("A");

                            int h2 = ((224 - ((clientSideProperty.value().length()+4) * width_16)) / 2);

                            g.drawString("<" + clientSideProperty.value() + ">", rightPanelX() - 64*7 + 32 + 64 + 128 + h2, rightPanelY() + 43 + 13 * 11);
                            g.setFont(monogram23);

                            continue;
                        }

                        if (PropertiesMatcher.matchKeyToString(clientSideProperty.key()) != null) {
                            if (!Objects.equals("true", clientSideProperty.value()))
                                g.drawString(PropertiesMatcher.matchKeyToString(clientSideProperty.key()) + ": " + clientSideProperty.value(), rightPanelX() - 64*7 + 32 + 64 + 128, rightPanelY() + 43 + 13 * y__);
                            else
                                g.drawString("(" + PropertiesMatcher.matchKeyToString(clientSideProperty.key()) + ")", rightPanelX() - 64*7 + 32 + 64 + 128, rightPanelY() + 43 + 13 * y__);
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
                g.drawString("Turn: ", 20, 40);
                g.setColor(teamDoingTurnColor);
                g.fillRect(55, 33, 8, 8);
                g.setColor(Color.black);
                g.drawString(Objects.requireNonNullElse(teamDoingTurnNick, "BOT"), 67, 40);

                g.drawString("Camera speed: " + camera_speed, rightPanelX() + 50, 22);
                g.setColor(new Color(96, 39, 2));
                g.fillRoundRect(rightPanelX() - 156, 6, 162,36, 5, 5);
                g.setColor(new Color(198, 130, 77));

                int y_count = 0;
                int x_count = 0;
                for (Map.Entry<String, String> entry: gamemodeProperties.entrySet()){
                    g.drawString(PropertiesMatcher.matchKeyToString(entry.getKey()) + ": " + entry.getValue(), rightPanelX()-150+x_count*50, 22+y_count*10);
                    y_count++;
                    if (y_count == 2){
                        x_count++;
                        y_count = 0;
                    }
                }

                g.setColor(Color.BLACK);

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

                        int offset_rect = g.getFontMetrics().stringWidth(m);
                        g.drawString(m, rightPanelX()-140, rightPanelY() - 1 - 40);

                        g.fillRect(rightPanelX()-140+offset_rect, rightPanelY() - 1 - 40 -g.getFontMetrics().getHeight()+2, g.getFontMetrics().stringWidth(" "), g.getFontMetrics().getHeight());
                    }

                // Render chat
                y = 0;
                for (String msg : chat) {
                    if (monogram != null) {
                        g.setColor(Color.WHITE);
                        g.setFont(monogram.deriveFont((float) (15)).deriveFont(Font.BOLD));
                    }
                    g.drawString(msg, rightPanelX() - 140, rightPanelY() - 151 + (y * 10));
                    y++;
                }

                if (p != null) {
                    if (focus_desktop_pane) {
                        g.setColor(new Color(0, 0, 0, 0.5F));
                        g.fillRect(0, 0, tr_width, tr_height);
                    }
                    p.printAll(g);
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

    private<T extends Image> void renderImagesScale(ArrayList<ArrayList<T>> buffered_images, int offset_x, int offset_y, float scale, Graphics2D g2) {
        int y;
        y = 0;
        for (ArrayList<T> row_x: buffered_images){
            int x_count = 0;

            for (T x : row_x){
                if (x != null)
                    g2.drawImage(getScaledImage(x, 1/scale), (x_count-1)*(int)Math.ceil(64/scale)+(int)Math.ceil(-offset_x/scale), (y-1)*(int)Math.ceil(64/scale)+(int)Math.ceil(-offset_y/scale), null);
                x_count++;
            }
            y++;
        }
    }

    // due to weird issues encountered with java scaling this has to be here
    private Image scaleImage(Image img, int width, int height){
        BufferedImage n = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        n.getGraphics().drawImage(img, 0, 0, null);

        // this essentially replicates functionality of getScaledInstance but takes BufferedImage's getSource
        // and that fixes weird scaling issues
        AreaAveragingScaleFilter areaAveragingScaleFilter = new AreaAveragingScaleFilter(width, height);
        ImageProducer prod = new FilteredImageSource(n.getSource(), areaAveragingScaleFilter);
        return Toolkit.getDefaultToolkit().createImage(prod);
    }

    private Image getScaledImage(Image img, float scale){
        if (scale == 1){
            return img;
        }

        if (!scale_map_cache.containsKey(scale)){
            scale_map_cache.put(scale, new HashMap<>());
        }

        if (!scale_map_cache.get(scale).containsKey(img)){
            scale_map_cache.get(scale).put(img, scaleImage(img ,(int) Math.ceil(64*scale), (int) Math.ceil(64*scale)));
        }

        return scale_map_cache.get(scale).get(img);
    }
}
