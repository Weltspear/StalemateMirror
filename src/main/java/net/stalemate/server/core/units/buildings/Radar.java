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
import net.stalemate.server.core.buttons.Scrap;
import net.stalemate.server.core.properties.Properties;
import net.stalemate.server.core.units.util.IBuilding;
import net.stalemate.server.core.units.util.IConstructableBuilding;
import net.stalemate.server.core.util.IGameController;
import net.stalemate.server.core.util.PriorityTurnUpdate;

import java.util.ArrayList;

public class Radar extends Unit implements IConstructableBuilding, IBuilding, PriorityTurnUpdate {
    public Radar(int x, int y, IGameController game) {
        super(x, y, game, new UnitStats(10, 10, 0, 0, 0, 0, -1, -1, 1, 0, 0), new AnimationController(), "Radar");

        Animation idle = new Animation(1);
        idle.addFrame("assets/units/building_radar.png");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");
        fog_of_war_range = 7;
    }

    @Override
    public ArrayList<IButton> getButtons() {
        ArrayList<IButton> buttons = new ArrayList<>();
        buttons.add(new Scrap());
        return buttons;
    }

    @Override
    public Properties getProperties() {
        Properties p = super.getProperties();
        p.rm("ended_turn");
        return p;
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
    }
}
