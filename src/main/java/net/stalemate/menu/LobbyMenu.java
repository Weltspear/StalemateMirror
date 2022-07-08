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
import net.stalemate.swing.ButtonHover;
import net.stalemate.swing.StJList;
import net.stalemate.swing.StalemateStyle;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class LobbyMenu extends JPanel {
    private final JFrame frame;
    private final BufferedImage background_img;
    private final BufferedImage title;

    private final ReentrantLock lock = new ReentrantLock();
    private final StJList stJList;

    /***
     * 1 -> disconnect
     */
    private int status = 0;

    public LobbyMenu(JFrame frame){
        this.frame = frame;
        this.setLayout(null);
        this.setSize(frame.getWidth(), frame.getHeight());

        Font basis33 = AssetLoader.getBasis33();
        title = AssetLoader.load("assets/stalemate.png");
        background_img = AssetLoader.load("assets/background.png");

        stJList = new StJList(400, 300, "Players");
        stJList.setLocation((this.getWidth()-stJList.getWidth()-50)/2, (this.getHeight()-stJList.getHeight())/2);
        stJList.setVisible(true);
        add(stJList);

        ButtonHover disconnect = new ButtonHover("Disconnect");
        disconnect.addActionListener(e -> status = 1);
        StalemateStyle.makeButton(disconnect);
        disconnect.setLocation(new Point(stJList.getX()+375+25, stJList.getY()));
        disconnect.setVisible(true);
        disconnect.setFont(basis33.deriveFont(16f));
        disconnect.setBorder(new BevelBorder(BevelBorder.LOWERED));
        add(disconnect);

        assert title!=null;
        assert background_img!=null;

        frame.add(this);
    }

    @Override
    public void paint(Graphics g){
        lock.lock();
        g.drawImage(background_img, 0, 0, null);
        paintComponents(g);
        if (title!=null)
            g.drawImage(title.getScaledInstance(364, 64, Image.SCALE_FAST), 234, 65, null);
        lock.unlock();
    }

    public void clFrame(){
        frame.remove(this);
    }

    public void setNicks(ArrayList<String> nicks){
        lock.lock();
        stJList.setStrings(nicks);
        lock.unlock();
    }

    public int getStatus() {
        lock.lock();
        try {
            return status;
        } finally {
            lock.unlock();
        }
    }
}
