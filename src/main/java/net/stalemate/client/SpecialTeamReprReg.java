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

package net.stalemate.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class SpecialTeamReprReg {

    private static final HashMap<String, TeamRepr> reg = new HashMap<>();

    private static final ReentrantLock lock = new ReentrantLock();

    public static class TeamRepr {
        ArrayList<int[]> coords = new ArrayList<>();

        public TeamRepr(){

        }

        public void addCoord(int x, int y){
            coords.add(new int[]{x,y});
        }

        public ArrayList<int[]> getCoords() {
            return coords;
        }
    }

    public static void makeAll(){
        TeamRepr heavyTank = new TeamRepr();
        heavyTank.addCoord(19, 16);
        heavyTank.addCoord(20, 16);
        heavyTank.addCoord(21, 16);
        heavyTank.addCoord(6, 9);
        reg.put("assets/units/heavy_tank_attack_0.png", heavyTank);
        reg.put("assets/units/heavy_tank_attack_1.png", heavyTank);
        reg.put("assets/units/heavy_tank_attack_2.png", heavyTank);
        reg.put("assets/units/heavy_tank_attack_3.png", heavyTank);
        reg.put("assets/units/heavy_tank_idle.png", heavyTank);
    }

    public static TeamRepr getTeamRepr(String n){
        lock.lock();
        try {
            return reg.get(n);
        } finally {
            lock.unlock();
        }
    }
}
