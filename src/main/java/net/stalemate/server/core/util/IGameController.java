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

package net.stalemate.server.core.util;

import net.stalemate.server.core.Entity;
import net.stalemate.server.core.MapObject;
import net.stalemate.server.core.Unit;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.event.EventListenerRegistry;
import net.stalemate.server.core.name.UnitNameGen;

import java.util.ArrayList;

public interface IGameController {

    void addEntity(Entity entity);
    void rmEntity(Entity entity);

    ArrayList<Entity> getEntities(int x, int y);

    MapObject getMapObject(int x, int y);

    ArrayList<Entity> getAllEntitiesCopy();
    ArrayList<Entity> getAllEntities();

    Game.Team getUnitsTeam(Unit u);

    int getMapWidth();
    int getMapHeight();

    EventListenerRegistry getEvReg();

    Game.StandardNeutralTeam getNeutralTeam();

    UnitNameGen getUnitNameGen();
}
