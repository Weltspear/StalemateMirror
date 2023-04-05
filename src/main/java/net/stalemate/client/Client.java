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

package net.stalemate.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import net.libutils.error.ErrorResult;
import net.libutils.error.Expect;
import net.libutils.etable.EntryTable;
import net.stalemate.StVersion;
import net.stalemate.client.config.Grass32ConfigClient;
import net.stalemate.client.ui.ClientMenu;
import net.stalemate.client.ui.InGameUI;
import net.stalemate.client.ui.LobbyMenu;
import net.stalemate.client.ui.LobbySelectMenu;

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
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.stalemate.log.MakeLog.makeLog;

@SuppressWarnings("unchecked")
public class Client {

    private final String ip;
    private final ClientMenu clientMenu;
    private Socket client;

    private PrintWriter output;
    private BufferedReader input;

    private final JFrame frame;

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

        int client_sel_x;
        int client_sel_y;

        int sel_x;
        int sel_y;

        int cbas_x;
        int cbas_y;

        int sbas_x;
        int sbas_y;

        public ClientGame getClientGame() {
            return clientGame;
        }

        private final ClientGame clientGame;
        private final InGameUI inGameUI;

        HashMap<String, Object> selected_unit = null;

        final InGameUI.KeyboardInput in;

        private boolean isselectorbutton_press = false;

        private boolean first_packet = true;

        public GameControllerClient(InGameUI.KeyboardInput input, InGameUI inGameUI, ClientGame clientGame){
            in = input;

            this.clientGame = clientGame;
            this.inGameUI = inGameUI;
        }

        @SuppressWarnings("unchecked")
        public Expect<String, ?> receive_packet(String json){
            try {
                Expect<String, ?> e = this.clientGame.load(json);

                PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                        .build();
                ObjectMapper objectMapper = JsonMapper.builder().polymorphicTypeValidator(ptv).build();
                Map<String, Object> data_map = (objectMapper).readValue(json, Map.class);

                sel_x = (int) data_map.get("sel_x");
                sel_y = (int) data_map.get("sel_y");

                client_sel_x = (int) data_map.get("sel_x");
                client_sel_y = (int) data_map.get("sel_y");

                cbas_x = (int) data_map.get("cbas_x");
                cbas_y = (int) data_map.get("cbas_y");

                sbas_x = (int) data_map.get("sbas_x");
                sbas_y = (int) data_map.get("sbas_y");

                if (first_packet){
                    client_sel_x = sbas_x;
                    client_sel_y = sbas_y;

                    inGameUI.unsafeLock.lock();
                    inGameUI.cam_x = cbas_x;
                    inGameUI.cam_y = cbas_y;
                    inGameUI.unsafeLock.unlock();

                    first_packet = false;
                }

                if (!(data_map.get("selected_unit_data") instanceof Integer) && data_map.get("selected_unit_data") != null){
                    selected_unit = (HashMap<String, Object>) data_map.get("selected_unit_data");

                    isselectorbutton_press = (boolean) selected_unit.get("iselectorbutton_press");
                } else{
                    selected_unit = null;
                    isselectorbutton_press = false;
                }

                return e;
            } catch (JsonProcessingException e) {
                return new Expect<>(() -> "Failed to parse JSON");
            } catch (ClassCastException | NullPointerException e){
                return new Expect<>(() -> "Incorrect packet format");
            }
        }

        private boolean reset_x_offset = false;
        private boolean reset_y_offset = false;

        public boolean[] resetOffsetArn(){
            try {
                return new boolean[]{reset_x_offset, reset_y_offset};
            } finally {
                reset_y_offset = false;
                reset_x_offset = false;
            }
        }

        @SuppressWarnings("unchecked")
        public String create_json_packet(){
            HashMap<String, Object> packet = new HashMap<>();
            ArrayList<Object> actions = new ArrayList<>();

            while(!in.getQueue().isEmpty()) {
                String input = in.getQueue().poll();

                if (Objects.equals(input, "SHIFT")){
                    inGameUI.unsafeLock.lock();
                    inGameUI.cam_x = cbas_x;
                    inGameUI.cam_y = cbas_y;
                    inGameUI.unsafeLock.unlock();

                    client_sel_x = sbas_x;
                    client_sel_y = sbas_y;
                }
                else if (Objects.equals(input, "UP")){
                    client_sel_y--;
                }
                else if (Objects.equals(input, "DOWN")){
                    client_sel_y++;
                }
                else if (Objects.equals(input, "LEFT")){
                    client_sel_x--;
                }
                else if (Objects.equals(input, "RIGHT")){
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
                else if (Objects.equals(input, "TAB")){
                    HashMap<String, Object> action = new HashMap<>();
                    action.put("action", "ChangeViewMode");
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


            inGameUI.unsafeLock.lock();

            packet.put("cam_x", inGameUI.cam_x);
            packet.put("cam_y", inGameUI.cam_y);

            inGameUI.unsafeLock.unlock();

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

        public int getSelX() {
            return sel_x;
        }

        public int getSelY() {
            return sel_y;
        }

    }

    public Expect<Integer, ?> start_client(){
        try {
            clientMenu.setStatus(3);
            clientMenu.update();

            client = new Socket(InetAddress.getByName(ip).getHostAddress(), 59657);

            clientMenu.clFrame();

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

            client.setSoTimeout(Grass32ConfigClient.getLobbyTimeout() * 1000);

            // Initialize output and input
            output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8)), true);
            input = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            LOGGER.log(Level.FINE, "Input output initialized");

            writeSafely(String.valueOf(StVersion.packet_version));
            Expect<String, ?> response_p = readSafely();

            if (response_p.isNone()){
                client.close();
                LOGGER.log(Level.WARNING, "Failed to get server response");
                return new Expect<>(() -> "Failed to get server response");
            }

            if (!(response_p.unwrap().equals("ok"))){
                Expect<String, ?> response_p2 = readSafely();

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
            Expect<String, ?> lobby_list = readSafely();
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

            writeSafely(Grass32ConfigClient.getNickname());

            Expect<String, ?> response = readSafely();

            if (response.isNone()){
                client.close();
                LOGGER.log(Level.WARNING, "Failed to get server response");
                return new Expect<>(() -> "Failed to get server response");
            }

            if (!response.unwrap().equals("ok")){
                Expect<String, ?> resp2 = readSafely();

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

            SwingUtilities.invokeAndWait(() ->{
                frame.validate();
                frame.repaint();
            });
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
                    writeSafely("continue");
                    readSafely();
                }

                if (lobbySelectMenu.getStatus() == 2) {
                    writeSafely("-1");
                    readSafely();

                    // get lobby list
                    lobby_list = readSafely();
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
                    writeSafely("" + (lobbySelectMenu.getIndex() + 1));
                    Expect<String, ?> status = readSafely();
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
                        lobby_list = readSafely();
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
            lobbySelectMenu.clFrame();

            LOGGER.log(Level.INFO, "Connected to lobby!");

            SwingUtilities.invokeAndWait(() ->{
                frame.validate();
                frame.repaint();
            });
            LobbyMenu lobbyMenu = new LobbyMenu(frame);

            // waiting in lobby
            while (true) {
                Expect<String, ?> msg = readSafely();
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
                writeSafely("ok");
            }

            lobbyMenu.clFrame();
            SwingUtilities.invokeAndWait(() ->{
                frame.validate();
                frame.repaint();
            });

            client.setSoTimeout(Grass32ConfigClient.getTimeout() * 1000);


            ClientGame cgame = new ClientGame(new ClientMapLoader());
            InGameUI inGameUI = new InGameUI(frame);
            InGameUIRunnable runnable = new InGameUIRunnable(inGameUI, cgame);
            (new Thread(runnable)).start();
            GameControllerClient controller = new GameControllerClient(inGameUI.getInput(), inGameUI, cgame);

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
                    inGameUI.clFrame();
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
                    inGameUI.clFrame();
                    client.close();
                    return new Expect<>(() -> "Lobby was terminated. Additional information: " + cause);
                }
                Expect<String, ?> expect = controller.receive_packet(json.unwrap());

                inGameUI.getClDataManager().setSelectorData(controller.sel_x, controller.sel_y);

                if (expect.isNone()) {
                    LOGGER.log(Level.WARNING, "Failed to read server packet, shutting down client: " + expect.getResult().message());
                    runnable.terminate();
                    client.close();
                    inGameUI.clFrame();
                    String msg = "Failed to read server packet, shutting down client: " + expect.getResult().message();
                    return new Expect<>(() -> msg);
                }


                // Escape menu connection termination
                if (inGameUI.isTermicon()) {
                    runnable.terminate();
                    client.close();
                    inGameUI.clFrame();
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
                inGameUI.clFrame();
                return new Expect<>(() -> msg);
            }
            client.close();
            inGameUI.setResults(result.unwrap());

            while (!inGameUI.isTermicon()) {
                Thread.sleep(1);
                inGameUI.inGameUIUpdate();
            }

            runnable.terminate();
            inGameUI.clFrame();

            return new Expect<>(1);


        } catch (Exception e){
            e.printStackTrace();
        }

        return new Expect<>(1);
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
