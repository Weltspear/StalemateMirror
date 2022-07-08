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

        StringBuilder text = new StringBuilder();
        int append = 0;
        for (int i = 0; i < content.toCharArray().length; i++){
            char c = content.toCharArray()[i];
            if (c == '\n' || !(i+1 < content.toCharArray().length)){
                if (!(i+1 < content.toCharArray().length)){
                    text.append(c);
                }
                JLabel label = new JLabel(text.toString());
                label.setSize(200, 20);
                StalemateStyle.makeComponent(label);
                label.setVisible(true);
                label.setLocation(25, 20+append);
                add(label);
                System.out.println(text);
                text = new StringBuilder();
                append += 20;
            }
            text.append(c);
        }
    }
}
