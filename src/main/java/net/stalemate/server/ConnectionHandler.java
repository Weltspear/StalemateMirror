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

    private Cipher cipherEncryption;
    private Cipher cipherDecryption;

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

            try {
                // Initialize encryption
                Signature.getInstance("SHA256withRSA");
                KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
                keyPairGen.initialize(2048);
                KeyPair keyPair = keyPairGen.generateKeyPair();
                LOGGER.log(Level.FINE, "KeyPair generated");

                // Init decryption
                cipherDecryption = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipherDecryption.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
                LOGGER.log(Level.FINE,"Decryption initialized");

                // Initialize output and input
                output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8)), true);
                // output = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(client.getOutputStream()), StandardCharsets.UTF_8), true);
                input = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                LOGGER.log(Level.FINE,"Input output initialized");

                // Send public key to client
                byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
                String publicKeyString = Base64.getEncoder().encodeToString(publicKeyBytes);
                output.println(publicKeyString);
                LOGGER.log(Level.FINE,"Public key sent!");

                // Get client's public key
                byte[] publicKeyByteClient = Base64.getDecoder().decode(input.readLine());
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey client_public_key = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyByteClient));
                LOGGER.log(Level.FINE,"Public key received!");

                // Initialize output encryption
                LOGGER.log(Level.FINE,"Initializing encryption");
                cipherEncryption = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipherEncryption.init(Cipher.ENCRYPT_MODE, client_public_key);

                initAES();
                LOGGER.log(Level.FINE,"Symmetric encryption initialized!");
            } catch (Exception e){
                LOGGER.log(Level.WARNING,"Initialization failure closing connection");
                client.close();
                isHandlerTerminated = true;
                return;
            }

            Expect<String, ?> packet_version = readEncryptedData();

            if (packet_version.isNone()){
                LOGGER.log(Level.WARNING,"Failed to get packet version");
                client.close();
                isHandlerTerminated = true;
                return;
            }

            try{
                int packet_v = Integer.parseInt(packet_version.unwrap());

                if (!(packet_v == StVersion.packet_version)){
                    sendEncryptedData("connection_terminated");
                    sendEncryptedData("Server requires packet version " + StVersion.packet_version
                            + " this packet version is supported in " + StVersion.version + ".");
                    LOGGER.log(Level.WARNING,"Client has wrong packet version");
                    client.close();
                    isHandlerTerminated = true;
                    return;
                }
                else
                sendEncryptedData("ok");
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

            sendEncryptedData(lobbyHandler.lobby_list_json());
            LOGGER.log(Level.FINE,"Lobby list sent!");

            // get nickname
            Expect<String, ?> nick = readEncryptedData();
            if (nick.isNone()){
                LOGGER.log(Level.WARNING,"Failed to read nickname: " + nick.getResult().message());
                client.close();
                isHandlerTerminated = true;
                return;
            }

            if (nick.unwrap().toLowerCase(Locale.ROOT).equals("server")){
                sendEncryptedData("connection_terminated");
                sendEncryptedData(String.format("Nickname \"%s\" is forbidden", nick.unwrap()));
                LOGGER.log(Level.WARNING,"Failed to read nickname: " + String.format("Nickname \"%s\" is forbidden", nick.unwrap()));
                client.close();
                isHandlerTerminated = true;
                return;
            } else{
                sendEncryptedData("ok");
            }

            Lobby.Player player = null;
            Lobby lobby_final = null;
            try {
                boolean lobby_invalid = true;
                while (lobby_invalid) {
                    Expect<String, ?> lb = readEncryptedData();
                    if (lb.isNone()){
                        LOGGER.log(Level.WARNING,"Failed to get player's lobby: " + lb.getResult().message());
                        client.close();
                        isHandlerTerminated = true;
                        return;
                    }

                    if (Objects.equals(lb.unwrap(), "continue")){
                        sendEncryptedData("ok");
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
                        sendEncryptedData("INCORRECT_LOBBY");
                        sendEncryptedData(lobbyHandler.lobby_list_json());
                    } else if (lobbyHandler.getLobby(lobby - 1).current_lobby_state().equals(Lobby.LobbyState.STARTED)) {
                        sendEncryptedData("STARTED");
                        sendEncryptedData(lobbyHandler.lobby_list_json());
                    } else if (lobbyHandler.getLobby(lobby - 1).currentPlayerCount() == lobbyHandler.getLobby(lobby - 1).getMaxPlayerCount()) {
                        sendEncryptedData("FULL");
                        sendEncryptedData(lobbyHandler.lobby_list_json());
                    } else {
                        sendEncryptedData("OK");
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
                sendEncryptedData(lobby_final.playerNicksString());
                Expect<String, ?> rd = readEncryptedData();
                if (rd.isNone()){
                    LOGGER.log(Level.WARNING,"Connection lost!");
                    player.terminateConnection();
                    lobby_final.rmFromLobby(player);
                    client.close();
                    isHandlerTerminated = true;
                    return;
                }

            }

            sendEncryptedData("start");

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

    private void sendEncryptedData(String data){
        try {
            byte[] input = data.getBytes();
            cipherEncryption.update(input);
            byte[] cipherText = cipherEncryption.doFinal();
            output.println(new String(Base64.getEncoder().encode(cipherText), StandardCharsets.UTF_8));//new String(cipherText, StandardCharsets.UTF_8));
        } catch (Exception ignored){

        }
    }

    private Expect<String, ?> readEncryptedData(){
        try {
            byte[] decipheredText = cipherDecryption.doFinal(Base64.getDecoder().decode(input.readLine()));
            return new Expect<>(new String(decipheredText, StandardCharsets.UTF_8));
        } catch (IOException e){
            return new Expect<>(() -> "Connection lost!");
        } catch (IllegalBlockSizeException | BadPaddingException e){
            return new Expect<>(() -> "Failed to decrypt data");
        } catch (NullPointerException e){
            return new Expect<>(() -> "Connection lost");
        }
    }

    // AES encryption
    private SecretKey aesKey;

    private void initAES(){
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            aesKey = keyGenerator.generateKey();

            byte[] key = aesKey.getEncoded();
            sendEncryptedData(new String(Base64.getEncoder().encode(key), StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ignored) {

        }
    }

    private final Random rnd = new Random(new BigInteger((new SecureRandom()).generateSeed(30)).longValue());

    @Deprecated
    private void sendEncryptedDataAES(String data) {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // AES/CBC/PKCS5Padding
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ignored) {

        }
        // Create random iv
        assert cipher != null;
        byte[] iv = new byte[cipher.getBlockSize()];
        rnd.nextBytes(iv);
        IvParameterSpec ivParams = new IvParameterSpec(iv);
        // Send random IV
        sendEncryptedData(Base64.getEncoder().encodeToString(ivParams.getIV()));
        // Encrypt data
        try {
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivParams);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            // e.printStackTrace();
        }
        try {
            byte[] encrypted_data = cipher.doFinal(data.getBytes());
            output.println(new String(Base64.getEncoder().encode(encrypted_data), StandardCharsets.UTF_8));
        } catch (IllegalBlockSizeException | BadPaddingException ignored) {

        }
    }

    @Deprecated
    private Expect<String, ?> readEncryptedDataAES(){
        byte[] iv;

        Expect<String, ?> data_read = readEncryptedData();
        if (data_read.isNone()){
            return new Expect<>(() -> "Failed to get initialization vector" + data_read.getResult().message());
        }
        iv = Base64.getDecoder().decode(data_read.unwrap());

        IvParameterSpec iv_ = new IvParameterSpec(iv);

        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // AES/CBC/PKCS5Padding
            cipher.init(Cipher.DECRYPT_MODE, aesKey, iv_);

            Expect<String, ?> data = readSafely();
            if (data.isNone()){
                return data;
            }
            byte[] received_data = cipher.doFinal(Base64.getDecoder().decode(data.unwrap()));
            return new Expect<>(new String(received_data, StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException ignored) {
            return new Expect<>(() -> "Decryption failure");
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
