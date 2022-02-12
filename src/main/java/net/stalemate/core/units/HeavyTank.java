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

package net.stalemate.core.units;

import net.stalemate.core.Unit;
import net.stalemate.core.animation.Animation;
import net.stalemate.core.animation.AnimationController;
import net.stalemate.core.buttons.AttackButton;
import net.stalemate.core.buttons.MoveButton;
import net.stalemate.core.units.util.IMechanized;
import net.stalemate.core.util.IGameController;

import java.util.ArrayList;

public class HeavyTank extends Unit implements IMechanized {
    public HeavyTank(int x, int y, IGameController game){
        super(x, y, game, new Unit.UnitStats(20, 20, 2, 1, 3, 3, 21, 21, 1), new AnimationController(), "Heavy Tank");

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
    public ArrayList<Unit.IButton> getButtons() {
        ArrayList<Unit.IButton> buttons = new ArrayList<>();

        buttons.add(new AttackButton(attack_range));
        buttons.add(new MoveButton(movement_range));
        return buttons;
    }
}
