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

package net.stalemate.core.units.buildings;

import net.stalemate.core.Unit;
import net.stalemate.core.animation.Animation;
import net.stalemate.core.animation.AnimationController;
import net.stalemate.core.units.util.IBuilding;
import net.stalemate.core.units.util.IConstructableBuilding;
import net.stalemate.core.util.IGameController;
import net.stalemate.core.util.PriorityUpdate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class Radar extends Unit implements IConstructableBuilding, IBuilding, PriorityUpdate {
    public Radar(int x, int y, IGameController game) {
        super(x, y, game, new UnitStats(10, 10, 0, 0, 0, 0, 7, 7, 1), new AnimationController(), "Radar");

        Animation idle = new Animation(1);
        idle.addFrame("assets/units/building_radar.png");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");
        fog_of_war_range = 7;
    }

    @Override
    public @Nullable ArrayList<IButton> getButtons() {
        return null;
    }

    @Override
    public AnimationController underConstructionAC() {
        AnimationController ac = new AnimationController();
        Animation idle = new Animation(1);
        idle.addFrame("assets/units/building_radar.png");
        ac.addAnimation("idle", idle);
        ac.setCurrentAnimation("idle");
        return ac;
    }

    @Override
    public void turnUpdate() {
        super.turnUpdate();
        this.supply--;
    }
}
