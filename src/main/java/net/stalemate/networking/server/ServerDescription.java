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

package net.stalemate.networking.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ServerDescription {
    private static String desc = null;

    public static boolean loadDesc(){
        try {
            List<String> desc_l = Files.readAllLines(Paths.get("config/server_desc.txt"));
            StringBuilder desc_b = new StringBuilder();

            for (String line: desc_l){
                desc_b.append(line);
                // a bit cursed <br> here
                desc_b.append("<br>");
            }
            desc = desc_b.toString();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String getDescription(){
        return desc;
    }
}
