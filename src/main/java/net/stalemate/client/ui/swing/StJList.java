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

import net.stalemate.client.AssetLoader;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class StJList extends JPanel{
    private final JList<String> jList = new JList<>();
    private ArrayList<String> last = null;

    public StJList(int width, int height, String title){
        setLayout(null);

        jList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        jList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        jList.setSize(width, height-20);
        jList.setLocation(0, 20);
        jList.setBounds(0, 20, width, height-20);
        StalemateStyle.makeComponent(jList);
        jList.setCellRenderer(new LobbyJList.StCellRenderer());
        jList.setBorder(new BevelBorder(BevelBorder.LOWERED));
        DefaultListModel<String> model = new DefaultListModel<>();
        jList.setModel(model);
        jList.setVisible(true);

        JLabel label = new JLabel(title,SwingConstants.CENTER);
        StalemateStyle.makeComponent(label);

        label.setOpaque(true);
        label.setBorder(new BevelBorder(BevelBorder.LOWERED));
        label.setSize(jList.getWidth(), 20);
        label.setLocation(0, 0);
        label.setBounds(0, 0, jList.getWidth(), 20);
        label.setFont(AssetLoader.getMonogram().deriveFont(15f));
        label.setVisible(true);

        add(jList);
        add(label);

        this.setSize(width, height);
        this.setBounds(new Rectangle(width, height));
    }

    public int getSelectedIndex(){
        AtomicInteger sel_idx = new AtomicInteger();

        try {
            SwingUtilities.invokeAndWait(() -> sel_idx.set(jList.getSelectedIndex()));
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return sel_idx.get();
    }

    public void setStrings(ArrayList<String> strings){
        DefaultListModel<String> s = new DefaultListModel<>();
        s.addAll(strings);

        if (!Objects.equals(strings, last)) {
            try {
                SwingUtilities.invokeAndWait(() -> jList.setModel(s));
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
            }
            last = strings;
        }
    }

    @Override
    public void paint(Graphics g) {
        paintComponents(g);
    }
}
