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

import net.stalemate.StVersion;
import net.stalemate.server.lobby_management.Lobby;
import net.stalemate.server.lobby_management.LobbyHandler;
import net.libutils.error.ErrorResult;
import net.libutils.error.Expect;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.stalemate.log.MakeLog.makeLog;

public class ConnectionHandler implements Runnable{
    public boolean isHandlerTerminated = false;

    private final Socket client;

    private final LobbyHandler lobbyHandler;

    private PrintWriter output;
    private BufferedReader input;

    private static final Logger LOGGER = makeLog(Logger.getLogger(ConnectionHandler.class.getSimpleName()));

    public ConnectionHandler(Socket client, LobbyHandler lobbyHandler){
        this.client = client;
        this.lobbyHandler = lobbyHandler;
    }

    @Override
    public void run() {
        // todo: make it handle unexpected disconnects while choosing lobbies
        try {
            client.setSoTimeout(30000);
            client.setTcpNoDelay(true);

            // Initialize output and input
            output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8)), true);
            input = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            LOGGER.log(Level.FINE, "Input output initialized");

            Expect<String, ?> packet_version = readSafely();

            if (packet_version.isNone()){
                LOGGER.log(Level.WARNING,"Failed to get packet version");
                client.close();
                isHandlerTerminated = true;
                return;
            }

            try{
                int packet_v = Integer.parseInt(packet_version.unwrap());

                if (!(packet_v == StVersion.packet_version)){
                    writeSafely("connection_terminated");
                    writeSafely("Server requires packet version " + StVersion.packet_version
                            + " this packet version is supported in " + StVersion.version + ".");
                    LOGGER.log(Level.WARNING,"Client has wrong packet version");
                    client.close();
                    isHandlerTerminated = true;
                    return;
                }
                else
                writeSafely("ok");
            } catch (Exception e){
                LOGGER.log(Level.WARNING,"Failed to get packet version");
                client.close();
                isHandlerTerminated = true;
                return;
            }

            if (ServerDescription.getDescription() == null){
                writeSafely("");
            }
            else {
                writeSafely(ServerDescription.getDescription());
            }

            writeSafely(lobbyHandler.lobby_list_json());
            LOGGER.log(Level.FINE,"Lobby list sent!");

            // get nickname
            Expect<String, ?> nick = readSafely();
            if (nick.isNone()){
                LOGGER.log(Level.WARNING,"Failed to read nickname: " + nick.getResult().message());
                client.close();
                isHandlerTerminated = true;
                return;
            }

            if (nick.unwrap().toLowerCase(Locale.ROOT).equals("server")){
                writeSafely("connection_terminated");
                writeSafely(String.format("Nickname \"%s\" is forbidden", nick.unwrap()));
                LOGGER.log(Level.WARNING,"Failed to read nickname: " + String.format("Nickname \"%s\" is forbidden", nick.unwrap()));
                client.close();
                isHandlerTerminated = true;
                return;
            } else{
                writeSafely("ok");
            }

            Lobby.Player player = null;
            Lobby lobby_final = null;
            try {
                boolean lobby_invalid = true;
                while (lobby_invalid) {
                    Expect<String, ?> lb = readSafely();
                    if (lb.isNone()){
                        LOGGER.log(Level.WARNING,"Failed to get player's lobby: " + lb.getResult().message());
                        client.close();
                        isHandlerTerminated = true;
                        return;
                    }

                    if (Objects.equals(lb.unwrap(), "continue")){
                        writeSafely("ok");
                        continue;
                    }

                    int lobby;
                    try {
                        lobby = Integer.parseInt(lb.unwrap());
                    } catch (Exception e){
                        LOGGER.log(Level.WARNING,"Client miscommunication closing connection");
                        client.close();
                        isHandlerTerminated = true;
                        return;
                    }

                    if (lobby < 0 | lobby > lobbyHandler.getLobbyCount() | lobby == 0) {
                        writeSafely("INCORRECT_LOBBY");
                        writeSafely(lobbyHandler.lobby_list_json());
                    } else if (lobbyHandler.getLobby(lobby - 1).current_lobby_state().equals(Lobby.LobbyState.STARTED)) {
                        writeSafely("STARTED");
                        writeSafely(lobbyHandler.lobby_list_json());
                    } else if (lobbyHandler.getLobby(lobby - 1).currentPlayerCount() == lobbyHandler.getLobby(lobby - 1).getMaxPlayerCount()) {
                        writeSafely("FULL");
                        writeSafely(lobbyHandler.lobby_list_json());
                    } else {
                        writeSafely("OK");
                        player = lobbyHandler.getLobby(lobby - 1).connect_to_lobby();
                        lobby_final = lobbyHandler.getLobby(lobby-1);
                        lobby_invalid = false;
                    }
                }
            } catch (NumberFormatException e){
                LOGGER.log(Level.FINE,"Connection closed unexpectedly!");
                return;
            }
            player.set_nickname(nick.unwrap());

            // Waits for game start
            while (!player.hasGameStarted()){
                writeSafely(lobby_final.playerNicksString());
                Expect<String, ?> rd = readSafely();
                if (rd.isNone()){
                    LOGGER.log(Level.WARNING,"Connection lost!");
                    player.terminateConnection();
                    lobby_final.rmFromLobby(player);
                    client.close();
                    isHandlerTerminated = true;
                    return;
                }

            }

            writeSafely("start");

            boolean terminated = false;

            while (!terminated){
                if (player.isConnectionTerminated()){
                    writeSafely("connection_terminated");
                    if (player.getConTerminationCause() == null)
                        writeSafely("Cause is unknown. Probably another player had disconnected or another player has" +
                                " map missing");
                    else
                        writeSafely(player.getConTerminationCause());
                    client.close();
                    isHandlerTerminated = true;
                    return;
                }
                // 66.66 packets per second
                long t1 = System.currentTimeMillis();
                writeSafely(player.create_json_packet());
                long t2 = System.currentTimeMillis() - t1;
                if (15 - t2 > 0){
                    Thread.sleep(15-t2);
                }

                Expect<String, ?> packet = readSafely();

                if (packet.isNone()){
                    LOGGER.log(Level.WARNING,"Failed to read packet: " + packet.getResult().message());
                    player.terminateConnection();
                    client.close();
                    isHandlerTerminated = true;
                    return;
                }
                try{
                    Expect<Integer, ?> result = player.push_command(packet.unwrap());
                    if (result.isNone()){
                        LOGGER.log(Level.WARNING,"Client miscommunication. Closing connection");
                        player.terminateConnection();
                        client.close();
                        isHandlerTerminated = true;
                        return;
                    }
                } /*Just in case*/ catch (Exception e){
                    LOGGER.log(Level.WARNING,"Client miscommunication. Closing connection");
                    e.printStackTrace();
                    player.terminateConnection();
                    client.close();
                    isHandlerTerminated = true;
                    return;
                }

                if (player.getEndOfAGameMessage() != null){
                    terminated = true;
                }
            }

            writeSafely("endofgame");
            writeSafely(player.getEndOfAGameMessage());
            isHandlerTerminated = true;
            client.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public Expect<String, ErrorResult> readSafely(){
        try {
            String read = input.readLine();
            if (read == null){
                return new Expect<>(() -> "Connection lost!");
            }
            return new Expect<>(read);
        } catch (Exception e){
            return new Expect<>(() -> "Connection lost!");
        }
    }

    public void writeSafely(String a){
        output.println(a);
    }
}
