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

package net.stalemate.core.units.buildings;

import net.stalemate.core.Unit;
import net.stalemate.core.animation.AnimationController;
import net.stalemate.core.properties.Properties;
import net.stalemate.core.units.util.IBuilding;
import net.stalemate.core.util.IGameController;

import java.util.ArrayList;

public class UnderConstructionBuilding extends Unit implements IBuilding {
    private final Unit outBuilding;
    private int constructionTime;

    /***
     * @param outBuilding Building to be deployed. It has to have the same coordinates as this entity
     */
    public UnderConstructionBuilding(int x, int y, IGameController g, Unit outBuilding, int constructionTime, AnimationController a) {
        super(x, y, g, new UnitStats(1, outBuilding.unitStats().getMaxHp(), 0, 0, 0, 0, 0, -1, 0, 0, 0), a, outBuilding.getName());
        this.outBuilding = outBuilding;
        this.constructionTime = constructionTime;
    }

    @Override
    public void endTurn() {
        super.endTurn();
        constructionTime--;

        if (constructionTime == 0){
            game.rmEntity(this);
            game.addEntity(outBuilding);
        }

        if (hp <= 0){
            outBuilding.setHp(-1);
        }
    }

    @Override
    public ArrayList<IButton> getButtons() {
        return new ArrayList<>();
    }

    @Override
    public Properties getProperties() {
        Properties p = super.getProperties();
        p.rm("ended_turn");
        p.put("construction_time", "" + constructionTime);
        return p;
    }
}
