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
import net.stalemate.server.core.buttons.util.IUnitMoveAmount;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.util.IGameController;

import java.util.ArrayList;

public class MotorizedUnitOther extends Unit implements IUnitMoveAmount {
    private final Unit contained_unit;

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
        public void action(Unit unit, IGameController gameController) {
            if (!hasTurnEnded){
                gameController.rmEntity(MotorizedUnitOther.this);
                contained_unit.setHp(hp);
                contained_unit.setSupply(supply);
                contained_unit.setX(x);
                contained_unit.setY(y);
                contained_unit.endTurn();
                gameController.addEntity(contained_unit);
                MotorizedUnitOther.this.setHp(-1);
            }
        }
    }

    public MotorizedUnitOther(int x, int y, IGameController game, Unit other) {
        super(x, y, game, new UnitStats(other.getHp(), other.getMaxHp(), 0, 3,0,0,
                        other.getSupply(), other.getMaxSupply(),0, 0, 0), new AnimationController(), other.getName());

        Animation idle = new Animation(1);
        idle.addFrame("assets/units/motorized_unit_idle.png");

        contained_unit = other;

        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");

        fog_of_war_range = 3;
    }

    @Override
    public ArrayList<IButton> getButtons() {
        ArrayList<IButton> buttons = new ArrayList<>();
        buttons.add(new MoveButton(movement_range));
        buttons.add(new DemotorizeButton());
        return buttons;
    }

    @Override
    public void onDeath() {
        contained_unit.setHp(-1);
    }

    private int move_amount = 2;

    @Override
    public void setMoveAmount(int m) {
        move_amount = m;
    }

    @Override
    public int getTurnMoveAmount() {
        return 2;
    }

    @Override
    public int getMoveAmount() {
        return move_amount;
    }

    @Override
    public void turnUpdate() {
        super.turnUpdate();
        setMoveAmount(getTurnMoveAmount());
    }

    @Override
    public Properties getProperties() {
        Properties p = super.getProperties();
        IUnitMoveAmount.addMoveAmountProperty(move_amount, hasTurnEnded, p);
        return p;
    }
}
