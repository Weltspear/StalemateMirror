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

import net.stalemate.server.core.Entity;
import net.stalemate.server.core.Unit;
import net.stalemate.server.core.animation.Animation;
import net.stalemate.server.core.animation.AnimationController;
import net.stalemate.server.core.buttons.Scrap;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.units.util.IBuilding;
import net.stalemate.server.core.units.util.IConstructableBuilding;
import net.stalemate.server.core.util.IGameController;

import java.util.ArrayList;


public class SupplyStation extends Unit implements IBuilding, IConstructableBuilding {
    public SupplyStation(int x, int y, IGameController game) {
        super(x, y, game, new UnitStats(10, 10, 0, 0, 0, 0, 0, 30, 0, 0, 0), new AnimationController(), "Supply Station");

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

        for (int _y = y-2; _y <= y+2; _y++){
            for (int _x = x-2; _x <= x+2; _x++){
                coords_to_resupply.add(new Integer[]{_x, _y});
            }
        }

        for (Integer[] c: coords_to_resupply){
            for (Entity e: game.getEntities(c[0], c[1])){
                if (e instanceof Unit u){
                    if (!(e instanceof SupplyStation)) {
                        if ((game.getUnitsTeam(u) == game.getUnitsTeam(this))) {
                            int needed_supply = u.unitStats().getMaxSupply() - u.unitStats().getSupply();
                            if (this.getSupply() - needed_supply > 1) {
                                u.consumeSupply(-needed_supply);
                                this.consumeSupply(needed_supply);
                            }
                        }
                    }
                }
            }
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

    @Override
    public Properties getProperties() {
        Properties p = super.getProperties();
        p.rm("ended_turn");
        return p;
    }
}
