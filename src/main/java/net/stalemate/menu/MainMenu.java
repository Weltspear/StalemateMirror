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

package net.stalemate.menu;

import net.stalemate.networking.client.AssetLoader;
import net.stalemate.networking.client.Client;
import net.stalemate.networking.server.Server;
import net.stalemate.swing.ButtonHover;
import net.stalemate.swing.StalemateStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class MainMenu extends JPanel {
    private final JFrame frame;
    private volatile Font basis33;
    private final ReentrantLock lock = new ReentrantLock();

    public int status = 0;

    private final static Color StalemateGreen = new Color(35, 115, 0);

    private final BufferedImage background;
    private final BufferedImage title;

    public MainMenu(){
        frame = new JFrame("Stalemate");
        frame.setMinimumSize(new Dimension(832+14,576+32+6));
        frame.setSize(new Dimension(832+32,576+32+6));
        frame.setResizable(false);
        frame.add(this);
        frame.setVisible(true);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLayout(null);

        try {
            basis33 = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getClassLoader().getResource("basis33/basis33.ttf")).openStream());

        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }

        background = AssetLoader.load("assets/background.png");
        title = AssetLoader.load("assets/stalemate.png");

        ButtonHover play = new ButtonHover("Play");
        StalemateStyle.makeButton(play);
        ButtonHover start_srv = new ButtonHover("Start a server");
        StalemateStyle.makeButton(start_srv);
        ButtonHover options = new ButtonHover("Options");
        StalemateStyle.makeButton(options);
        ButtonHover exit = new ButtonHover("Exit");
        StalemateStyle.makeButton(exit);

        play.setLocation(new Point((832+14- play.getWidth())/2, 250));
        play.setFont(basis33.deriveFont(16f));
        play.addActionListener((e) -> status = 1);
        start_srv.setLocation(new Point((832+14- start_srv.getWidth())/2, 280));
        start_srv.setFont(basis33.deriveFont(16f));
        start_srv.addActionListener((e) -> status = 2);
        options.setLocation(new Point((832+14- options.getWidth())/2, 310));
        options.setFont(basis33.deriveFont(16f));
        exit.setLocation(new Point((832+14- exit.getWidth())/2, 340));
        exit.setFont(basis33.deriveFont(16f));
        exit.addActionListener((e) -> System.exit(0));

        this.add(play);
        this.add(start_srv);
        this.add(options);
        this.add(exit);
    }

    public void update(){
        lock.lock();
        this.repaint();

        if (status == 1){
            // frame.setVisible(false);
            this.frame.remove(this);
            Client client = new Client(this.frame);
            client.start_client();
            this.frame.add(this);
            status = 0;
            frame.setVisible(true);
        }
        else if (status == 2){
            frame.setVisible(false);
            Server server = new Server();
            server.start_server();
            frame.setVisible(true);
        }
        lock.unlock();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        this.setBackground(StalemateGreen);
        g.drawImage(background, 0, 0, null);

        while (basis33 == null){
            Thread.onSpinWait();
        }
        g.setFont(basis33.deriveFont(16f));
        g.setColor(Color.WHITE);
        g.drawString("Version v0.3a-dev", 3, 570);
        g.drawString("Made by Weltspear and Dzolab", 587, 540);
        g.drawString("Licensed under terms of GNU AGPLv3", 587, 550);
        g.drawString("See NOTICE.md and LICENSE.md for m", 587, 560);
        g.drawString("ore information", 587, 570);

        // Render button text
        int y = 276;
        if (title!=null)
            g.drawImage(title.getScaledInstance(364, 64, Image.SCALE_FAST), 234, 230-60, null);
        paintComponents(g);
    }
}
