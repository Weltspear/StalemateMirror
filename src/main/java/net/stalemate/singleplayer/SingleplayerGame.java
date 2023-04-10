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

import net.stalemate.client.*;
import net.stalemate.client.config.Grass32ConfigClient;
import net.stalemate.client.ui.InGameUI;
import net.stalemate.server.lobby_management.Lobby;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SingleplayerGame {

    private final SingleplayerLobby lobby;
    private final Thread lobbyThr;

    private final Lobby.Player player;

    private final InGameUI inGameUI;
    private final Client.GameControllerClient gameController;
    private final InGameUIRunnable inGameUIRunnable;

    private final ClientGame cgame;

    private final JFrame frame;

    public SingleplayerGame(String map){
        this.player = new Lobby.Player();
        this.player.setNickname("Player");

        lobby = new SingleplayerLobby(map, player);
        lobbyThr = new Thread(lobby, "LobbyThread");

        /*stolen directly from main menu*/
        // fixme: replace all of this with directly passing main menu jframe
        frame = new JFrame("Stalemate");
        frame.setMinimumSize(new Dimension(832+14,576+32+6));
        frame.setSize(new Dimension(832+32,576+32+6));
        frame.setResizable(false);
        frame.setVisible(true);
        frame.setIconImage(AssetLoader.load("assets/ui/selectors/ui_attack.png"));
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(null);

        // Disable F10 bind due to weird stuff happening when openning escape menu
        Action emptyAction = new AbstractAction(){public void actionPerformed(ActionEvent e) {}};
        KeyStroke f10 = KeyStroke.getKeyStroke("F10");
        frame.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(f10, "F10");
        frame.getRootPane().getActionMap().put("F10", emptyAction);
        //

        cgame = new ClientGame(new ClientMapLoader());
        inGameUI = new InGameUI(frame);
        inGameUIRunnable = new InGameUIRunnable(inGameUI, cgame);
        gameController = new Client.GameControllerClient(inGameUI.getInput(), inGameUI, cgame);
    }

    public void startGame(){
        Grass32ConfigClient.loadGrass32();
        lobbyThr.start();
        (new Thread(inGameUIRunnable)).start();

        while(!lobby.isReady()){
            Thread.onSpinWait();
        }

        while (true){
            // 66.66 updates per second
            long t1 = System.currentTimeMillis();
            player.pushCommand(gameController.create_json_packet());

            gameController.receive_packet(player.createJsonPacket());

            inGameUI.getClDataManager().setSelectorData(gameController.getSelX(), gameController.getSelY());

            long t2 = System.currentTimeMillis() - t1;
            if (15 - t2 > 0){
                try {
                    Thread.sleep(15-t2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (inGameUI.isTermicon()){
                lobby.terminateLobby();
                inGameUIRunnable.terminate();
                inGameUI.getFrame().dispose();
                System.exit(0);
            }

            if (player.getEndOfAGameMessage() != null){
                break;
            }
        }

        inGameUI.setResults(player.getEndOfAGameMessage());

        while (!inGameUI.isTermicon()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            inGameUI.inGameUIUpdate();
        }

        inGameUIRunnable.terminate();
        inGameUI.clFrame();

    }


}