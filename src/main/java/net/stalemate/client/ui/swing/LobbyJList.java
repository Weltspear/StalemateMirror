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

package net.stalemate.client.ui.swing;

import net.libutils.error.ErrorResult;
import net.libutils.error.Expect;
import net.stalemate.client.AssetLoader;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class LobbyJList extends JPanel {
    private final JList<String> first = new JList<>();
    private final JList<String> second = new JList<>();
    private final JList<String> third = new JList<>();

    private final ReentrantLock lock = new ReentrantLock();

    public static class StCellRenderer extends JLabel implements ListCellRenderer<String> {

        public StCellRenderer(){
            Font monogram = AssetLoader.getMonogram();
            setOpaque(true);
            setFont(monogram.deriveFont(17f));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {

            if (this.getPreferredSize().width != list.getWidth()){
                setPreferredSize(new Dimension(list.getWidth(), 15));
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setText(value);
            return this;
        }
    }

    @SuppressWarnings("unchecked")
    public LobbyJList(){
        super();
        setLayout(null);
        JList<String>[] every = (JList<String>[]) (new JList[]{first, second, third});

        Font monogram = AssetLoader.getMonogram();

        int plus = 0;
        int i = 0;
        String[] col_name = new String[]{"Gamemode", "Map", "Player Count"};
        for (JList<String> jList: every){
            jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jList.setSize(i == 0 ? 150 : i == 1 ? 150 : 100, 300);
            jList.setLocation(plus, 0);
            jList.setBounds(plus, 20, i == 0 ? 150 : i == 1 ? 150 : 100, 300);
            StalemateStyle.makeComponent(jList);
            jList.setCellRenderer(new StCellRenderer());
            jList.setBorder(new BevelBorder(BevelBorder.LOWERED));
            jList.addListSelectionListener(new ListSelectionListener() {
                private static final ReentrantLock lock = new ReentrantLock();

                @Override
                public void valueChanged(ListSelectionEvent e) {
                    lock.lock();
                    try {
                        if (first.getSelectedIndex() == jList.getSelectedIndex() && second.getSelectedIndex() == jList.getSelectedIndex()
                                && third.getSelectedIndex() == jList.getSelectedIndex()){
                            return;
                        }
                        first.setSelectedIndex(jList.getSelectedIndex());
                        second.setSelectedIndex(jList.getSelectedIndex());
                        third.setSelectedIndex(jList.getSelectedIndex());
                    }
                    finally {
                        lock.unlock();
                    }
                }
            });
            DefaultListModel<String> model = new DefaultListModel<>();

            jList.setModel(model);
            JLabel label = new JLabel(col_name[i],SwingConstants.CENTER);
            StalemateStyle.makeComponent(label);

            label.setOpaque(true);
            label.setBorder(new BevelBorder(BevelBorder.LOWERED));
            label.setSize(jList.getWidth(), 20);
            label.setLocation(plus, 0);
            label.setBounds(plus, 0, jList.getWidth(), 20);
            label.setFont(monogram.deriveFont(15f));
            label.setVisible(true);
            add(label);
            plus += (i == 0 ? 150 : i == 1 ? 150 : 100);
            i++;
        }
        add(first);
        add(second);
        add(third);
        first.setVisible(true);
        second.setVisible(true);
        third.setVisible(true);
        this.setSize(400, 300);
        this.setBounds(new Rectangle(400, 300));
    }

    @Override
    public void paint(Graphics g) {
        lock.lock();
        paintComponents(g);
        lock.unlock();
    }

    public int getSelectedIndex(){
        lock.lock();
        try {
            return first.getSelectedIndex();
        } finally {
            lock.unlock();
        }
    }

    public Expect<?, ErrorResult> setLobbies(ArrayList<String> lobbies){
        lock.lock();
        try {
            DefaultListModel<String> gamemodes = new DefaultListModel<>();
            DefaultListModel<String> map_names = new DefaultListModel<>();
            DefaultListModel<String> player_counts = new DefaultListModel<>();

            for (String lobby : lobbies) {
                String[] lb = lobby.split(",");
                gamemodes.addElement(lb[0]);
                map_names.addElement(lb[1]);
                player_counts.addElement(lb[2]);
            }

            first.setModel(gamemodes);
            second.setModel(map_names);
            third.setModel(player_counts);
        } catch (Exception e){
            return new Expect<>(() -> "Failed to setup lobby list");
        }
        lock.unlock();
        return new Expect<>(new Object());
    }
}
