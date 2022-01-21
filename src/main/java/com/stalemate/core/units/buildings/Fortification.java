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

package com.stalemate.core.units.buildings;

import com.stalemate.core.Unit;
import com.stalemate.core.animation.Animation;
import com.stalemate.core.animation.AnimationController;
import com.stalemate.core.buttons.AttackButton;
import com.stalemate.core.buttons.util.Unflippable;
import com.stalemate.core.units.util.IBuilding;
import com.stalemate.core.util.IGameController;

import java.util.ArrayList;

public class Fortification extends Unit implements IBuilding, Unflippable {
    public Fortification(int x, int y, IGameController game) {
        super(x, y, game, new UnitStats(20, 20, 2, 0, 2, 1, 15, 15, 2), new AnimationController(), "Fortification");

        Animation idle = new Animation(1);
        idle.addFrame("assets/units/fortification.png");

        Animation attack = new Animation(1);
        attack.addFrame("assets/units/fortification_attack_1.png");
        attack.addFrame("assets/units/fortification_attack_2.png");
        attack.addFrame("assets/units/fortification_attack_3.png");
        attack.addFrame("assets/units/fortification_attack_4.png");
        attack.addFrame("assets/units/fortification_attack_5.png");
        attack.addFrame("assets/units/fortification_attack_2.png");
        attack.addFrame("assets/units/fortification_attack_3.png");
        attack.addFrame("assets/units/fortification_attack_4.png");
        attack.addFrame("assets/units/fortification_attack_5.png");
        attack.addFrame("assets/units/fortification_attack_6.png");
        attack.addFrame("assets/units/fortification_attack_7.png");

        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");
        anim.addAnimation("attack", attack);
        anim.addShift("attack", "idle");
    }

    @Override
    public ArrayList<IButton> getButtons() {
        ArrayList<IButton> buttons = new ArrayList<>();
        buttons.add(new AttackButton(attack_range));

        return buttons;
    }
}
