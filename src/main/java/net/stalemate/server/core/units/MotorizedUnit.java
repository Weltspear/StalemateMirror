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
import net.stalemate.server.core.buttons.ResupplyButton;
import net.stalemate.server.core.units.util.IMechanized;
import net.stalemate.server.core.util.IGameController;

import java.util.ArrayList;

public class MotorizedUnit extends Unit implements IMechanized {

    public MotorizedUnit(int x, int y, IGameController game) {
        super(x, y, game, new UnitStats(5, 5, 0, 3, 0, 0, 40, 40, 0, 0, 0), new AnimationController(), "Motorized Unit");

        Animation idle = new Animation(1);
        idle.addFrame("assets/units/motorized_unit_idle.png");

        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");

        fog_of_war_range = 3;
    }

    @Override
    public ArrayList<IButton> getButtons() {
        ArrayList<IButton> buttons = new ArrayList<>();
        buttons.add(new MoveButton(movement_range));
        buttons.add(new ResupplyButton());
        return buttons;
    }
}
