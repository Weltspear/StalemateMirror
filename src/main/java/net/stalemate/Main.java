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

package net.stalemate;

import net.stalemate.client.SpecialTeamReprReg;
import net.stalemate.client.ui.MainMenu;
import net.stalemate.client.AssetLoader;
import net.stalemate.server.Server;

import java.awt.*;
import java.io.File;

public class Main {
    public static void main(String[] args) throws IllegalAccessException {
        File f = new File("grass32");
        if (!f.exists()){
            throw new IllegalAccessException("grass32 doesn't exist");
        }

        if (args.length > 0){
            if (args[0].equals("--serv")){
                Server server = new Server();
                server.start_server();
            }
        }
        else {
            //System.setProperty("sun.java2d.opengl", "true");
            AssetLoader.loadAll();
            SpecialTeamReprReg.makeAll();

            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(AssetLoader.getMonogram());

            MainMenu mm = new MainMenu();
            while (true) {
                mm.update();
            }
        }
    }
}
