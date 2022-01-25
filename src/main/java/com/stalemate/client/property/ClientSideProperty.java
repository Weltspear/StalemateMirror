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

package com.stalemate.client.property;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record ClientSideProperty(String key, String value){
    public ClientSideProperty(String[] property){
        this(property[0], property[1]);
    }

    @Contract(value = " -> new", pure = true)
    public String @NotNull [] asStringArray(){
        return new String[]{key, value};
    }
}
