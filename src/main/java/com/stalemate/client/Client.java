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

package com.stalemate.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.stalemate.client.config.Grass32ConfigClient;
import com.stalemate.util.CompressionDecompression;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

@SuppressWarnings("unchecked")
public class Client {

    private Socket client;

    private KeyPair keyPair;

    private Cipher cipherDecryption;
    private Cipher cipherEncryption;

    private PrintWriter output;
    private BufferedReader input;

    private PublicKey server_public_key;

    private GameControllerClient controller;

    public Client(){

    }

    public static class GameControllerClient{
        int client_cam_x;
        int client_cam_y;

        int client_sel_x;
        int client_sel_y;
        int camSelMode = 0;

        int cam_x;
        int cam_y;

        int sel_x;
        int sel_y;

        HashMap<String, Object> selected_unit = null;

        final InGameUI.KeyboardInput in;

        private boolean isselectorbutton_press = false;

        public GameControllerClient(InGameUI.KeyboardInput input){
            in = input;
        }

        @SuppressWarnings("unchecked")
        public void receive_packet(String json){
            try {
                PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                        .build();
                ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
                Map<String, Object> data_map = (objectMapper).readValue(json, Map.class);

                cam_x = (int) data_map.get("x");
                cam_y = (int) data_map.get("y");

                sel_x = (int) data_map.get("sel_x");
                sel_y = (int) data_map.get("sel_y");

                client_cam_x = (int) data_map.get("x");
                client_cam_y = (int) data_map.get("y");

                client_sel_x = (int) data_map.get("sel_x");
                client_sel_y = (int) data_map.get("sel_y");

                if (!(data_map.get("selected_unit_data") instanceof Integer) && data_map.get("selected_unit_data") != null){
                    selected_unit = (HashMap<String, Object>) data_map.get("selected_unit_data");

                    isselectorbutton_press = (boolean) selected_unit.get("iselectorbutton_press");
                } else{
                    selected_unit = null;
                    isselectorbutton_press = false;
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        @SuppressWarnings("unchecked")
        public String create_json_packet(){
            HashMap<String, Object> packet = new HashMap<>();
            ArrayList<Object> actions = new ArrayList<>();

            while(!in.getQueue().isEmpty()) {
                String input = in.getQueue().poll();

                if (Objects.equals(input, "CTRL")){
                    switch (camSelMode) {
                        case 0 -> camSelMode = 1;
                        case 1 -> camSelMode = 0;
                    }
                    HashMap<String, Object> action = new HashMap<>();
                    action.put("action", "ChangeCamSelMode");
                    actions.add(action);
                }
                else if (Objects.equals(input, "SHIFT")){
                    camSelMode = 0;
                    HashMap<String, Object> action = new HashMap<>();
                    action.put("action", "TeleportCamToBase1");
                    actions.add(action);
                }
                else if (Objects.equals(input, "UP")){
                    if (camSelMode == 0)
                        client_cam_y--;
                    else
                        client_sel_y--;
                }
                else if (Objects.equals(input, "DOWN")){
                    if (camSelMode == 0)
                        client_cam_y++;
                    else
                        client_sel_y++;
                }
                else if (Objects.equals(input, "LEFT")){
                    if (camSelMode == 0)
                        client_cam_x--;
                    else
                        client_sel_x--;
                }
                else if (Objects.equals(input, "RIGHT")){
                    if (camSelMode == 0)
                        client_cam_x++;
                    else
                        client_sel_x++;
                }
                else if (Objects.equals(input, "SPACE")){
                    HashMap<String, Object> action = new HashMap<>();
                    action.put("action", "EndTurn");
                    actions.add(action);
                }
                else if (Objects.equals(input, "ENTER")) {
                    HashMap<String, Object> action = new HashMap<>();
                    if (isselectorbutton_press){
                        action.put("action", "ISBSelect");
                    }
                    else {
                        action.put("action", "SelectUnit");
                    }
                    actions.add(action);
                }
                else if (Objects.equals(input, "ESCAPE")){
                    HashMap<String, Object> action = new HashMap<>();
                    if (isselectorbutton_press)
                        action.put("action", "ISBCancel");
                    else
                        action.put("action", "DeselectUnit");
                    actions.add(action);
                }
                else if ("qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM".contains(String.valueOf(input))){
                    if (selected_unit != null && input != null){
                        HashMap<String, Object> action = new HashMap<>();

                        for (Object button: (ArrayList<Object>)(selected_unit.get("buttons"))){
                            if (!(button instanceof Integer)){
                                HashMap<String, Object> b = (HashMap<String, Object>) button;

                                // Standard button press handling
                                if (((int)b.get("mode")) == 1){
                                    if (((String)b.get("bind")).equalsIgnoreCase(input)){
                                        action.put("action", "IStandardButtonPress");

                                        HashMap<String, Object> params = new HashMap<>();
                                        params.put("id", b.get("id"));
                                        action.put("params", params);

                                        actions.add(action);
                                    }
                                }
                                if (((int)b.get("mode")) == 2){
                                    if (((String)b.get("bind")).equalsIgnoreCase(input)){
                                        action.put("action", "ISelectorButtonPress");

                                        HashMap<String, Object> params = new HashMap<>();
                                        params.put("id", b.get("id"));
                                        action.put("params", params);

                                        actions.add(action);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            packet.put("cam_x", client_cam_x);
            packet.put("cam_y", client_cam_y);

            packet.put("sel_x", client_sel_x);
            packet.put("sel_y", client_sel_y);

            packet.put("actions", actions);

            try {
                PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                        .build();
                ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
                return (objectMapper).writeValueAsString(packet);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    public void start_client(){
        try {
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter IP: ");

            // InetAddress.getByName(sc.next())
            String ip = sc.next();
            
            client = new Socket(InetAddress.getByName(ip).getHostAddress(), 59657);

            handle_connection();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void handle_connection(){ // TODO: make it handle socket exceptions etc
        try {
            // load grass32
            Grass32ConfigClient.loadGrass32();

            client.setTcpNoDelay(true);
            client.setSoTimeout(Grass32ConfigClient.getTimeout()*1000);
            try {
                // Initialize encryption
                KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
                keyPairGen.initialize(2048);
                keyPair = keyPairGen.generateKeyPair();
                // System.out.println("KeyPair generated");

                // Init decryption
                cipherDecryption = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipherDecryption.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
                // System.out.println("Decryption initialized");

                // Initialize output and input
                output = new PrintWriter(client.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                // System.out.println("Input output initialized");

                // Get server's public key
                byte[] publicKeyByteServer = Base64.getDecoder().decode(input.readLine());
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                server_public_key = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyByteServer));
                // System.out.println("Public key received!");

                // Send public key to client
                byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
                String publicKeyString = Base64.getEncoder().encodeToString(publicKeyBytes);
                output.println(publicKeyString);
                // System.out.println("Public key sent!");

                // Initialize output encryption
                // System.out.println("Initializing encryption");
                cipherEncryption = Cipher.getInstance("RSA/ECB/PKCS1Padding"); // "RSA/ECB/PKCS1Padding"
                cipherEncryption.init(Cipher.ENCRYPT_MODE, server_public_key);

                initAES();
                // System.out.println("Symmetric encryption initialized!");
            } catch (Exception e){
                // System.out.println("Initialization failure closing connection");
                client.close();
                return;
            }

            client.setSoTimeout(Grass32ConfigClient.getLobbyTimeout()*1000);

            // Make player choose the lobby
            String lobby_list = readEncryptedData();
            if (lobby_list == null){
                client.close();
                System.out.println("Server closed connection unexpectedly");
                return;
            }
            PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                    .build();
            ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
            Map<String, Object> lobby_map = (objectMapper).readValue(lobby_list, Map.class);

            sendEncryptedData(Grass32ConfigClient.getNickname());

            boolean lobby_invalid = true;
            while (lobby_invalid) {
                Scanner lobby = new Scanner(System.in);
                StringBuilder lobbies_available = new StringBuilder("Lobbies: \n");
                int i = 1;
                for (String lobby_data : (ArrayList<String>)(lobby_map.get("lobbies"))){
                    lobbies_available.append(i).append(". ").append(lobby_data).append("\n");
                    i++;
                }
                System.out.println(lobbies_available);
                System.out.print(">");
                String lobby_selected = lobby.next();
                if (Integer.parseInt(lobby_selected) > ((ArrayList<String>)(lobby_map.get("lobbies"))).size() | Integer.parseInt(lobby_selected) < 0 | Integer.parseInt(lobby_selected) == 0){
                    System.out.print("Incorrect lobby number\n");
                    continue;
                }
                sendEncryptedData(lobby_selected);

                String status = readEncryptedData();
                if (status.equals("OK")){
                    lobby_invalid = false;
                }
                else{
                    System.out.println("Can't connect to lobby because " + (status.equals("INCORRECT_LOBBY") ? "incorrect lobby number was provided" :
                            status.equals("STARTED") ? "game has already started" : status.equals("FULL") ? "lobby is full" : "UNKNOWN") );
                    lobby_list = readEncryptedData();
                    if (lobby_list == null){
                        client.close();
                        System.out.println("Server closed connection unexpectedly");
                        return;
                    }
                    ptv = BasicPolymorphicTypeValidator.builder()
                            .build();
                    objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
                    lobby_map = (objectMapper).readValue(lobby_list, Map.class);
                }
            }
            System.out.println("Connected to lobby!");

            String data = readEncryptedData();
            if (data == null){
                client.close();
                System.out.println("Server closed connection unexpectedly");
                return;
            }

            client.setSoTimeout(Grass32ConfigClient.getTimeout()*1000);

            if (data.equals("start")) {
                InGameUI inGameUI = new InGameUI();
                controller = new GameControllerClient(inGameUI.getInput());

                int tick = 0;

                while (true) {
                    long t1 = System.currentTimeMillis();
                    String json = readCompressedAndEncrypted();
                    long t2 = System.currentTimeMillis();
                    tick++;
                    if (tick == 1000) {
                        System.out.println("ping:" + (t2 - t1));
                        tick = 0;
                    }
                    if (20-(t2-t1) > 0){
                        Thread.sleep(20-(t2-t1));
                    }

                    if (json == null){
                        client.close();
                        System.out.println("Server closed connection unexpectedly");
                        inGameUI.getFrame().dispose();
                        return;
                    }
                    if (json.startsWith("endofgame")){
                        break;
                    }

                    if (json.startsWith("connection_terminated")){
                        String cause = readCompressedAndEncrypted();
                        System.out.println("Lobby was terminated. Additional information: " + cause);
                        inGameUI.getFrame().dispose();
                        client.close();
                        return;
                    }

                    inGameUI.getRenderer().change_render_data(json, controller.camSelMode);
                    controller.receive_packet(json);

                    String packet = controller.create_json_packet();
                    // System.out.println(packet);
                    sendCompressedAndEncryptedAES(packet);
                    inGameUI.repaint();
                    // System.out.println(t2-t1);
                }

                System.out.println(readCompressedAndEncrypted());
                inGameUI.getFrame().dispose();

            } else {
                System.out.println("Server miscommunication closing connection");
            }
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
            output.println(new String(Base64.getEncoder().encode(cipherText), StandardCharsets.UTF_8));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private String readEncryptedData(){
        try {
            byte[] decipheredText = cipherDecryption.doFinal(Base64.getDecoder().decode(input.readLine()));
            return new String(decipheredText, StandardCharsets.UTF_8);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    // AES encryption
    private SecretKey aesKey;

    private void initAES(){
        String key_str = readEncryptedData();
        byte[] key_bytes = Base64.getDecoder().decode(key_str);
        aesKey = new SecretKeySpec(key_bytes, "AES");
    }

    private void sendEncryptedDataAES(String data){
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // AES/CBC/PKCS5Padding
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
        try {
            byte[] encrypted_data = cipher.doFinal(data.getBytes());
            output.println(new String(Base64.getEncoder().encode(encrypted_data), StandardCharsets.UTF_8));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }


    }

    private String readEncryptedDataAES(){
        byte[] iv = Base64.getDecoder().decode(readEncryptedData());
        IvParameterSpec iv_ = new IvParameterSpec(iv);

        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // AES/CBC/PKCS5Padding
            cipher.init(Cipher.DECRYPT_MODE, aesKey, iv_);

            byte[] received_data = cipher.doFinal(Base64.getDecoder().decode(input.readLine()));
            return new String(received_data, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void sendCompressedAndEncryptedAES(String data){
        try {
            byte[] compressed = CompressionDecompression.compress(data);
            sendEncryptedDataAES(new String(Base64.getEncoder().encode(compressed)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String readCompressedAndEncrypted(){
        try {
            String compressed = readEncryptedDataAES();
            return CompressionDecompression.decompress(Base64.getDecoder().decode(compressed));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
