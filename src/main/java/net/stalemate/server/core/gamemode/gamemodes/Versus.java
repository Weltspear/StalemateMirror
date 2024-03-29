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

package net.stalemate.server.core.gamemode.gamemodes;

import net.stalemate.server.core.gamemode.IGamemode;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.controller.Game;

import java.util.ArrayList;

public class Versus implements IGamemode {
    @Override
    public void tick(Game g) {

    }

    @Override
    public boolean hasGameEnded(Game g) {
        ArrayList<Game.Team> teams_who_have_units = new ArrayList<>();

        for (Game.Team team: g.getTeams()){
            if (!team.getTeamUnits().isEmpty() && !team.getIsDisabledTurn()){
                teams_who_have_units.add(team);
            }
        }

        return teams_who_have_units.size() == 1;
    }

    @Override
    public Game.Team getVictoriousTeam(Game g) {

        ArrayList<Game.Team> teams_who_have_units = new ArrayList<>();

        for (Game.Team team : g.getTeams()) {
            if (!(team instanceof Game.NeutralTeam))
            if (!team.getTeamUnits().isEmpty() && !team.getIsDisabledTurn()) {
                teams_who_have_units.add(team);
            }
        }

        if (teams_who_have_units.size() == 1) {
            return teams_who_have_units.get(0);
        }

        return null;
    }

    @Override
    public String gmName() {
        return "Versus";
    }
}
