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
import net.stalemate.server.core.buttons.*;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.units.util.IUnitName;
import net.stalemate.server.core.util.IGameController;

public class Infantry extends Unit implements IUnitName{
    public Infantry(int x, int y, IGameController game) {
        super(x, y, game, new UnitStats(10, 10, 1, 1, 2, 1, 20, 20, 0, 1, 3), new AnimationController(), "Infantry");
        Animation idle = new Animation(20);
        idle.addFrame("assets/units/rifleman_idle_1.png");
        idle.addFrame("assets/units/rifleman_idle_2.png");

        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");

        Animation attack = new Animation(1);
        attack.addFrame("assets/units/rifleman_fire_1.png");
        attack.addFrame("assets/units/rifleman_fire_2.png");
        attack.addFrame("assets/units/rifleman_fire_3.png");
        attack.addFrame("assets/units/rifleman_fire_4.png");
        attack.addFrame("assets/units/rifleman_fire_5.png");

        anim.addAnimation("attack", attack);
        anim.addShift("attack", "idle");

        move_amount = 2;
        turn_move_amount = 2;
    }

    @Override
    public IButton[] getButtons() {
        IButton[] buttons = new IButton[9];
        buttons[0] = new AttackButton(attack_range);
        buttons[1] = new MoveButton(movement_range);
        if (supply > 5) {
            buttons[2] = new MotorizeButton();
        }
        else {
            buttons[8] = new HPSacrificeSU();
        }

        if (hp < max_hp && supply > 3){
            buttons[7] = new RecoverButton();
        }


        buttons[3] = new ProtectUnitButton(Layer.GROUND);

        return buttons;
    }

    @Override
    public Properties getProperties() {
        Properties p = super.getProperties();

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
