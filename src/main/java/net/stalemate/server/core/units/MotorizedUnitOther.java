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

package net.stalemate.server.core.units;

import net.stalemate.server.core.Unit;
import net.stalemate.server.core.animation.Animation;
import net.stalemate.server.core.animation.AnimationController;
import net.stalemate.server.core.buttons.MoveButton;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.units.util.IUnitName;
import net.stalemate.server.core.controller.Game;

public class MotorizedUnitOther extends Unit implements IUnitName {
    private final Unit contained_unit;

    @Override
    public String getUnitName() {
        if (contained_unit instanceof IUnitName uname)
            return uname.getUnitName();
        else
            return "";
    }

    @Override
    public void setUnitName(String n) {

    }

    public class DemotorizeButton implements IStandardButton{
        @Override
        public String bind() {
            return "Q";
        }

        @Override
        public String texture() {
            return "assets/ui/buttons/demotorize.png";
        }

        @Override
        public String identifier() {
            return "button_motorized_unit_other_demotorize";
        }

        @Override
        public void action(Unit unit, Game gameController) {
            if (!hasTurnEnded){
                gameController.rmEntity(MotorizedUnitOther.this);
                contained_unit.setHp(hp);
                contained_unit.setSupply(supply);
                contained_unit.setX(x);
                contained_unit.setY(y);
                gameController.addEntity(contained_unit);
                MotorizedUnitOther.this.setHp(-1);
            }
        }
    }

    public MotorizedUnitOther(int x, int y, Game game, Unit other) {
        super(x, y, game, new UnitStats(other.getHp(), other.getMaxHp(), 0, 3,0,0,
                        other.getSupply(), other.getMaxSupply(),0, 0, 0), new AnimationController(), other.getName());

        Animation idle = new Animation(1);
        idle.addFrame("assets/units/motorized_unit_idle.png");

        contained_unit = other;

        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");

        fog_of_war_range = 3;

        move_amount = 2;
        turn_move_amount = 2;
    }

    @Override
    public IButton[] getButtons() {
        IButton[] buttons = new IButton[9];
        buttons[0] = new MoveButton(movement_range);
        buttons[1] = new DemotorizeButton();
        return buttons;
    }

    @Override
    public void onDeath() {
        contained_unit.setHp(-1);
    }

    @Override
    public Properties getProperties() {
        Properties p = super.getProperties();
        IUnitName.addNameProperty(getUnitName(), p);
        return p;
    }
}
