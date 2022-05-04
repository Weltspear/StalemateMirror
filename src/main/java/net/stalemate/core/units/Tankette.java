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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class Tankette extends Unit implements IMechanized {
    public Tankette(int x, int y, IGameController game) {
        super(x, y, game, new UnitStats(10, 10, 1, 2, 2, 1, 20, 20, 1), new AnimationController(), "Tankette");
        Animation idle = new Animation(20);
        idle.addFrame("assets/units/tankette_idle.png");

        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");

        Animation attack = new Animation(1);
        attack.addFrame("assets/units/tankette_attack_1.png");
        attack.addFrame("assets/units/tankette_attack_2.png");
        attack.addFrame("assets/units/tankette_attack_3.png");
        //
        attack.addFrame("assets/units/tankette_attack_1.png");
        attack.addFrame("assets/units/tankette_attack_2.png");
        attack.addFrame("assets/units/tankette_attack_3.png");
        //
        attack.addFrame("assets/units/tankette_attack_1.png");
        attack.addFrame("assets/units/tankette_attack_2.png");
        attack.addFrame("assets/units/tankette_attack_3.png");

        anim.addAnimation("attack", attack);
        anim.addShift("attack", "idle");
    }

    @Override
    public ArrayList<IButton> getButtons() {
        ArrayList<IButton> buttons = new ArrayList<>();

        buttons.add(new AttackButton(attack_range));
        buttons.add(new MoveButton(movement_range));
        return buttons;
    }
}
