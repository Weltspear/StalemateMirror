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

package net.stalemate.server;

import net.stalemate.server.lobby_management.Lobby;
import net.stalemate.server.lobby_management.LobbyHandler;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.stalemate.log.MakeLog.makeLog;

public class Server {

    protected ServerSocket serverSocket;
    protected final boolean server_running = true;
    final LobbyHandler lobbyHandler = new LobbyHandler();
    private static final Logger LOGGER = makeLog(Logger.getLogger(Server.class.getSimpleName()));

    public Server(){

    }

    @SuppressWarnings("all")
    public void start_server(){
        LOGGER.log(Level.INFO, "Stalemate Lobby Server");

        if (Lobby.DEBUG){
            LOGGER.log(Level.WARNING, "Stalemate server running in debug mode");
            LOGGER.log(Level.WARNING, "Players can see each other's units despite fog of war");
            LOGGER.log(Level.WARNING, "Chat commands enabled");
        }

        boolean result = ServerDescription.loadDesc();
        if (!result)
            LOGGER.log(Level.WARNING, "Failed to load server description");

        try {
            serverSocket = new ServerSocket(59657);
            while (server_running) {
                Socket client = serverSocket.accept();
                LOGGER.log(Level.INFO,"Connection established from remote address" + client.getRemoteSocketAddress().toString());
                ConnectionHandler connection_handler = new ConnectionHandler(client, lobbyHandler);
                new Thread(connection_handler, "ConHandler+" + client.getRemoteSocketAddress().toString()).start();
            }
        }
        catch (Exception e){
            e.printStackTrace();
            System.err.println("Failed to start server!");
            System.exit(-1);
        }
    }


}
