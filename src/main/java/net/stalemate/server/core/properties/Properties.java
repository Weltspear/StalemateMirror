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

package net.stalemate.server.core.properties;

import java.util.ArrayList;

public class Properties {
    private final ArrayList<Property> properties = new ArrayList<>();

    public Properties(){

    }

    public Properties put(String key, String value){
        properties.add(new Property(key, value));
        return this;
    }

    public Properties rm(String key){
        Property rm = null;
        for (Property p: properties){
            if (p.key().equals(key)){
                rm = p;
                break;
            }
        }
        if (rm != null)
        properties.remove(rm);
        return this;
    }

    public ArrayList<String[]> serialize(){
        ArrayList<String[]> properties = new ArrayList<>();
        for (Property p: this.properties){
            properties.add(p.asStringArray());
        }
        return properties;
    }
}
