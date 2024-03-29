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

package net.stalemate.server.core;

public class MapObject {
    private final String texture_file;
    private final boolean isPassable;

    public MapObject(String texture_file, boolean isPassable){
        this.texture_file = texture_file;
        this.isPassable = isPassable;
    }

    public String getTexture() {
        return texture_file;
    }

    public boolean isPassable() {
        return isPassable;
    }
}
