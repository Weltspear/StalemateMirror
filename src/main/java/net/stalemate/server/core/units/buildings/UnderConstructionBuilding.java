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

package net.stalemate.server.core.units.buildings;

import net.stalemate.server.core.Unit;
import net.stalemate.server.core.animation.AnimationController;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.units.util.IBuilding;
import net.stalemate.server.core.controller.Game;

public class UnderConstructionBuilding extends Unit implements IBuilding {
    private final Unit outBuilding;
    private int constructionTime;
    private final int initialConstructionTime;

    /***
     * @param outBuilding Building to be deployed. It has to have the same coordinates as this entity
     */
    public UnderConstructionBuilding(int x, int y, Game g, Unit outBuilding, int constructionTime, AnimationController a) {
        super(x, y, g, new UnitStats(1, outBuilding.unitStats().getMaxHp(), 0, 0, 0, 0, 0, -1, 0, 0, 0), a, outBuilding.getName());
        this.outBuilding = outBuilding;
        this.constructionTime = constructionTime;
        this.initialConstructionTime = constructionTime;
        this.move_amount = -1;
    }

    @Override
    public void endTurn() {
        super.endTurn();
        constructionTime--;

        this.hp += (int)((1/(float)initialConstructionTime)*((float)(outBuilding.getMaxHp())));

        if (hp > max_hp){hp=max_hp;}

        if (constructionTime == 0){
            game.rmEntity(this);
            game.getUnitsTeam(this).rmUnit(this);
            game.addEntity(outBuilding);
        }

        if (hp <= 0){
            outBuilding.setHp(-1);
        }
    }

    @Override
    public IButton[] getButtons() {
        return new IButton[9];
    }

    @Override
    public Properties getProperties() {
        Properties p = super.getProperties();
        p.rm("ended_turn");
        p.put("construction_time", String.valueOf(constructionTime));
        return p;
    }

    @Override
    public Unit shiftSelectionOnRemoval() {
        if (constructionTime == 0 && hp > 0){
            return outBuilding;
        }
        else{
            return null;
        }
    }
}
