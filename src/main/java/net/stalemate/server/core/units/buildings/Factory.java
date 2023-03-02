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
import net.stalemate.server.core.controller.Game;

import java.util.ArrayList;

public class Factory extends Unit implements IBuilding, IConstructableBuilding {
    public Factory(int x, int y, Game game) {
        super(x, y, game, new UnitStats(10, 10, 0, 0, 0,0,-1, -1,0, 0, 0), new AnimationController(), "Factory");
        Animation idle = new Animation(1);
        idle.addFrame("assets/units/factory.png");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");
    }

    @Override
    public void turnUpdate() {
        super.turnUpdate();

        game.getUnitsTeam(this).setMilitaryPoints(game.getUnitsTeam(this)
                .getMilitaryPoints() + 1);
    }

    @Override
    public IButton[] getButtons() {
        IButton[] buttons = new IButton[9];
        buttons[0] = new Scrap();
        return buttons;
    }

    @Override
    public AnimationController underConstructionAC() {
        AnimationController anim = new AnimationController();
        Animation idle = new Animation(1);
        idle.addFrame("assets/units/factory_build.png");
        anim.addAnimation("idle", idle);
        anim.setCurrentAnimation("idle");
        return anim;
    }

    @Override
    public Properties getProperties() {
        Properties p = super.getProperties();
        p.rm("ended_turn");
        return p;
    }
}
