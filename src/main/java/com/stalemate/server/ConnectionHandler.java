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

package com.stalemate.server;

import com.stalemate.server.lobby_management.Lobby;
import com.stalemate.server.lobby_management.LobbyHandler;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ConnectionHandler implements Runnable{
    public boolean isHandlerTerminated = false;

    private final Socket client;

    private final LobbyHandler lobbyHandler;

    private PrintWriter output;
    private BufferedReader input;

    private Cipher cipherEncryption;
    private Cipher cipherDecryption;

    public ConnectionHandler(Socket client, LobbyHandler lobbyHandler){
        this.client = client;
        this.lobbyHandler = lobbyHandler;
    }

    @Override
    public void run() {
        // todo: make it handle unexpected disconnects while choosing lobbies
        try {
            // client.setTcpNoDelay(true);
            client.setSoTimeout(30000);
            try {
                // Initialize encryption
                Signature.getInstance("SHA256withRSA");
                KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
                keyPairGen.initialize(2048);
                KeyPair keyPair = keyPairGen.generateKeyPair();
                // System.out.println("[ConnectionHandler] KeyPair generated");

                // Init decryption
                cipherDecryption = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipherDecryption.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
                // System.out.println("[ConnectionHandler] Decryption initialized");

                // Initialize output and input
                output = new PrintWriter(client.getOutputStream(), true);
                // output = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(client.getOutputStream()), StandardCharsets.UTF_8), true);
                input = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                // System.out.println("[ConnectionHandler] Input output initialized");

                // Send public key to client
                byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
                String publicKeyString = Base64.getEncoder().encodeToString(publicKeyBytes);
                output.println(publicKeyString);
                // System.out.println("[ConnectionHandler] Public key sent!");

                // Get client's public key
                byte[] publicKeyByteClient = Base64.getDecoder().decode(input.readLine());
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey client_public_key = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyByteClient));
                // System.out.println("[ConnectionHandler] Public key received!");

                // Initialize output encryption
                // System.out.println("[ConnectionHandler] Initializing encryption");
                cipherEncryption = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipherEncryption.init(Cipher.ENCRYPT_MODE, client_public_key);

                initAES();
                // System.out.println("[ConnectionHandler] Symmetric encryption initialized!");
            } catch (Exception e){
                // System.out.println("[ConnectionHandler] Initialization failure closing connection");
                client.close();
                return;
            }

            sendEncryptedData(lobbyHandler.lobby_list_json());
            // System.out.println("[ConnectionHandler] Lobby list sent!");

            String player_nickname = readEncryptedData();

            if (player_nickname == null){
                System.out.println("Connection lost unexpectedly!");
                client.close();
                isHandlerTerminated = true;
                return;
            }

            Lobby.Player player = null;
            try {
                boolean lobby_invalid = true;
                while (lobby_invalid) {
                    String lb = readEncryptedData();
                    if (lb == null){
                        System.out.println("Connection lost unexpectedly!");
                        client.close();
                        isHandlerTerminated = true;
                        return;
                    }
                    int lobby;
                    try {
                        lobby = Integer.parseInt(lb);
                    } catch (Exception e){
                        System.out.println("Client miscommunication closing connection");
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
                        lobby_invalid = false;
                    }
                }
            } catch (NumberFormatException e){
                System.out.println("Connection closed unexpectedly!");
                return;
            }
            // System.out.println("[ConnectionHandler] Lobby selection is OK!");
            player.set_nickname(player_nickname);

            while (!player.hasGameStarted()){
                // Waits for game start
                Thread.onSpinWait();
                // System.out.println("[ConnectionHandler] Waiting for game to start");
            }

            sendEncryptedData("start");

            boolean terminated = false;

            while (!terminated){
                if (player.isConnectionTerminated()){
                    sendCompressedAndEncrypted("connection_terminated");
                    sendCompressedAndEncrypted("Cause is unknown. Probably another player had disconnected");
                    client.close();
                    isHandlerTerminated = true;
                    return;
                }
                // 50 packets per second
                long t1 = System.currentTimeMillis();
                sendCompressedAndEncrypted(player.create_json_packet());
                long t2 = System.currentTimeMillis() - t1;
                if (20 - t2 > 0){
                    Thread.sleep(20-t2);
                }
                String packet = readCompressedAndEncrypted();
                if (packet == null){
                    System.out.println("Connection lost unexpectedly!");
                    player.terminateConnection();
                    client.close();
                    isHandlerTerminated = true;
                    return;
                }
                try {
                    player.push_command(packet);
                } catch (Exception e){
                    System.out.println("Client miscommunication. Closing connection");
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

            sendEncryptedDataAES("endofgame");
            sendEncryptedData(player.getEndOfAGameMessage());
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

    private String readEncryptedData(){
        try {
            byte[] decipheredText = cipherDecryption.doFinal(Base64.getDecoder().decode(input.readLine()));
            return new String(decipheredText, StandardCharsets.UTF_8);
        } catch (Exception e){
            return null;
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

    private void sendEncryptedDataAES(String data){
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // AES/CBC/PKCS5Padding
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ignored) {

        }
        // Create random iv
        SecureRandom rnd = new SecureRandom();
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

    private String readEncryptedDataAES(){
        byte[] iv;
        try {
            iv = Base64.getDecoder().decode(readEncryptedData());
        } catch (NullPointerException e){
            return null;
        }
        IvParameterSpec iv_ = new IvParameterSpec(iv);

        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // AES/CBC/PKCS5Padding
            cipher.init(Cipher.DECRYPT_MODE, aesKey, iv_);

            byte[] received_data = cipher.doFinal(Base64.getDecoder().decode(input.readLine()));
            return new String(received_data, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException | IOException ignored) {

        }

        return null;
    }

    public void sendCompressedAndEncrypted(String s){
        Deflater d = new Deflater();
        d.setInput(s.getBytes(StandardCharsets.UTF_8));
        d.finish();
        byte[] compressed = new byte[1024*8];
        d.deflate(compressed);
        d.end();
        sendEncryptedDataAES(Base64.getEncoder().encodeToString(compressed));
    }

    public String readCompressedAndEncrypted(){
        try {
            String compressed = readEncryptedDataAES();
            byte[] compressedb = Base64.getDecoder().decode(compressed);
            Inflater i = new Inflater();
            i.setInput(compressedb);
            byte[] orig = new byte[1024 * 8];
            try {
                i.inflate(orig, 0, 1024 * 8);
            } catch (DataFormatException e) {
                e.printStackTrace();
            }
            return new String(orig, StandardCharsets.UTF_8);
        } catch (NullPointerException e){
            return null;
        }
    }
}
