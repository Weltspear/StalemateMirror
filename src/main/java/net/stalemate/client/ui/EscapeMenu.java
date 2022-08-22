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

import net.stalemate.client.AssetLoader;
import net.stalemate.client.ui.swing.ButtonHover;
import net.stalemate.client.ui.swing.StalemateStyle;

import javax.swing.*;
import java.awt.*;

public class EscapeMenu extends JInternalFrame {
    /***
     * 1 -> Disconnect
     * 2 -> Options
     * 3 -> Return to game
     */
    public int status = -1;

    public EscapeMenu(){
        super();

        Font monogram = AssetLoader.getMonogram().deriveFont(16f);

        this.setLayout(null);
        this.setBackground(new Color(66, 40, 14));
        this.setSize(150, 100);
        StalemateStyle.simplifyJInternalFrame(this);

        ButtonHover returnToGame = new ButtonHover("Return to game");
        returnToGame.setLocation(new Point(0,0));
        returnToGame.setSize(150, 25);
        returnToGame.setFont(monogram);
        returnToGame.addActionListener(e -> status = 3);
        StalemateStyle.makeButton(returnToGame);

        // currently disabled
        /*ButtonHover options = new ButtonHover("Options");
        options.setLocation(new Point(0,25));
        options.setSize(150, 25);
        options.setFont(monogram);
        options.addActionListener(e -> status = 2);
        StalemateStyle.makeButton(options);*/

        ButtonHover disconnect = new ButtonHover("Disconnect");
        disconnect.setLocation(new Point(0,50));
        disconnect.setSize(150, 25);
        disconnect.setFont(monogram);
        disconnect.addActionListener(e -> status = 1);
        StalemateStyle.makeButton(disconnect);

        ButtonHover exit = new ButtonHover("Exit");
        exit.setLocation(new Point(0,75));
        exit.setSize(150, 25);
        exit.setFont(monogram);
        exit.addActionListener(e -> System.exit(0));
        StalemateStyle.makeButton(exit);

        this.add(returnToGame);
        // this.add(options);
        this.add(disconnect);
        this.add(exit);
    }

    public int getStatus() {
        return status;
    }
}
