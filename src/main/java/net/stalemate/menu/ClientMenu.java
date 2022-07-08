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
import net.stalemate.swing.HintEntry;
import net.stalemate.swing.StalemateStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class ClientMenu extends JPanel {

    private final JFrame frame;
    private final Font basis33;
    private final ReentrantLock lock = new ReentrantLock();

    private final BufferedImage background;
    private final BufferedImage title;

    private final JTextField entry;
    private final ButtonHover connect;

    private boolean alr_added_ok_button = false;

    public int status = 0;

    private final static Color StalemateGreen = new Color(35, 115, 0);

    private String error = "";
    public void setError(String error){
        this.error = error;
    }

    public String getTxt(){
        lock.lock();
        try {
            if (entry.getText().equals("Enter IP")){
                return "";
            }
            return entry.getText();
        } finally {
            lock.unlock();
        }
    }

    public ClientMenu(JFrame f){
        this.frame = f;
        frame.add(this);
        frame.pack();
        this.setLayout(null);

        basis33 = AssetLoader.getBasis33();

        entry = new HintEntry("Enter IP");
        StalemateStyle.makeJTextField(entry);
        entry.setFont(basis33.deriveFont(18f));
        entry.setBounds(new Rectangle(240, 40));
        entry.setLocation((832-14-entry.getWidth())/2, 276);
        this.add(entry);

        connect = new ButtonHover("Connect");
        StalemateStyle.makeButton(connect);
        connect.setLocation((832-14-connect.getWidth())/2, 320);
        connect.setFont(basis33.deriveFont(16f));
        connect.addActionListener(a -> status = 1);
        this.add(connect);

        background = AssetLoader.load("assets/background.png");
        title = AssetLoader.load("assets/stalemate.png");

    }

    public void update(){
        lock.lock();
        if (status != 0 && !alr_added_ok_button){
            this.remove(connect);
            this.remove(entry);
        }

        if (status == 4 && !alr_added_ok_button){
            // Repurposing pf connect button
            this.add(connect);
            connect.setSelected(false);
            connect.setText("OK");
            connect.addActionListener(a -> status = -1);
            alr_added_ok_button = true;
            // add display of ok button here {status = -1;}
        }
        this.repaint();
        lock.unlock();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        this.setBackground(StalemateGreen);
        g.drawImage(background, 0, 0, null);

        lock.lock();

        // title char size
        FontMetrics metrics = g.getFontMetrics(basis33.deriveFont((float) (25)).deriveFont(Font.BOLD));
        int width = metrics.stringWidth("A");
        int y;

        if (title!=null)
            g.drawImage(title.getScaledInstance(364, 64, Image.SCALE_FAST), 234, 230-60, null);

        if (status == 3) {
            // Render connecting]
            g.setFont(basis33.deriveFont(25f));
            g.setColor(Color.WHITE);
            int half = ((832 + 14) - width * "Connecting...".length()) / 2;
            g.drawString("Connecting...", half, 296);
        }
        if (status == 4) {
            // Render error
            g.setFont(basis33.deriveFont(25f));
            g.setColor(Color.WHITE);
            int half = ((832 + 14) - width * error.length()) / 2;
            g.drawString(error, half, 296);
        }

        this.paintComponents(g);
        lock.unlock();
    }

    public void setStatus(int status){
        lock.lock();
        this.status = status;
        lock.unlock();
    }

    public void clFrame(){
        lock.lock();
        frame.remove(this);
        lock.unlock();
    }
}
