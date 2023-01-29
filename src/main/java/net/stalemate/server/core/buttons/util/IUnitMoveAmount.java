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

package net.stalemate.server.core.buttons.util;

import net.stalemate.server.core.properties.Properties;

public interface IUnitMoveAmount {

    /***
     * Sets the amount of moves remaining for the unit this turn
     */
    void setMoveAmount(int m);

    /***
     * The amount of moves available each turn
     */
    int getTurnMoveAmount();
    int getMoveAmount();

    static void addMoveAmountProperty(int m, boolean has_turn_ended, Properties properties){
        if (has_turn_ended){
            properties.put("move_amount", String.valueOf(0));
        }else{
            properties.put("move_amount", String.valueOf(m));
        }
    }

}
