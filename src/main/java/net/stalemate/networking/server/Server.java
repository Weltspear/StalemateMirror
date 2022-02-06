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

package net.stalemate.networking.server;

import net.stalemate.networking.server.lobby_management.LobbyHandler;

import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    protected ServerSocket serverSocket;
    protected final boolean server_running = true;
    // ArrayList<ConnectionHandler> connectionHandlers = new ArrayList<>();
    final LobbyHandler lobbyHandler = new LobbyHandler();

    public Server(){

    }

    public void start_server(){
        System.out.println("Stalemate Lobby Server");
        try {
            serverSocket = new ServerSocket(59657);
            while (server_running) {
                Socket client = serverSocket.accept();
                System.out.println("Connection established from remote address" + client.getRemoteSocketAddress().toString());
                ConnectionHandler connection_handler = new ConnectionHandler(client, lobbyHandler);
                new Thread(connection_handler).start();
                // connectionHandlers.add(connection_handler);
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


}
