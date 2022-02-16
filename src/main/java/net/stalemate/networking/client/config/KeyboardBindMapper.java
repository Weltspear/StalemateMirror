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

import com.sun.source.tree.BreakTree;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;

public class KeyboardBindMapper {

    public static int move_up = 0;
    public static int move_left = 0;
    public static int move_right = 0;
    public static int move_down = 0;

    public static int confirm = 0;
    public static int escape = 0;
    public static int finish_turn = 0;

    public static int change_cam_sel_mode = 0;
    public static int goto_first_built_base = 0;
    public static int chat = 0;

    public static void makeBinds(HashMap<String, String> binds){
        move_up = AWTKeyStroke.getAWTKeyStroke(binds.get("move_up")).getKeyCode();
        move_left = AWTKeyStroke.getAWTKeyStroke(binds.get("move_left")).getKeyCode();
        move_right = AWTKeyStroke.getAWTKeyStroke(binds.get("move_right")).getKeyCode();
        move_down = AWTKeyStroke.getAWTKeyStroke(binds.get("move_down")).getKeyCode();

        confirm = AWTKeyStroke.getAWTKeyStroke(binds.get("confirm")).getKeyCode();
        escape = AWTKeyStroke.getAWTKeyStroke(binds.get("escape")).getKeyCode();
        finish_turn = AWTKeyStroke.getAWTKeyStroke(binds.get("finish_turn")).getKeyCode();

        change_cam_sel_mode = AWTKeyStroke.getAWTKeyStroke(binds.get("change_cam_sel_mode")).getKeyCode();
        goto_first_built_base = AWTKeyStroke.getAWTKeyStroke(binds.get("goto_first_built_base")).getKeyCode();
        chat = AWTKeyStroke.getAWTKeyStroke(binds.get("chat")).getKeyCode();
    }

}
