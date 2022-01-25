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

package com.stalemate.core.units;

import com.stalemate.core.Unit;
import com.stalemate.core.animation.Animation;
import com.stalemate.core.animation.AnimationController;
import com.stalemate.core.buttons.AttackButton;
import com.stalemate.core.buttons.HPSacrificeSU;
import com.stalemate.core.buttons.MoveButton;
import com.stalemate.core.util.IGameController;

import java.util.ArrayList;

public class Infantry extends Unit {
    public Infantry(int x, int y, IGameController game) {
        super(x, y, game, new UnitStats(10, 10, 1, 1, 2, 1, 30, 30, 0), new AnimationController(), "Infantry");
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
    }

    @Override
    public ArrayList<IButton> getButtons() {
        ArrayList<IButton> buttons = new ArrayList<>();
        if (supply > 5) {
            buttons.add(new AttackButton(attack_range));
            buttons.add(new MoveButton(movement_range));
        }
        else {
            buttons.add(new AttackButton(attack_range));
            buttons.add(new MoveButton(movement_range));
            for (int i = 0; i < 6; i++)
                buttons.add(null);
            buttons.add(new HPSacrificeSU());
        }
        return buttons;
    }
}
