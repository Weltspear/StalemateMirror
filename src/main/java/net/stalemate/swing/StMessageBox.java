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
import java.awt.*;
import java.beans.PropertyVetoException;

public class StMessageBox extends StalemateInternalFrame{
    public StMessageBox(String title, String content) {
        super(title);
        setSize(250, 150);
        setLayout(null);
        ButtonHover ok = new ButtonHover("OK");
        ok.setVisible(true);
        StalemateStyle.makeButton(ok);
        ok.setSize(100, 20);
        ok.setLocation((250-ok.getWidth())/2, 105);
        ok.setFont(AssetLoader.getBasis33().deriveFont(15f));
        ok.addActionListener(e -> {
            try {
                this.setClosed(true);
            } catch (PropertyVetoException ex) {
                ex.printStackTrace();
            }
        });
        add(ok);

        JTextArea label = new JTextArea(content);
        label.setEditable(false);
        label.setEnabled(false);
        StalemateStyle.makeComponent(label);
        label.setSize(200, 80);
        label.setFont(label.getFont().deriveFont(11f));
        label.setLocation(25, 20);
        this.add(label);
    }
}
