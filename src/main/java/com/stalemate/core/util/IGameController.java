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

package com.stalemate.core.util;

import com.stalemate.core.Entity;
import com.stalemate.core.MapObject;
import com.stalemate.core.Unit;
import com.stalemate.core.controller.Game;
import com.stalemate.core.event.EventListenerRegistry;

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

}
