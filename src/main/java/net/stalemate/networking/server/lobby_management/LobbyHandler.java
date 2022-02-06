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

package net.stalemate.networking.server.lobby_management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.stalemate.core.config.LobbyConfigLoader;

import java.util.ArrayList;

public class LobbyHandler {
    protected final ArrayList<Lobby> lobbies = new ArrayList<>();

    /***
     * This class handles lobbies basically
     */
    public LobbyHandler(){
        lobbies.addAll(LobbyConfigLoader.loadLobbiesFromConfig());

        for (Lobby lobby : lobbies){
            (new Thread(lobby)).start();
        }
    }

    /***
     * @return serialized list of lobbies
     */
    public synchronized String lobby_list_json(){
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        rootNode.put("type", "LobbyList");

        ArrayNode node = rootNode.putArray("lobbies");
        for (Lobby lobby: lobbies){
            node.add(lobby.asString());
        }

        try {
            return mapper.writer().writeValueAsString(rootNode);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public Lobby getLobby(int index){
        return lobbies.get(index);
    }

    public int getLobbyCount(){
        return lobbies.size();
    }

}
