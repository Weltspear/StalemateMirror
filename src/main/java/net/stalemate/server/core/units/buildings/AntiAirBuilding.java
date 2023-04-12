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

package net.stalemate.server.core.units.buildings;

import net.stalemate.server.core.Unit;
import net.stalemate.server.core.animation.Animation;
import net.stalemate.server.core.animation.AnimationController;
import net.stalemate.server.core.buttons.AttackButton;
import net.stalemate.server.core.buttons.Scrap;
import net.stalemate.server.core.buttons.util.NoMoveAttack;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.units.util.IAntiAir;
import net.stalemate.server.core.units.util.IBuilding;
import net.stalemate.server.core.units.util.IConstructableBuilding;
import org.jetbrains.annotations.Nullable;

public class AntiAirBuilding extends Unit implements IBuilding, IConstructableBuilding, IAntiAir, NoMoveAttack {
    public AntiAirBuilding(int x, int y, Game game) {
        super(x, y, game, new UnitStats(10, 10, 3, 0, 4, 0, 15, 15, 0, 0, 0), new AnimationController(), "Anti-Air");

        Animation idle = new Animation(1);
        idle.addFrame("texture_missing");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");

        Animation attack = new Animation(1);
        attack.addFrame("texture_missing");
        anim.addAnimation("attack", attack);
        anim.addShift("attack", "idle");
        anim.setCurrentAnimation("attack");
    }

    @Override
    public @Nullable IButton[] getButtons() {
        IButton[] buttons = new IButton[9];
        buttons[0] = new AttackButton(attack_range, Layer.AIR);
        buttons[8] = new Scrap();
        return buttons;
    }

    @Override
    public AnimationController underConstructionAC() {
        AnimationController anim = new AnimationController();
        Animation idle = new Animation(1);
        idle.addFrame("texture_missing");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");
        return anim;
    }
}
