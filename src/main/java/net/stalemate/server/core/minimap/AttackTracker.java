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

package net.stalemate.server.core.minimap;

import net.stalemate.server.core.Unit;
import net.stalemate.server.core.event.EventListener;
import net.stalemate.server.core.event.OnEvent;

import java.util.ArrayList;

/***
 * Tracks all combat in current turn.
 * Used for minimap blinking.
 */
public class AttackTracker implements EventListener {
    private ArrayList<int[]> combat_coords = new ArrayList<>();

    @OnEvent(type = OnEvent.EventType.ON_UNIT_ATTACK)
    public void onAttack(Unit attacker, Unit attacked_unit){
        combat_coords.add(new int[]{attacked_unit.getX(), attacked_unit.getY()});
    }

    public void turnUpdate(){
        combat_coords = new ArrayList<>();
    }

    public ArrayList<int[]> getCombatCoords(){
        return combat_coords;
    }

    public long getCombatCoordsHash(){
        return combat_coords.hashCode();
    }

}
