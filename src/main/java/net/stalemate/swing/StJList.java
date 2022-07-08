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

package net.stalemate.swing;

import net.stalemate.networking.client.AssetLoader;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;

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
        jList.setCellRenderer(new LobbyJList.StCellRenderer(AssetLoader.getBasis33()));
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
        label.setFont(AssetLoader.getBasis33().deriveFont(15f));
        label.setVisible(true);

        add(jList);
        add(label);

        this.setSize(width, height);
        this.setBounds(new Rectangle(width, height));
    }

    public int getSelectedIndex(){
        return jList.getSelectedIndex();
    }

    public void setStrings(ArrayList<String> strings){
        DefaultListModel<String> s = new DefaultListModel<>();
        s.addAll(strings);

        if (!Objects.equals(strings, last)) {
            jList.setModel(s);
            last = strings;
        }
    }

    @Override
    public void paint(Graphics g) {
        paintComponents(g);
    }
}
