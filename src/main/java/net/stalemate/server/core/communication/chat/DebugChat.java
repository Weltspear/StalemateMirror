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

package net.stalemate.server.core.communication.chat;

import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.gamemode.gamemodes.Fortress;
import net.stalemate.server.lobby_management.Lobby;

import java.util.HashMap;
import java.util.Objects;

public class DebugChat extends Chat{

    private final Lobby lobby;

    private final HashMap<String, Game.Team> curTeam = new HashMap<>();

    public DebugChat(Lobby lobby){
        this.lobby = lobby;
    }

    public void consoleMsg(String out){
        super.pushMsg(new Message(null, out));
    }

    @Override
    public void pushMsg(Message msg) {
        lock.lock();

        super.pushMsg(msg);

        if (msg.getMessage().startsWith("/")) {
            String[] command = msg.getMessage().substring(1).split(" ");
            if (command.length == 1) {
                if (command[0].equals("list_teams")) {
                    Game g = lobby.getGame();
                    g.lock.lock();
                    consoleMsg("List of teams in this game:");
                    for (Game.Team t : g.getTeams()) {
                        consoleMsg(t.getTeamName());
                    }
                    g.lock.unlock();
                }
                else if (command[0].equals("me")) {
                    Game g = lobby.getGame();
                    g.lock.lock();
                    for (Game.Team t : g.getTeams()) {
                        if (Objects.equals(t.getTeamName(), msg.getAuthor())){
                            curTeam.put(msg.getAuthor(), t);
                            consoleMsg("Team set successfully");
                            break;
                        }
                    }
                    g.lock.unlock();
                }
                else if (command[0].equals("fortressinfo")) {
                    Game g = lobby.getGame();
                    g.lock.lock();
                    if (g.getMode() instanceof Fortress fortress){
                        consoleMsg(fortress.fortressInfo());
                    }
                    g.lock.unlock();
                }
                else if (command[0].equals("help")) {
                    consoleMsg("/help - shows this message");
                    consoleMsg("/set_team <x> - sets player's console current team");
                    consoleMsg("/me - sets player's console current team to his own");
                    consoleMsg("/list_teams - sets player's console current team");
                    consoleMsg("/mp <x> - sets mp of player's console current team t");
                    consoleMsg("o x");
                    consoleMsg("/fortressinfo - shows info about current fortress game");
                }
            }

            if (command.length == 2) {
                if (command[0].equals("set_team")) {
                    Game g = lobby.getGame();
                    g.lock.lock();

                    boolean found = false;

                    for (Game.Team t : g.getTeams()) {
                        if (Objects.equals(t.getTeamName(), command[1])) {
                            curTeam.put(msg.getAuthor(), t);
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        consoleMsg("Set current team to " + command[1]);
                    } else {
                        consoleMsg("Team " +command[1]+ " not found");
                    }
                    g.lock.unlock();
                }
                else if (command[0].equals("mp")) {
                    Game g = lobby.getGame();
                    g.lock.lock();

                    if (curTeam.containsKey(msg.getAuthor())){
                        Game.Team t = curTeam.get(msg.getAuthor());

                        try {
                            int mp = Integer.parseInt(command[1]);

                            t.setMilitaryPoints(mp);
                            consoleMsg("MP set successfully");
                        } catch (NumberFormatException e){
                            consoleMsg("Command argument is not a number");
                        }

                    } else{
                        consoleMsg("You haven't set your current team use /set_team to do so");
                    }

                    g.lock.unlock();
                }
            }
        }
        lock.unlock();
    }
}
