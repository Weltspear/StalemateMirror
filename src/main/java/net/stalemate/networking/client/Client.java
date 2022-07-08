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

package net.stalemate.networking.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import net.libutils.etable.EntryTable;
import net.stalemate.StVersion;
import net.stalemate.menu.ClientMenu;
import net.stalemate.menu.LobbyMenu;
import net.stalemate.menu.LobbySelectMenu;
import net.stalemate.networking.client.config.Grass32ConfigClient;
import net.libutils.error.ErrorResult;
import net.libutils.error.Expect;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.io.*;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.stalemate.log.MakeLog.makeLog;

@SuppressWarnings("unchecked")
public class Client {

    private final String ip;
    private final ClientMenu clientMenu;
    private Socket client;

    private KeyPair keyPair;

    private Cipher cipherDecryption;
    private Cipher cipherEncryption;

    private PrintWriter output;
    private BufferedReader input;

    private JFrame frame;

    private static final Logger LOGGER = makeLog(Logger.getLogger(Client.class.getSimpleName()));

    public Client(JFrame frame){
        this.frame = frame;

        clientMenu = new ClientMenu(frame);
        while (clientMenu.status == 0){
            clientMenu.update();
        }

        ip = clientMenu.getTxt();
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

            while (!in.getChatMSGS().isEmpty()){
                HashMap<String, String> typechat = new HashMap<>();
                typechat.put("action", "TypeChat");
                typechat.put("msg", in.getChatMSGS().poll());
                actions.add(typechat);
            }

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

    public Expect<Integer, ?> start_client(){
        try {
            client = new Socket(InetAddress.getByName(ip).getHostAddress(), 59657);

            clientMenu.setStatus(3);
            clientMenu.update();
            clientMenu.clFrame();
            frame.setVisible(false);

            return handle_connection();
        } catch (ConnectException e){
            clientMenu.setStatus(4);
            clientMenu.setError("Connection refused");

            while (clientMenu.status != -1){
                clientMenu.update();
            }
            clientMenu.clFrame();
        } catch (IOException e){
            clientMenu.setStatus(4);
            clientMenu.setError("Unknown host");

            while (clientMenu.status != -1){
                clientMenu.update();
            }
            clientMenu.clFrame();
        }
        return new Expect<>(2);
    }

    private Expect<Integer, ?> handle_connection(){
        try {
            // load grass32
            Grass32ConfigClient.loadGrass32();

            client.setTcpNoDelay(true);
            client.setSoTimeout(Grass32ConfigClient.getTimeout() * 1000);
            try {
                // Initialize encryption
                KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
                keyPairGen.initialize(2048);
                keyPair = keyPairGen.generateKeyPair();
                LOGGER.log(Level.FINE, "KeyPair generated");

                // Init decryption
                cipherDecryption = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipherDecryption.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
                LOGGER.log(Level.FINE, "Decryption initialized");

                // Initialize output and input
                output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8)), true);
                input = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                LOGGER.log(Level.FINE, "Input output initialized");

                // Get server's public key
                byte[] publicKeyByteServer = Base64.getDecoder().decode(input.readLine());
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey server_public_key = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyByteServer));
                LOGGER.log(Level.FINE, "Public key received!");

                // Send public key to client
                byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
                String publicKeyString = Base64.getEncoder().encodeToString(publicKeyBytes);
                output.println(publicKeyString);
                LOGGER.log(Level.FINE, "Public key sent!");

                // Initialize output encryption
                LOGGER.log(Level.FINE, "Initializing encryption");
                cipherEncryption = Cipher.getInstance("RSA/ECB/PKCS1Padding"); // "RSA/ECB/PKCS1Padding"
                cipherEncryption.init(Cipher.ENCRYPT_MODE, server_public_key);

                initAES();
                LOGGER.log(Level.FINE, "Symmetric encryption initialized!");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Initialization failure closing connection");
                client.close();
                return new Expect<>(() -> "Initialization failure closing connection");
            }

            client.setSoTimeout(Grass32ConfigClient.getLobbyTimeout() * 1000);

            sendEncryptedData(String.valueOf(StVersion.packet_version));
            Expect<String, ?> response_p = readEncryptedData();

            if (response_p.isNone()){
                client.close();
                LOGGER.log(Level.WARNING, "Failed to get server response");
                return new Expect<>(() -> "Failed to get server response");
            }

            if (!(response_p.unwrap().equals("ok"))){
                Expect<String, ?> response_p2 = readEncryptedData();

                if (response_p2.isNone()){
                    client.close();
                    LOGGER.log(Level.WARNING, "Failed to get server response");
                    return new Expect<>(() -> "Failed to get server response");
                }
                return new Expect<>(response_p2::unwrap);
            }

            Expect<String, ?> srv_desc = readSafely();
            if (srv_desc.isNone()){
                client.close();
                LOGGER.log(Level.WARNING, "Failed to get server description");
                return new Expect<>(() -> "Failed to get server description");
            }

            // Make player choose the lobby
            Expect<String, ?> lobby_list = readEncryptedData();
            if (lobby_list.isNone()) {
                client.close();
                LOGGER.log(Level.WARNING, "Failed to read lobby list: " + lobby_list.getResult().message());
                String msg = "Failed to read lobby list: " + lobby_list.getResult().message();
                return new Expect<>(() -> msg);
            }
            PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                    .build();
            ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
            EntryTable lobby_etable = new EntryTable((HashMap<String, Object>) (objectMapper).readValue(lobby_list.unwrap(), HashMap.class));

            sendEncryptedData(Grass32ConfigClient.getNickname());

            Expect<String, ?> response = readEncryptedData();

            if (response.isNone()){
                client.close();
                LOGGER.log(Level.WARNING, "Failed to get server response");
                return new Expect<>(() -> "Failed to get server response");
            }

            if (!response.unwrap().equals("ok")){
                Expect<String, ?> resp2 = readEncryptedData();

                if (resp2.isNone()){
                    client.close();
                    LOGGER.log(Level.WARNING, "Failed to get server response");
                    return new Expect<>(() -> "Failed to get server response");
                }
                else {
                    client.close();
                    LOGGER.log(Level.WARNING, resp2.unwrap());
                    String msg = resp2.unwrap();
                    return new Expect<>(() -> msg);
                }
            }

            frame.setVisible(true);
            LobbySelectMenu lobbySelectMenu = new LobbySelectMenu(frame, srv_desc.unwrap().replace("<br>", "\n"));

            boolean has_connected_to_lb = false;
            while (!has_connected_to_lb) {
                Expect<ArrayList<String>, EntryTable.EntryTableGetFailure> lblist = lobby_etable.get("lobbies");
                if (lblist.isNone()){
                    client.close();
                    LOGGER.log(Level.WARNING, "Failed to get lobbies");
                    lobbySelectMenu.clFrame();
                    return new Expect<>(() -> "Failed to get lobbies");
                }
                Expect<?, ?> resultExpect = lobbySelectMenu.setLobbies(lblist.unwrap());

                if (resultExpect.isNone()) {
                    client.close();
                    LOGGER.log(Level.WARNING, resultExpect.getResult().message());
                    lobbySelectMenu.clFrame();
                    String msg = resultExpect.getResult().message();
                    return new Expect<>(() -> msg);
                }

                while (lobbySelectMenu.getStatus() == 0) {
                    sendEncryptedData("continue");
                    readEncryptedData();
                }

                if (lobbySelectMenu.getStatus() == 2) {
                    sendEncryptedData("-1");
                    readEncryptedData();

                    // get lobby list
                    lobby_list = readEncryptedData();
                    if (lobby_list.isNone()) {
                        client.close();
                        LOGGER.log(Level.WARNING, "Failed to read lobby list: " + lobby_list.getResult().message());
                        lobbySelectMenu.clFrame();
                        String msg = "Failed to read lobby list: " + lobby_list.getResult().message();
                        return new Expect<>(() -> msg);
                    }
                    ptv = BasicPolymorphicTypeValidator.builder()
                            .build();
                    objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
                    lobby_etable = new EntryTable((HashMap<String, Object>) (objectMapper).readValue(lobby_list.unwrap(), HashMap.class));
                    lobbySelectMenu.setStatus(0);
                } else if (lobbySelectMenu.getStatus() == 1) {
                    sendEncryptedData("" + (lobbySelectMenu.getIndex() + 1));
                    Expect<String, ?> status = readEncryptedData();
                    if (status.isNone()) {
                        client.close();
                        LOGGER.log(Level.WARNING, "Connection lost!");
                        lobbySelectMenu.clFrame();
                        return new Expect<>(() -> "Connection lost!");
                    } else if (status.unwrap().equals("OK")) {
                        has_connected_to_lb = true;
                    } else {
                        lobbySelectMenu.setText("Can't connect to lobby because " + (status.unwrap().equals("INCORRECT_LOBBY") ? "incorrect lobby was chosen" :
                                status.unwrap().equals("STARTED") ? "game has already started" : status.unwrap().equals("FULL") ? "lobby is full" : "UNKNOWN"));
                        lobby_list = readEncryptedData();
                        if (lobby_list.isNone()) {
                            client.close();
                            LOGGER.log(Level.WARNING, "Failed to read lobby list: " + lobby_list.getResult().message());
                            lobbySelectMenu.clFrame();
                            String msg = "Failed to read lobby list: " + lobby_list.getResult().message();
                            return new Expect<>(() -> msg);
                        }
                    }
                    lobbySelectMenu.setStatus(0);
                } else if (lobbySelectMenu.getStatus() == 3) {
                    client.close();
                    LOGGER.log(Level.INFO, "Disconnecting...");
                    lobbySelectMenu.clFrame();
                    return new Expect<>(1);
                }
            }
            frame.setVisible(false);
            lobbySelectMenu.clFrame();

            LOGGER.log(Level.INFO, "Connected to lobby!");

            LobbyMenu lobbyMenu = new LobbyMenu(frame);
            frame.setVisible(true);

            // waiting in lobby
            while (true) {
                Expect<String, ?> msg = readEncryptedData();
                if (msg.isNone()) {
                    client.close();
                    LOGGER.log(Level.WARNING, "Connection lost!");
                    lobbyMenu.clFrame();
                    return new Expect<>(() -> "Connection lost!");
                } else if (msg.unwrap().equals("start")) {
                    break;
                } else {
                    ptv = BasicPolymorphicTypeValidator.builder()
                            .build();
                    objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
                    EntryTable nick_etable = new EntryTable((objectMapper).readValue(msg.unwrap(), HashMap.class));
                    Expect<ArrayList<String>, ?> nicks = nick_etable.get("nicks");

                    if (nicks.isNone()){
                        client.close();
                        LOGGER.log(Level.WARNING, "Failed to get nick list");
                        lobbyMenu.clFrame();
                        return new Expect<>(() -> "Failed to get nick list");
                    }

                    lobbyMenu.setNicks(nicks.unwrap());
                }

                if (lobbyMenu.getStatus() == 1){
                    client.close();
                    lobbyMenu.clFrame();
                    return new Expect<>(1);
                }
                sendEncryptedData("ok");
            }

            lobbyMenu.clFrame();
            frame.setVisible(false);

            client.setSoTimeout(Grass32ConfigClient.getTimeout() * 1000);


            InGameUI inGameUI = new InGameUI();
            InGameUIRunnable runnable = new InGameUIRunnable(inGameUI);
            (new Thread(runnable)).start();
            GameControllerClient controller = new GameControllerClient(inGameUI.getInput());

            int tick = 0;

            while (true) {
                long t1 = System.currentTimeMillis();
                Expect<String, ?> json = readSafely();
                long t2 = System.currentTimeMillis();
                tick++;
                if (tick == 100) {
                    LOGGER.log(Level.INFO, "ping:" + (t2 - t1));
                    tick = 0;
                }

                if (json.isNone()) {
                    client.close();
                    LOGGER.log(Level.WARNING, "Failed to read packet: " + json.getResult().message());
                    runnable.terminate();
                    inGameUI.getFrame().dispose();
                    String msg = "Failed to read packet: " + json.getResult().message();
                    return new Expect<>(() -> msg);
                }
                if (json.unwrap().startsWith("endofgame")) {
                    break;
                }

                if (json.unwrap().startsWith("connection_terminated")) {
                    String cause = input.readLine();
                    LOGGER.log(Level.WARNING, "Lobby was terminated. Additional information: " + cause);
                    runnable.terminate();
                    inGameUI.getFrame().dispose();
                    client.close();
                    return new Expect<>(() -> "Lobby was terminated. Additional information: " + cause);
                }

                runnable.lock.lock();
                Expect<String, ?> expect = inGameUI.getRenderer().change_render_data(json.unwrap(), controller.camSelMode);
                runnable.lock.unlock();
                if (expect.isNone()) {
                    LOGGER.log(Level.WARNING, "Failed to read server packet, shutting down client: " + expect.getResult().message());
                    runnable.terminate();
                    client.close();
                    inGameUI.getFrame().dispose();
                    String msg = "Failed to read server packet, shutting down client: " + expect.getResult().message();
                    return new Expect<>(() -> msg);
                }

                controller.receive_packet(json.unwrap());
                inGameUI.inGameUIUpdate();

                // Escape menu connection termination
                if (inGameUI.isTermicon()) {
                    runnable.terminate();
                    client.close();
                    inGameUI.getFrame().dispose();
                    return new Expect<>(1);
                }

                String packet = controller.create_json_packet();
                writeSafely(packet);
            }

            Expect<String, ?> result = readSafely();
            if (result.isNone()) {
                LOGGER.log(Level.WARNING, "Failed to get result " + result.getResult().message());
                client.close();
                runnable.terminate();
                String msg = "Failed to get result " + result.getResult().message();
                inGameUI.getFrame().dispose();
                return new Expect<>(() -> msg);
            }
            client.close();
            inGameUI.setResults(result.unwrap());

            while (!inGameUI.isTermicon()) {
                Thread.sleep(1);
                inGameUI.inGameUIUpdate();
            }

            runnable.terminate();
            inGameUI.getFrame().dispose();

            return new Expect<>(1);


        } catch (Exception e){
            e.printStackTrace();
        }

        return new Expect<>(1);
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

    private Expect<String, ?> readEncryptedData(){
        try {
            byte[] decipheredText = cipherDecryption.doFinal(Base64.getDecoder().decode(input.readLine()));
            return new Expect<>(new String(decipheredText, StandardCharsets.UTF_8));
        } catch (IOException e){
            return new Expect<>(() -> "Connection lost!");
        } catch (IllegalBlockSizeException | BadPaddingException e){
            return new Expect<>(() -> "Failed to decrypt data");
        }
    }

    // AES encryption
    private SecretKey aesKey;

    private final Random rnd = new Random(new BigInteger((new SecureRandom()).generateSeed(30)).longValue());

    private void initAES(){
        String key_str = readEncryptedData().unwrap();
        byte[] key_bytes = Base64.getDecoder().decode(key_str);
        aesKey = new SecretKeySpec(key_bytes, "AES");
    }

    @Deprecated
    private void sendEncryptedDataAES(String data){
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // AES/CBC/PKCS5Padding
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
        try {
            byte[] encrypted_data = cipher.doFinal(data.getBytes());
            output.println(new String(Base64.getEncoder().encode(encrypted_data), StandardCharsets.UTF_8));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
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
            String data = input.readLine();
            if (data != null)
            return new Expect<>(data);
            else return new Expect<>(() -> "Connection lost!");
        } catch (Exception e){
            return new Expect<>(() -> "Connection lost!");
        }
    }

    public void writeSafely(String a){
        output.println(a);
    }
}
