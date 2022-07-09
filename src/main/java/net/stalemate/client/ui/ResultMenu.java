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

package net.stalemate.client.ui;

import net.stalemate.client.ui.swing.ButtonHover;
import net.stalemate.client.ui.swing.StalemateStyle;

import javax.swing.*;
import java.awt.*;

public class ResultMenu extends JInternalFrame {
    public volatile int status = 0;

    public ResultMenu(Font basis33, String result_){
        super();
        setLayout(null);
        setSize(400, 225);
        StalemateStyle.simplifyJInternalFrame(this);

        JLabel label = new JLabel("Game results");
        label.setSize(140, 40);
        label.setLocation((400-label.getWidth())/2, 0);
        label.setVisible(true);
        label.setForeground(Color.black);
        label.setFont(basis33.deriveFont(25f));
        add(label);

        JLabel result = new JLabel(result_);
        result.setSize(400, 100);
        result.setLocation(20, 30);
        result.setVisible(true);
        result.setForeground(Color.black);
        result.setFont(basis33.deriveFont(15f));
        add(result);

        ButtonHover buttonHover = new ButtonHover("OK");
        buttonHover.addActionListener(e -> status = 1);
        buttonHover.setSize(120, 20);
        buttonHover.setLocation(125, 195);
        buttonHover.setVisible(true);
        StalemateStyle.makeButton(buttonHover);
        buttonHover.setFont(basis33.deriveFont(12f));
        add(buttonHover);


        setVisible(true);
    }

}
