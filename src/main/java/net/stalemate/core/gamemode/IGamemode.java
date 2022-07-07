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

package net.stalemate.core.gamemode;

import net.stalemate.core.controller.Game;
import net.stalemate.core.properties.Properties;
import net.stalemate.core.util.IGameControllerGamemode;

public interface IGamemode {
    void tick(IGameControllerGamemode g);
    boolean hasGameEnded(IGameControllerGamemode g);
    Game.Team getVictoriousTeam(IGameControllerGamemode g);
    default Properties getProperties(){
        return new Properties();
    }
    String gmName();
}
