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

package net.stalemate.server.core.units.air;

import net.stalemate.server.core.AirUnit;
import net.stalemate.server.core.animation.Animation;
import net.stalemate.server.core.animation.AnimationController;
import net.stalemate.server.core.buttons.AttackButton;
import net.stalemate.server.core.buttons.MoveButton;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.units.util.IConstructableBuilding;
import net.stalemate.server.core.units.util.IMechanized;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HeavyFighter extends AirUnit implements IMechanized{
    public HeavyFighter(int x, int y, Game game) {
        super(x, y, game, new UnitStats(13, 13, 1, 2, 4, 3, 18, 18, 0, 0, 0), new AnimationController(), "Heavy Fighter");
        Animation animation = new Animation(1);
        animation.addFrame("assets/units/placeholder_fighter.png");
        anim.addAnimation("idle",animation);
        anim.setCurrentAnimation("idle");

        Animation attack = new Animation(1);
        attack.addFrame("assets/units/placeholder_fighter.png");
        anim.addAnimation("attack", attack);
        anim.addShift("attack", "idle");

        fog_of_war_range = 3;
        move_amount = 2;
        turn_move_amount = 2;

    }

    @Override
    public IButton[] getButtons() {
        return new IButton[]{new MoveButton(movement_range, Layer.AIR), new AttackButton(1, Layer.AIR)};
    }
}
