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
import net.stalemate.server.core.units.util.IMechanized;
import net.stalemate.server.core.util.IGameController;

import java.util.ArrayList;

public class LightTank extends Unit implements IMechanized {
    public LightTank(int x, int y, IGameController game){
        super(x, y, game, new UnitStats(15, 15, 1, 2, 3, 2, 20, 20, 1, 0, 0), new AnimationController(), "Light Tank");

        Animation idle = new Animation(1);
        idle.addFrame("assets/units/light_tank_idle_1.png");
        idle.addFrame("assets/units/light_tank_idle_2.png");
        idle.addFrame("assets/units/light_tank_idle_3.png");
        idle.addFrame("assets/units/light_tank_idle_4.png");
        idle.addFrame("assets/units/light_tank_idle_5.png");

        Animation attack = new Animation(1);
        attack.addFrame("assets/units/light_tank_attack_1.png");
        attack.addFrame("assets/units/light_tank_attack_2.png");
        attack.addFrame("assets/units/light_tank_attack_3.png");

        anim.addAnimation("idle", idle);
        anim.addAnimation("attack", attack);

        anim.addShift("attack", "idle");

        anim.setCurrentAnimation("idle");
    }

    @Override
    public ArrayList<IButton> getButtons() {
        ArrayList<IButton> buttons = new ArrayList<>();

        buttons.add(new AttackButton(attack_range));
        buttons.add(new MoveButton(movement_range));
        return buttons;
    }
}
