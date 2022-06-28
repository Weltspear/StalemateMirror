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

package net.stalemate.core.replay;

import net.libutils.etable.EntryTable;
import net.stalemate.core.Unit;

public class Replay {

    public static class StatisticEntity{
        private final int x;
        private final int y;
        private final int su;
        private final int max_su;
        private final int hp;
        private final int max_hp;
        private final String name;
        private final String idle;

        public StatisticEntity(Unit u){
            x = u.getX();
            y = u.getY();
            hp = u.unitStats().getHp();
            max_hp = u.unitStats().getMaxHp();
            su = u.unitStats().getSupply();
            max_su = u.unitStats().getMaxSupply();
            name = u.getName();
            idle = u.getAnimationController().getAnimation("idle").copy().getFrame();
        }

        public EntryTable asEntryTable(){
            EntryTable t = new EntryTable();
            t.setInt("x", x);
            t.setInt("y", y);
            t.setInt("su", su);
            t.setInt("max_su", max_su);
            t.setInt("hp", hp);
            t.setInt("hp_su", max_hp);
            t.setString("name", name);
            t.setString("idle", idle);
            return t;
        }
    }

    public static class Turn{

    }

}
