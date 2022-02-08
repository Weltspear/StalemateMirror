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
import net.stalemate.core.animation.Animation;
import net.stalemate.core.animation.AnimationController;
import net.stalemate.core.buttons.Scrap;
import net.stalemate.core.controller.Game;
import net.stalemate.core.units.util.IBuilding;
import net.stalemate.core.units.util.IConstructableBuilding;
import net.stalemate.core.util.IGameController;

import java.util.ArrayList;


public class SupplyStation extends Unit implements IBuilding, IConstructableBuilding {

    public static class ResupplyButton implements ISelectorButtonUnit{
        @Override
        public String bind() {
            return "R";
        }

        @Override
        public String texture() {
            return "assets/ui/buttons/resupply_button.png";
        }

        @Override
        public String identifier() {
            return "button_resupply";
        }

        @Override
        public void action(Unit selected_unit, Unit unit, IGameController gameController) {
            if (!unit.hasTurnEnded() && unit != selected_unit){
                if (selected_unit.unitStats().getMaxSupply() - selected_unit.unitStats().getSupply() < 10){
                    int needed_supply = selected_unit.unitStats().getMaxSupply() - selected_unit.unitStats().getSupply();

                    if (unit.unitStats().getSupply() >= needed_supply){
                        unit.consumeSupply(needed_supply);
                        selected_unit.consumeSupply(-(needed_supply));
                        unit.endTurn();
                    }
                }
                else if (unit.unitStats().getSupply() >= 30){
                    selected_unit.consumeSupply(-10);
                    unit.consumeSupply(10);
                    unit.endTurn();
                }
            }

        }

        @Override
        public int selector_range() {
            return 1;
        }

        @Override
        public String selector_texture() {
            return "assets/ui/selectors/ui_resupply.png";
        }

        @Override
        public boolean isUsedOnOurUnit() {
            return true;
        }

        @Override
        public boolean isUsedOnEnemy() {
            return false;
        }

        @Override
        public boolean isUsedOnAlliedUnit() {
            return true;
        }
    }

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
        buttons.add(new ResupplyButton());
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
        ((Game)game).getUnitsTeamIgnoreSafety(this).setMilitaryPoints(((Game)game).getUnitsTeamIgnoreSafety(this)
                .getMilitaryPoints()+1);

        supply+=5;
        if (supply > max_supply){
            supply = max_supply;
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
