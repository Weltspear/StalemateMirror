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

import net.stalemate.core.Entity;
import net.stalemate.core.Unit;
import net.stalemate.core.animation.Animation;
import net.stalemate.core.animation.AnimationController;
import net.stalemate.core.buttons.ResupplyButton;
import net.stalemate.core.buttons.Scrap;
import net.stalemate.core.controller.Game;
import net.stalemate.core.units.util.IBuilding;
import net.stalemate.core.units.util.IConstructableBuilding;
import net.stalemate.core.util.IGameController;

import java.util.ArrayList;


public class SupplyStation extends Unit implements IBuilding, IConstructableBuilding {
    public SupplyStation(int x, int y, IGameController game) {
        super(x, y, game, new UnitStats(10, 10, 0, 0, 0, 0, 0, 30, 0), new AnimationController(), "Supply Station");

        Animation a = new Animation(5);
        a.addFrame("assets/units/supply_station.png");
        anim.addAnimation("idle", a);
        anim.setCurrentAnimation("idle");
    }

    @Override
    public ArrayList<IButton> getButtons() {
        ArrayList<IButton> buttons = new ArrayList<>();
        buttons.add(new Scrap());
        return buttons;
    }

    @Override
    public void update() {
        anim.tick();

        if (hp <= 0){
            game.rmEntity(this);
            game.getEvReg().triggerUnitDeathEvent(this);
        }
    }

    @Override
    public void turnUpdate() {
        super.turnUpdate();

        supply+=7;
        if (supply > max_supply){
            supply = max_supply;
        }

        ArrayList<Integer[]> coords_to_resupply = new ArrayList<>();
        coords_to_resupply.add(new Integer[]{x-1, y});
        coords_to_resupply.add(new Integer[]{x+1, y});
        coords_to_resupply.add(new Integer[]{x, y+1});
        coords_to_resupply.add(new Integer[]{x, y-1});
        coords_to_resupply.add(new Integer[]{x+1, y+1});
        coords_to_resupply.add(new Integer[]{x-1, y+1});
        coords_to_resupply.add(new Integer[]{x+1, y-1});
        coords_to_resupply.add(new Integer[]{x-1, y-1});

        for (Integer[] c: coords_to_resupply){
            for (Entity e: game.getEntities(c[0], c[1])){
                if (e instanceof Unit u){
                    if ((game.getUnitsTeam(u) == game.getUnitsTeam(this))){
                        int needed_supply = u.unitStats().getMaxSupply() - u.unitStats().getSupply();
                        if (this.getSupply() - needed_supply > 1){
                            u.consumeSupply(-needed_supply);
                            this.consumeSupply(needed_supply);
                        }
                    }
                }
            }
        }

        if (supply - 20 >= 0) {
            game.getUnitsTeam(this).setMilitaryPoints(game.getUnitsTeam(this)
                    .getMilitaryPoints() + 1);
            supply -= 20;
        }
    }

    @Override
    public AnimationController underConstructionAC() {
        AnimationController anim = new AnimationController();
        Animation idle = new Animation(1);
        idle.addFrame("assets/units/supply_station_build.png");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");
        return anim;
    }
}
