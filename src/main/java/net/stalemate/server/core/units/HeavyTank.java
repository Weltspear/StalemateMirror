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
import net.stalemate.server.core.buttons.AttackButton;
import net.stalemate.server.core.buttons.MoveButton;
import net.stalemate.server.core.buttons.util.IUnitMoveAmount;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.units.util.IMechanized;
import net.stalemate.server.core.units.util.IUnitName;
import net.stalemate.server.core.util.IGameController;

import java.util.ArrayList;

public class HeavyTank extends Unit implements IMechanized, IUnitMoveAmount, IUnitName {
    public HeavyTank(int x, int y, IGameController game){
        super(x, y, game, new Unit.UnitStats(20, 20, 1, 1, 3, 3, 21, 21, 1, 0, 0), new AnimationController(), "Heavy Tank");

        Animation idle = new Animation(2);
        idle.addFrame("assets/units/heavy_tank_idle.png");

        Animation attack = new Animation(1);
        attack.addFrame("assets/units/heavy_tank_attack_0.png");
        attack.addFrame("assets/units/heavy_tank_attack_1.png");
        attack.addFrame("assets/units/heavy_tank_attack_2.png");
        attack.addFrame("assets/units/heavy_tank_attack_3.png");

        anim.addAnimation("idle", idle);
        anim.addAnimation("attack", attack);

        anim.addShift("attack", "idle");

        anim.setCurrentAnimation("idle");
    }

    @Override
    public IButton[] getButtons() {
        IButton[] buttons = new IButton[9];

        buttons[0] = new AttackButton(attack_range);
        buttons[1] = new MoveButton(movement_range);
        return buttons;
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

        if (uname.isEmpty()){
            uname = game.getUnitNameGen().genName(name);
        }

        IUnitName.addNameProperty(uname, p);
        return p;
    }

    private String uname = "";

    @Override
    public String getUnitName() {
        return uname;
    }

    @Override
    public void setUnitName(String n) {
        uname = n;
    }
}
