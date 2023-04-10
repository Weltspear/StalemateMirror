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
import net.stalemate.server.core.Unit;
import net.stalemate.server.core.animation.Animation;
import net.stalemate.server.core.animation.AnimationController;
import net.stalemate.server.core.buttons.BombButton;
import net.stalemate.server.core.buttons.MoveButton;
import net.stalemate.server.core.buttons.ResupplyButton;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.units.util.IMechanized;

public class TransportPlane extends AirUnit implements IMechanized {
    public TransportPlane(int x, int y, Game game) {
        super(x, y, game, new Unit.UnitStats(8, 8, 0, 2, 0, 0, 25, 30, 0, 0, 0), new AnimationController(), "Transport Plane");
        Animation animation = new Animation(1);
        animation.addFrame("assets/units/placeholder_fighter.png");
        anim.addAnimation("idle",animation);
        anim.setCurrentAnimation("idle");

        fog_of_war_range = 2;
        move_amount = 2;
        turn_move_amount = 2;

    }

    @Override
    public Unit.IButton[] getButtons() {
        IButton[] buttons = new IButton[9];
        buttons[0] = new MoveButton(movement_range, Layer.AIR);
        buttons[8] = new ResupplyButton();
        buttons[7] = new ResupplyButton(Layer.AIR);
        return buttons;
    }
}
