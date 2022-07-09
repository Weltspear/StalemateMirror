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

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import java.awt.*;

public class StalemateStyle {
    public static void makeComponent(JComponent jComponent){
        jComponent.setForeground(new Color(198, 130, 77));
        jComponent.setBackground(new Color(60, 38, 22));
    }

    public static void makeButton(ButtonHover jButton){
        makeComponent(jButton);
        jButton.setHoverBackgroundColor(new Color(198, 130, 77));
        jButton.setHoverForegroundColor(new Color(60, 38, 22));
        jButton.setPressedBackgroundColor(new Color(51, 39, 31));
        jButton.setPressedForegroundColor(Color.BLACK);
        jButton.setSize(150, 25);
    }

    public static void makeJTextField(JTextField textField){
        makeComponent(textField);
        textField.setBorder(new LineBorder(Color.BLACK, 1));
    }

    public static void simplifyJInternalFrame(JInternalFrame s){
        s.setVisible(true);
        s.setMaximizable(false);
        s.setBackground(new Color(66, 40, 14));
        s.setBorder(new LineBorder(Color.BLACK, 1));
        JComponent c = ((BasicInternalFrameUI) s.getUI()).getNorthPane();
        c.setPreferredSize(new Dimension(c.getPreferredSize().width, 0));
    }
}
