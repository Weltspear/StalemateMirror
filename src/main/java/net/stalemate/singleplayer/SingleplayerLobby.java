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

package net.stalemate.singleplayer;

import net.libutils.error.Expect;
import net.stalemate.server.core.Unit;
import net.stalemate.server.core.controller.Game;
import net.stalemate.server.core.gamemode.IGamemodeAI;
import net.stalemate.server.core.gamemode.IGamemodeTextUI;
import net.stalemate.server.core.units.util.IBase;
import net.stalemate.server.lobby_management.Lobby;
import net.stalemate.singleplayer.textui.TextUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import static net.stalemate.log.MakeLog.makeLog;

public class SingleplayerLobby extends Lobby {
    private static final Logger LOGGER = makeLog(Logger.getLogger(SingleplayerLobby.class.getSimpleName()));
    private final Player player;
    private boolean terminate_lobby = false;
    public volatile boolean isReady = false;

    public boolean isReady() {
        return isReady;
    }

    private ArrayList<AIPlayer> aiPlayers = new ArrayList<>();

    public SingleplayerLobby(String map_path, Player player) {
        super(map_path, new ArrayList<>());
        this.player = player;
        this.next_maps = new ArrayList<>();
        this.next_maps.add(map_path);
    }

    public static class AIPlayer extends Player{
        private final AITurn ai;

        public AIPlayer(AITurn ai){
            super();
            this.ai = ai;
        }

        public void doTurn(){
            ai.doTurn();
        }

        @Override
        public boolean isConnectionTerminated(){
            return false;
        }

        @Override
        public void terminateConnection(){

        }

        @Override
        public void terminateConnection(String cause){

        }

        @Override
        public String getConTerminationCause(){
            return null;
        }

        @Override
        public void setCamPos(int x, int y){

        }

        @Override
        public synchronized Expect<Integer, ?> pushCommand(String json){
            return new Expect<>(() -> "AIPlayer is controlled by AI");
        }

        @Override
        public synchronized String createJsonPacket() {
            return null;
        }

        @Override
        public void setEndOfGameMessage(String endOfAGameMessage) {

        }

        @Override
        public String getEndOfAGameMessage() {
            return null;
        }
    }

    @Override
    public void lbstart() {
        lobby_lock.lock();
        game.lock.lock();

        IGamemodeAI gmAI;

        if ((game.getMode() instanceof IGamemodeAI gamemodeAI)){
            gmAI = gamemodeAI;
        }
        else{
            throw new RuntimeException("This gamemode doesn't have AI implemented");
        }

        Game.Team team = game.getUnassignedTeam();

        for (Unit u: team.getTeamUnits()){
            if (u instanceof IBase){
                player.setCamPos(u.getX() - 6, u.getY() - 2);
                break;
            }
        }

        player.setTeam(team);
        player.setGame(game);
        player.setMapPath(map_path);
        player.setChat(chat);

        players.add(player);
        team.setTeamName("Player");

        Game.Team next = game.getUnassignedTeam();
        while (next != null){
            AIPlayer ai = new AIPlayer(gmAI.getAI(game, next));
            ai.setTeam(next);
            players.add(ai);
            aiPlayers.add(ai);
            next = game.getUnassignedTeam();
        }

        HashMap<Integer, String> player_colors = new HashMap<>();

        for (Player player: players){
            player_colors.put(player.getTeam().getTeamColor().getRGB(), player.getNickname());
        }

        for (Player player: players){
            player.setPlayerColors(player_colors);
        }

        game.lock.unlock();
        lobby_lock.unlock();

        isReady = true;

        while (!game.hasGameEnded()){ // Hardcoded tick speed: 15
            long timeCurrent1 = System.currentTimeMillis();
            game.update();

            if (terminate_lobby){
                return;
            }

            lobby_lock.lock();
            game.lock.lock();

            for (AIPlayer player: aiPlayers){
                if (!player.getTeam().endedTurn()){
                    player.doTurn();

                    player.getTeam().endTurn();
                }
            }

            game.lock.unlock();
            lobby_lock.unlock();

            long timeCurrent2 = System.currentTimeMillis();
            long t = timeCurrent2 - timeCurrent1;
            long t2 = 70 - t;
            if (t2 > 0){
                try {
                    Thread.sleep(t2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        lobby_lock.lock();


        if (player.getTeam() == game.getVictoriousTeam()){
            player.setEndOfGameMessage("You won!");
        } else{
            player.setEndOfGameMessage("You lost!");
        }

        lobby_lock.unlock();
    }

    @Override
    public String asString() {
        return "SingleplayerLobby";
    }

    @Override
    public void run() {
        resetLobby();
    }

    public void terminateLobby(){
        lobby_lock.lock();
        terminate_lobby = true;
        lobby_lock.unlock();
    }

    @Override
    public LobbyState currentLobbyState() {
        return null;
    }

    @Override
    public int getMaxPlayerCount() {
        return -1;
    }

    @Override
    public int currentPlayerCount() {
        return -1;
    }

    @Override
    public void printStatus() {

    }

    @Override
    public Player connectToLobby() {
        return null;
    }

    @Override
    public void rmFromLobby(Player player) {

    }

    @Override
    public String playerNicksString() {
        return null;
    }

    public TextUI getTextUI(){
        try{
            game.lock.lock();
            if (game.getMode() instanceof IGamemodeTextUI t){
                return t.getTextUI();
            }
            else return null;
        }
        finally {
            game.lock.unlock();
        }
    }
}
